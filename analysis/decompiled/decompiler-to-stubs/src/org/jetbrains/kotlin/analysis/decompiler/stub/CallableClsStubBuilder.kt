// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.analysis.decompiler.stub

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.analysis.decompiler.stub.flags.*
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.kotlin.*
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.ProtoBuf.MemberKind
import org.jetbrains.kotlin.metadata.ProtoBuf.Modality
import org.jetbrains.kotlin.metadata.deserialization.*
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtSimpleNameStringTemplateEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.stubs.ConstantValueKind
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.psi.stubs.impl.*
import org.jetbrains.kotlin.resolve.DataClassResolver
import org.jetbrains.kotlin.resolve.constants.ClassLiteralValue
import org.jetbrains.kotlin.serialization.deserialization.AnnotatedCallableKind
import org.jetbrains.kotlin.serialization.deserialization.ProtoContainer
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol
import org.jetbrains.kotlin.serialization.deserialization.getName

fun createPackageDeclarationsStubs(
    parentStub: StubElement<out PsiElement>,
    outerContext: ClsStubBuilderContext,
    protoContainer: ProtoContainer.Package,
    packageProto: ProtoBuf.Package
) {
    createDeclarationsStubs(parentStub, outerContext, protoContainer, packageProto.functionList, packageProto.propertyList)
    createTypeAliasesStubs(parentStub, outerContext, protoContainer, packageProto.typeAliasList)
}

fun createDeclarationsStubs(
    parentStub: StubElement<out PsiElement>,
    outerContext: ClsStubBuilderContext,
    protoContainer: ProtoContainer,
    functionProtos: List<ProtoBuf.Function>,
    propertyProtos: List<ProtoBuf.Property>,
) {
    for (propertyProto in propertyProtos) {
        if (!shouldSkip(propertyProto.flags, outerContext.nameResolver.getName(propertyProto.name))) {
            PropertyClsStubBuilder(parentStub, outerContext, protoContainer, propertyProto).build()
        }
    }
    for (functionProto in functionProtos) {
        if (!shouldSkip(functionProto.flags, outerContext.nameResolver.getName(functionProto.name))) {
            FunctionClsStubBuilder(parentStub, outerContext, protoContainer, functionProto).build()
        }
    }
}

fun createTypeAliasesStubs(
    parentStub: StubElement<out PsiElement>,
    outerContext: ClsStubBuilderContext,
    protoContainer: ProtoContainer,
    typeAliasesProtos: List<ProtoBuf.TypeAlias>
) {
    for (typeAliasProto in typeAliasesProtos) {
        createTypeAliasStub(parentStub, typeAliasProto, protoContainer, outerContext)
    }
}

fun createConstructorStub(
    parentStub: StubElement<out PsiElement>,
    constructorProto: ProtoBuf.Constructor,
    outerContext: ClsStubBuilderContext,
    protoContainer: ProtoContainer
) {
    ConstructorClsStubBuilder(parentStub, outerContext, protoContainer, constructorProto).build()
}

private fun shouldSkip(flags: Int, name: Name): Boolean {
    return when (Flags.MEMBER_KIND.get(flags)) {
        MemberKind.FAKE_OVERRIDE, MemberKind.DELEGATION -> true
        //TODO: fix decompiler to use sane criteria
        MemberKind.SYNTHESIZED -> !DataClassResolver.isComponentLike(name)
        else -> false
    }
}

abstract class CallableClsStubBuilder(
    parent: StubElement<out PsiElement>,
    outerContext: ClsStubBuilderContext,
    protected val protoContainer: ProtoContainer,
    private val typeParameters: List<ProtoBuf.TypeParameter>
) {
    protected val c = outerContext.child(typeParameters)
    protected val typeStubBuilder = TypeClsStubBuilder(c)
    private val contextReceiversListStubBuilder = ContextReceiversListStubBuilder(c)
    protected val isTopLevel: Boolean get() = protoContainer is ProtoContainer.Package
    protected val callableStub: StubElement<out PsiElement> by lazy(LazyThreadSafetyMode.NONE) { doCreateCallableStub(parent) }

    fun build() {
        contextReceiversListStubBuilder.createContextReceiverStubs(callableStub, contextReceiverTypes)
        createModifierListStub()
        val typeConstraintListData = typeStubBuilder.createTypeParameterListStub(callableStub, typeParameters)
        createReceiverTypeReferenceStub()
        createValueParameterList()
        createReturnTypeStub()
        typeStubBuilder.createTypeConstraintListStub(callableStub, typeConstraintListData)
        createInitializerStub()
    }

    abstract val receiverType: ProtoBuf.Type?
    abstract val receiverAnnotations: List<ClassIdWithTarget>

    abstract val returnType: ProtoBuf.Type?
    abstract val contextReceiverTypes: List<ProtoBuf.Type>

    private fun createReceiverTypeReferenceStub() {
        receiverType?.let {
            typeStubBuilder.createTypeReferenceStub(callableStub, it, this::receiverAnnotations)
        }
    }

    private fun createReturnTypeStub() {
        returnType?.let {
            typeStubBuilder.createTypeReferenceStub(callableStub, it)
        }
    }

    abstract fun createModifierListStub()

    abstract fun createValueParameterList()

    abstract fun doCreateCallableStub(parent: StubElement<out PsiElement>): StubElement<out PsiElement>

    protected open fun createInitializerStub() {}
}

private class FunctionClsStubBuilder(
    parent: StubElement<out PsiElement>,
    outerContext: ClsStubBuilderContext,
    protoContainer: ProtoContainer,
    private val functionProto: ProtoBuf.Function
) : CallableClsStubBuilder(parent, outerContext, protoContainer, functionProto.typeParameterList) {
    override val receiverType: ProtoBuf.Type?
        get() = functionProto.receiverType(c.typeTable)

    override val receiverAnnotations: List<ClassIdWithTarget>
        get() {
            return c.components.annotationLoader
                .loadExtensionReceiverParameterAnnotations(protoContainer, functionProto, AnnotatedCallableKind.FUNCTION)
                .map { ClassIdWithTarget(it, AnnotationUseSiteTarget.RECEIVER) }
        }

    override val returnType: ProtoBuf.Type
        get() = functionProto.returnType(c.typeTable)

    override val contextReceiverTypes: List<ProtoBuf.Type>
        get() = functionProto.contextReceiverTypes(c.typeTable)

    override fun createValueParameterList() {
        typeStubBuilder.createValueParameterListStub(callableStub, functionProto, functionProto.valueParameterList, protoContainer)
    }

    override fun createModifierListStub() {
        val modalityModifier = if (isTopLevel) listOf() else listOf(MODALITY)
        val modifierListStubImpl = createModifierListStubForDeclaration(
            callableStub, functionProto.flags,
            listOf(VISIBILITY, OPERATOR, INFIX, EXTERNAL_FUN, INLINE, TAILREC, SUSPEND, EXPECT_FUNCTION) + modalityModifier
        )

        // If function is marked as having no annotations, we don't create stubs for it
        if (!Flags.HAS_ANNOTATIONS.get(functionProto.flags)) return

        val annotationIds = c.components.annotationLoader.loadCallableAnnotations(
            protoContainer, functionProto, AnnotatedCallableKind.FUNCTION
        )
        createAnnotationStubs(annotationIds, modifierListStubImpl)
    }

    override fun doCreateCallableStub(parent: StubElement<out PsiElement>): StubElement<out PsiElement> {
        val callableName = c.nameResolver.getName(functionProto.name)

        // Note that arguments passed to stubs here and elsewhere are based on what stabs would be generated based on decompiled code
        // As functions are never decompiled to fun f() = 1 form, hasBlockBody is always true
        // This info is anyway irrelevant for the purposes these stubs are used
        return KotlinFunctionStubImpl(
            parent,
            callableName.ref(),
            isTopLevel,
            c.containerFqName.child(callableName),
            isExtension = functionProto.hasReceiver(),
            hasBlockBody = true,
            hasBody = Flags.MODALITY.get(functionProto.flags) != Modality.ABSTRACT,
            hasTypeParameterListBeforeFunctionName = functionProto.typeParameterList.isNotEmpty(),
            mayHaveContract = functionProto.hasContract()
        )
    }
}

private class PropertyClsStubBuilder(
    parent: StubElement<out PsiElement>,
    outerContext: ClsStubBuilderContext,
    protoContainer: ProtoContainer,
    private val propertyProto: ProtoBuf.Property
) : CallableClsStubBuilder(parent, outerContext, protoContainer, propertyProto.typeParameterList) {
    private val isVar = Flags.IS_VAR.get(propertyProto.flags)
    private val initializer = lazy { calcInitializer() }

    override val receiverType: ProtoBuf.Type?
        get() = propertyProto.receiverType(c.typeTable)

    override val receiverAnnotations: List<ClassIdWithTarget>
        get() = c.components.annotationLoader
            .loadExtensionReceiverParameterAnnotations(protoContainer, propertyProto, AnnotatedCallableKind.PROPERTY_GETTER)
            .map { ClassIdWithTarget(it, AnnotationUseSiteTarget.RECEIVER) }

    override val returnType: ProtoBuf.Type
        get() = propertyProto.returnType(c.typeTable)

    override val contextReceiverTypes: List<ProtoBuf.Type>
        get() = propertyProto.contextReceiverTypes(c.typeTable)

    override fun createValueParameterList() {
    }

    override fun createModifierListStub() {
        val constModifier = if (isVar) listOf() else listOf(CONST)
        val modalityModifier = if (isTopLevel) listOf() else listOf(MODALITY)

        val modifierListStubImpl = createModifierListStubForDeclaration(
            callableStub, propertyProto.flags,
            listOf(VISIBILITY, LATEINIT, EXTERNAL_PROPERTY, EXPECT_PROPERTY) + constModifier + modalityModifier
        )

        // If field is marked as having no annotations, we don't create stubs for it
        if (!Flags.HAS_ANNOTATIONS.get(propertyProto.flags)) return

        val propertyAnnotations =
            c.components.annotationLoader.loadCallableAnnotations(protoContainer, propertyProto, AnnotatedCallableKind.PROPERTY)
        val backingFieldAnnotations =
            c.components.annotationLoader.loadPropertyBackingFieldAnnotations(protoContainer, propertyProto)
        val delegateFieldAnnotations =
            c.components.annotationLoader.loadPropertyDelegateFieldAnnotations(protoContainer, propertyProto)
        val allAnnotations =
            propertyAnnotations.map { ClassIdWithTarget(it, null) } +
                    backingFieldAnnotations.map { ClassIdWithTarget(it, AnnotationUseSiteTarget.FIELD) } +
                    delegateFieldAnnotations.map { ClassIdWithTarget(it, AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD) }
        createTargetedAnnotationStubs(allAnnotations, modifierListStubImpl)
    }

    override fun doCreateCallableStub(parent: StubElement<out PsiElement>): StubElement<out PsiElement> {
        val callableName = c.nameResolver.getName(propertyProto.name)
        // Note that arguments passed to stubs here and elsewhere are based on what stabs would be generated based on decompiled code
        // This info is anyway irrelevant for the purposes these stubs are used

        return KotlinPropertyStubImpl(
            parent,
            callableName.ref(),
            isVar,
            isTopLevel,
            hasDelegate = false,
            hasDelegateExpression = false,
            hasInitializer = initializer.value != null,
            isExtension = propertyProto.hasReceiver(),
            hasReturnTypeRef = true,
            fqName = c.containerFqName.child(callableName)
        )
    }

    override fun createInitializerStub() {
        val constantInitializer = initializer.value
        if (constantInitializer != null) {
            buildConstantInitializer(constantInitializer, c.nameResolver, callableStub)
        }

        val flags = propertyProto.flags
        if (Flags.HAS_GETTER[flags] && propertyProto.hasGetterFlags()) {
            val getterFlags = propertyProto.getterFlags
            if (Flags.IS_NOT_DEFAULT.get(getterFlags)) {
                val getterStub = KotlinPropertyAccessorStubImpl(callableStub, true, false, true)
                val modifierList = createModifierListStubForDeclaration(
                    getterStub,
                    getterFlags,
                    listOf(VISIBILITY, MODALITY, INLINE, EXTERNAL_ACCESSOR)
                )
                if (Flags.HAS_ANNOTATIONS.get(getterFlags)) {
                    val annotationIds = c.components.annotationLoader.loadCallableAnnotations(
                        protoContainer,
                        propertyProto,
                        AnnotatedCallableKind.PROPERTY_GETTER
                    )
                    createAnnotationStubs(annotationIds, modifierList)
                }
            }
        }

        if (Flags.HAS_SETTER[flags] && propertyProto.hasSetterFlags()) {
            val setterFlags = propertyProto.setterFlags
            if (Flags.IS_NOT_DEFAULT.get(setterFlags)) {
                val setterStub = KotlinPropertyAccessorStubImpl(callableStub, false, true, true)
                val modifierList = createModifierListStubForDeclaration(
                    setterStub,
                    setterFlags,
                    listOf(VISIBILITY, MODALITY, INLINE, EXTERNAL_ACCESSOR)
                )
                if (Flags.HAS_ANNOTATIONS.get(setterFlags)) {
                    val annotationIds = c.components.annotationLoader.loadCallableAnnotations(
                        protoContainer,
                        propertyProto,
                        AnnotatedCallableKind.PROPERTY_SETTER
                    )
                    createAnnotationStubs(annotationIds, modifierList)
                }

                if (propertyProto.hasSetterValueParameter()) {
                    typeStubBuilder.createValueParameterListStub(
                        setterStub,
                        propertyProto,
                        listOf(propertyProto.setterValueParameter),
                        protoContainer,
                        AnnotatedCallableKind.PROPERTY_SETTER
                    )
                }
            }
        }
    }

    private fun calcInitializer(): ConstantInitializer? {
        val classFinder = c.components.classFinder
        val containerClass =
            if (classFinder != null) getSpecialCaseContainerClass(classFinder, c.components.jvmMetadataVersion!!) else null
        val source = protoContainer.source
        val binaryClass = containerClass ?: (source as? KotlinJvmBinarySourceElement)?.binaryClass
        var constantInitializer: ConstantInitializer? = null
        if (binaryClass != null) {
            val callableName = c.nameResolver.getName(propertyProto.name)
            binaryClass.visitMembers(object : KotlinJvmBinaryClass.MemberVisitor {
                override fun visitMethod(name: Name, desc: String): KotlinJvmBinaryClass.MethodAnnotationVisitor? {
                    if (name == callableName && protoContainer is ProtoContainer.Class && protoContainer.kind == ProtoBuf.Class.Kind.ANNOTATION_CLASS) {
                        return object : KotlinJvmBinaryClass.MethodAnnotationVisitor {
                            override fun visitParameterAnnotation(
                                index: Int,
                                classId: ClassId,
                                source: SourceElement
                            ): KotlinJvmBinaryClass.AnnotationArgumentVisitor? = null

                            override fun visitAnnotationMemberDefaultValue(): KotlinJvmBinaryClass.AnnotationArgumentVisitor {
                                return object : KotlinJvmBinaryClass.AnnotationArgumentVisitor {
                                    //todo support all kind of possible annotation arguments
                                    override fun visit(name: Name?, value: Any?) {
                                        if (value != null) {
                                            val returnType = desc.substring(2) // trim leading '()' - empty parameters
                                            constantInitializer = ConstantInitializer(value, null, returnType)
                                        }
                                    }

                                    override fun visitClassLiteral(name: Name?, value: ClassLiteralValue) {}
                                    override fun visitEnum(name: Name?, enumClassId: ClassId, enumEntryName: Name) {}
                                    override fun visitAnnotation(
                                        name: Name?,
                                        classId: ClassId
                                    ): KotlinJvmBinaryClass.AnnotationArgumentVisitor? = null

                                    override fun visitArray(name: Name?): KotlinJvmBinaryClass.AnnotationArrayArgumentVisitor? = null
                                    override fun visitEnd() {}
                                }
                            }

                            override fun visitAnnotation(
                                classId: ClassId,
                                source: SourceElement
                            ): KotlinJvmBinaryClass.AnnotationArgumentVisitor? = null

                            override fun visitEnd() {}
                        }
                    }
                    return null
                }

                override fun visitField(name: Name, desc: String, initializer: Any?): KotlinJvmBinaryClass.AnnotationVisitor? {
                    if (initializer != null && name == callableName) {
                        constantInitializer = ConstantInitializer(initializer, null, desc)
                    }
                    return null
                }
            }, null)
        } else {
            val value = propertyProto.getExtensionOrNull(BuiltInSerializerProtocol.compileTimeValue)
            if (value != null) {
                constantInitializer = ConstantInitializer(null, value, value.type.name)
            }
        }
        return constantInitializer
    }

    /**
     * [org.jetbrains.kotlin.load.kotlin.AbstractBinaryClassAnnotationLoader.getSpecialCaseContainerClass]
     */
    //special cases when data might be stored in a neighbour class
    private fun getSpecialCaseContainerClass(
        classFinder: KotlinClassFinder,
        jvmMetadataVersion: JvmMetadataVersion
    ): KotlinJvmBinaryClass? {
        val isConst = Flags.IS_CONST.get(propertyProto.flags) && Flags.VISIBILITY.get(propertyProto.flags) != ProtoBuf.Visibility.PRIVATE
        if (protoContainer is ProtoContainer.Class && protoContainer.kind == ProtoBuf.Class.Kind.INTERFACE) {
            return classFinder.findKotlinClass(
                protoContainer.classId.createNestedClassId(Name.identifier(JvmAbi.DEFAULT_IMPLS_CLASS_NAME)),
                jvmMetadataVersion
            )
        }
        if (isConst && protoContainer is ProtoContainer.Package) {
            // Const properties in multifile classes are generated into the facade class
            val facadeClassName = (protoContainer.source as? JvmPackagePartSource)?.facadeClassName
            if (facadeClassName != null) {
                // Converting '/' to '.' is fine here because the facade class has a top level ClassId
                return classFinder.findKotlinClass(
                    ClassId.topLevel(FqName(facadeClassName.internalName.replace('/', '.'))),
                    jvmMetadataVersion
                )
            }
        }
        if (protoContainer is ProtoContainer.Class && protoContainer.kind == ProtoBuf.Class.Kind.COMPANION_OBJECT) {
            val outerClass = protoContainer.outerClass
            if (outerClass != null &&
                (outerClass.kind == ProtoBuf.Class.Kind.CLASS || outerClass.kind == ProtoBuf.Class.Kind.ENUM_CLASS ||
                        (JvmProtoBufUtil.isMovedFromInterfaceCompanion(propertyProto) &&
                                (outerClass.kind == ProtoBuf.Class.Kind.INTERFACE ||
                                        outerClass.kind == ProtoBuf.Class.Kind.ANNOTATION_CLASS)))
            ) {
                // Backing fields of properties of a companion object in a class are generated in the outer class
                return (outerClass.source as? KotlinJvmBinarySourceElement)?.binaryClass
            }
        }
        if (protoContainer is ProtoContainer.Package && protoContainer.source is JvmPackagePartSource) {
            val jvmPackagePartSource = protoContainer.source as JvmPackagePartSource

            return jvmPackagePartSource.knownJvmBinaryClass
                ?: classFinder.findKotlinClass(jvmPackagePartSource.classId, jvmMetadataVersion)
        }
        return null
    }
}

private class ConstructorClsStubBuilder(
    parent: StubElement<out PsiElement>,
    outerContext: ClsStubBuilderContext,
    protoContainer: ProtoContainer,
    private val constructorProto: ProtoBuf.Constructor
) : CallableClsStubBuilder(parent, outerContext, protoContainer, emptyList()) {
    override val receiverType: ProtoBuf.Type?
        get() = null

    override val receiverAnnotations: List<ClassIdWithTarget>
        get() = emptyList()

    override val returnType: ProtoBuf.Type?
        get() = null

    override val contextReceiverTypes: List<ProtoBuf.Type>
        get() = emptyList()

    override fun createValueParameterList() {
        typeStubBuilder.createValueParameterListStub(callableStub, constructorProto, constructorProto.valueParameterList, protoContainer)
    }

    override fun createModifierListStub() {
        val modifierListStubImpl = createModifierListStubForDeclaration(callableStub, constructorProto.flags, listOf(VISIBILITY))

        // If constructor is marked as having no annotations, we don't create stubs for it
        if (!Flags.HAS_ANNOTATIONS.get(constructorProto.flags)) return

        val annotationIds = c.components.annotationLoader.loadCallableAnnotations(
            protoContainer, constructorProto, AnnotatedCallableKind.FUNCTION
        )
        createAnnotationStubs(annotationIds, modifierListStubImpl)
    }

    override fun doCreateCallableStub(parent: StubElement<out PsiElement>): StubElement<out PsiElement> {
        val name = (protoContainer as ProtoContainer.Class).classId.shortClassName.ref()
        // Note that arguments passed to stubs here and elsewhere are based on what stabs would be generated based on decompiled code
        // As decompiled code for secondary constructor would be just constructor(args) { /* compiled code */ } every secondary constructor
        // delegated call is not to this (as there is no this keyword) and it has body (while primary does not have one)
        // This info is anyway irrelevant for the purposes these stubs are used
        return if (Flags.IS_SECONDARY.get(constructorProto.flags))
            KotlinConstructorStubImpl(parent, KtStubElementTypes.SECONDARY_CONSTRUCTOR, name, hasBody = true, isDelegatedCallToThis = false)
        else
            KotlinConstructorStubImpl(parent, KtStubElementTypes.PRIMARY_CONSTRUCTOR, name, hasBody = false, isDelegatedCallToThis = false)
    }
}

private data class ConstantInitializer(
    val valueFromClassFile: Any?,
    val builtInValue: ProtoBuf.Annotation.Argument.Value?,
    val constKind: String
)

private fun buildConstantInitializer(
    initializer: ConstantInitializer, nameResolver: NameResolver, parent: StubElement<out PsiElement>
) {
    when (initializer.constKind) {
        "BYTE", "B", "SHORT", "S", "LONG", "J", "INT", "I" -> {
            val number = (initializer.valueFromClassFile ?: initializer.builtInValue?.intValue) as Number
            if (number.toLong() < 0) return
            KotlinConstantExpressionStubImpl(
                parent,
                KtStubElementTypes.INTEGER_CONSTANT,
                ConstantValueKind.INTEGER_CONSTANT,
                StringRef.fromString(number.toString())
            )
        }
        "CHAR", "C" -> KotlinConstantExpressionStubImpl(
            parent,
            KtStubElementTypes.CHARACTER_CONSTANT,
            ConstantValueKind.CHARACTER_CONSTANT,
            StringRef.fromString(String.format("'\\u%04X'", initializer.valueFromClassFile ?: initializer.builtInValue?.intValue))
        )
        "FLOAT", "F" -> {
            val value = (initializer.valueFromClassFile ?: initializer.builtInValue?.floatValue) as Float
            if (value < 0 || value.isNaN() || value.isInfinite()) return
            KotlinConstantExpressionStubImpl(
                parent,
                KtStubElementTypes.FLOAT_CONSTANT,
                ConstantValueKind.FLOAT_CONSTANT,
                StringRef.fromString(value.toString() + "f")
            )
        }
        "DOUBLE", "D" -> {
            val value = (initializer.valueFromClassFile ?: initializer.builtInValue?.doubleValue) as Double
            if (value < 0 || value.isNaN() || value.isInfinite()) return
            KotlinConstantExpressionStubImpl(
                parent,
                KtStubElementTypes.FLOAT_CONSTANT,
                ConstantValueKind.FLOAT_CONSTANT,
                StringRef.fromString(value.toString())
            )
        }
        "BOOLEAN", "Z" -> KotlinConstantExpressionStubImpl(
            parent,
            KtStubElementTypes.BOOLEAN_CONSTANT,
            ConstantValueKind.BOOLEAN_CONSTANT,
            StringRef.fromString(((initializer.valueFromClassFile ?: initializer.builtInValue?.intValue) != 0).toString())
        )
        "STRING", "Ljava/lang/String;" -> {
            val text = (initializer.valueFromClassFile ?: nameResolver.getString(initializer.builtInValue!!.stringValue)) as String
            val stringTemplate = KotlinPlaceHolderStubImpl<KtStringTemplateExpression>(
                parent, KtStubElementTypes.STRING_TEMPLATE
            )
            KotlinPlaceHolderWithTextStubImpl<KtSimpleNameStringTemplateEntry>(
                stringTemplate,
                KtStubElementTypes.LITERAL_STRING_TEMPLATE_ENTRY,
                text
            )
        }
    }
}
