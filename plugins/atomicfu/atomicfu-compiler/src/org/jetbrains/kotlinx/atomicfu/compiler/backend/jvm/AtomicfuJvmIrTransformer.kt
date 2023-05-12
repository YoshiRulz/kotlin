/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.backend.jvm

import org.jetbrains.kotlin.backend.common.extensions.*
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.util.capitalizeDecapitalize.*
import org.jetbrains.kotlinx.atomicfu.compiler.backend.*
import org.jetbrains.kotlinx.atomicfu.compiler.backend.common.AbstractAtomicfuTransformer
import kotlin.collections.set

private const val ATOMICFU = "atomicfu"
private const val ATOMIC_ARRAY_RECEIVER_SUFFIX = "\$array"
private const val DISPATCH_RECEIVER = "${ATOMICFU}\$dispatchReceiver"
private const val ATOMIC_HANDLER = "${ATOMICFU}\$handler"
private const val ACTION = "${ATOMICFU}\$action"
private const val INDEX = "${ATOMICFU}\$index"
private const val VOLATILE_WRAPPER_SUFFIX = "\$VolatileWrapper"
private const val LOOP = "loop"
private const val UPDATE = "update"

class AtomicfuJvmIrTransformer(
    pluginContext: IrPluginContext,
    override val atomicSymbols: JvmAtomicSymbols
) : AbstractAtomicfuTransformer(pluginContext) {

    override fun transformAtomicProperties(moduleFragment: IrModuleFragment) {
        for (irFile in moduleFragment.files) {
            irFile.transform(JvmAtomicPropertiesTransformer(), null)
        }
    }

    override fun transformAtomicExtensions(moduleFragment: IrModuleFragment) {
        for (irFile in moduleFragment.files) {
            irFile.transform(JvmAtomicExtensionTransformer(), null)
        }
    }

    override fun transformAtomicFunctions(moduleFragment: IrModuleFragment) {
        for (irFile in moduleFragment.files) {
            irFile.transform(JvmAtomicFunctionCallTransformer(), null)
        }
    }

    private val propertyToAtomicHandler = mutableMapOf<IrProperty, IrProperty>()

    private inner class JvmAtomicPropertiesTransformer : AtomicPropertiesTransformer() {

        override fun IrProperty.transformInClassAtomic(parentContainer: IrClass) =
            toVolatilePropertyWithAtomicUpdater(parentContainer)

        override fun IrProperty.transformStaticAtomic(parentContainer: IrDeclarationContainer) {
            val atomicProperty = this
            val wrapperClass = buildWrapperClass(atomicProperty, parentContainer).also {
                // add a static instance of the generated wrapper class to the parent container
                pluginContext.buildClassInstance(it, parentContainer, atomicProperty.visibility, true)
            }
            toVolatilePropertyWithAtomicUpdater(wrapperClass)
            moveFromFileToClass(parentContainer, wrapperClass)
        }

        override fun IrProperty.transformDelegatedAtomic(parentContainer: IrDeclarationContainer) =
            toDelegatedVolatileProperty(parentContainer)

        override fun IrProperty.transformAtomicArray(parentContainer: IrDeclarationContainer) =
            toJavaAtomicArray(parentContainer)

        private fun IrProperty.moveFromFileToClass(
            parentFile: IrDeclarationContainer,
            parentClass: IrClass
        ) {
            parentFile.declarations.remove(this)
            parentClass.declarations.add(this)
            parent = parentClass
        }

        private fun IrProperty.toVolatilePropertyWithAtomicUpdater(parentClass: IrClass) {
            // Atomic property transformation:
            // 1. replace it's backingField with a volatile property of atomic value type
            // 2. create j.u.c.a.Atomic*FieldUpdater for this volatile property to handle it's value atomically
            // val a = atomic(0) ->
            // volatile var a: Int = 0
            // val a$FU = AtomicIntegerFieldUpdater.newUpdater(parentClass, "a")
            //
            // Top-level atomic properties transformation:
            // 1. replace it's backingField with a volatile property of atomic value type
            // 2. wrap this volatile property into the generated class
            // 3. create j.u.c.a.Atomic*FieldUpdater for the volatile property to handle it's value atomically
            // val a = atomic(0) ->
            // class A$ParentFile$VolatileWrapper { volatile var a: Int = 0 }
            // val a$FU = AtomicIntegerFieldUpdater.newUpdater(A$ParentFile$VolatileWrapper::class, "a")
            val property = this
            backingField = buildVolatileBackingField(property, parentClass)
            updateGetter(parentClass, irBuiltIns)
            parentClass.addJavaAtomicFieldUpdater(property).also {
                registerAtomicHandler(it)
            }
        }

        private fun IrProperty.toJavaAtomicArray(parentContainer: IrDeclarationContainer) {
            // Replace atomicfu array classes with the corresponding atomic arrays from j.u.c.a.:
            // val intArr = atomicArrayOfNulls<Any?>(5) ->
            // val intArr = AtomicReferenceArray(5)
            val property = this
            backingField = buildJavaAtomicArrayField(property, parentContainer)
            // update property accessors
            updateGetter(parentContainer, irBuiltIns)
            registerAtomicHandler(property)
        }

        private fun IrProperty.toDelegatedVolatileProperty(parent: IrDeclarationContainer) {
            backingField?.let {
                it.initializer?.let {
                    val initializer = it.expression as IrCall
                    if (initializer.isAtomicFactory()) {
                        // Property delegated to atomic factory invocation:
                        // 1. replace it's backingField with a volatile property of value type
                        // 2. transform getter/setter
                        // var a by atomic(0) ->
                        // volatile var a: Int = 0
                        // todo: make this function look more consistent

                        val volatileField = buildVolatileBackingField(this, parent)
                        parent.declarations.add(volatileField)
                        backingField = null
                        getter?.transformAccessor(volatileField, getter?.dispatchReceiverParameter?.capture())
                        setter?.transformAccessor(volatileField, setter?.dispatchReceiverParameter?.capture())
                    } else {
                        // Property delegated to the atomic property:
                        // 1. delegate it's accessors to get/set of the backingField of the atomic delegate
                        // (that is already transformed to a volatile field of value type)
                        // val _a = atomic(0)
                        // var a by _a ->
                        // volatile var _a: Int = 0
                        // var a by _a
                        val atomicProperty = initializer.getCorrespondingProperty()
                        val volatileField = atomicProperty.backingField!!
                        backingField = null
                        if (atomicProperty.isTopLevel()) {
                            with(atomicSymbols.createBuilder(symbol)) {
                                val wrapper = getStaticVolatileWrapperInstance(atomicProperty)
                                getter?.transformAccessor(volatileField, getProperty(wrapper, null))
                                setter?.transformAccessor(volatileField, getProperty(wrapper, null))
                            }
                        } else {
                            if (this.parent == atomicProperty.parent) {
                                //class A {
                                //    val _a = atomic()
                                //    var a by _a
                                //}
                                getter?.transformAccessor(volatileField, getter?.dispatchReceiverParameter?.capture())
                                setter?.transformAccessor(volatileField, setter?.dispatchReceiverParameter?.capture())
                            } else {
                                //class A {
                                //    val _a = atomic()
                                //    inner class B {
                                //        var a by _a
                                //    }
                                //}
                                val thisReceiver = atomicProperty.parentAsClass.thisReceiver
                                getter?.transformAccessor(volatileField, thisReceiver?.capture())
                                setter?.transformAccessor(volatileField, thisReceiver?.capture())
                            }
                        }
                    }
                }
            }
        }

        private fun IrFunction.transformAccessor(volatileField: IrField, parent: IrExpression?) {
            val accessor = this
            with(atomicSymbols.createBuilder(symbol)) {
                body = irExprBody(
                    irReturn(
                        if (accessor.isGetter) {
                            irGetField(parent, volatileField)
                        } else {
                            irSetField(parent, volatileField, accessor.valueParameters[0].capture())
                        }
                    )
                )
            }
        }

        private fun IrProperty.registerAtomicHandler(atomicHandlerProperty: IrProperty) {
            propertyToAtomicHandler[this] = atomicHandlerProperty
        }

        private fun IrClass.addJavaAtomicFieldUpdater(
            property: IrProperty
        ): IrProperty {
            val volatileField = requireNotNull(property.backingField) { "BackingField of property $property should not be null" }
            val atomicFieldUpdater = with(atomicSymbols.createBuilder(volatileField.symbol)) {
                irJavaAtomicFieldUpdater(volatileField, this@addJavaAtomicFieldUpdater)
            }
            return addProperty {
                name = atomicFieldUpdater.name
                visibility = property.visibility // equal to the atomic property visibility
                isVar = false
            }.apply {
                backingField = atomicFieldUpdater
                addStaticGetter(irBuiltIns)
            }
        }

        private fun buildJavaAtomicArrayField(
            property: IrProperty,
            parentContainer: IrDeclarationContainer
        ): IrField {
            val atomicArrayField = requireNotNull(property.backingField) { "BackingField of atomic array $property should not be null" }
            val atomicArrayClass = atomicSymbols.getAtomicArrayClassByAtomicfuArrayType(atomicArrayField.type)
            // todo fix this, pass generated array field as the second argument, here is the BUG NOW
            val initializer = atomicArrayField.initializer?.expression
            val isInitializedInInitBlock = atomicArrayField.isInitializedInInitBlock(parentContainer)
            if (initializer == null && !isInitializedInInitBlock) error("Atomic array $property was not initialized")
            val size = initializer?.let { (it as IrConstructorCall).getArraySizeArgument() }
            return with(atomicSymbols.createBuilder(atomicArrayField.symbol)) {
                irJavaAtomicArrayField(
                    atomicArrayField.name,
                    atomicArrayClass,
                    atomicArrayField.isFinal,
                    atomicArrayField.isStatic,
                    atomicArrayField.annotations,
                    size,
                    initializer.dispatchReceiver,
                    parentContainer
                ).also {
                    if (isInitializedInInitBlock) {
                        updateInitBlockFieldInitialization(parentContainer, atomicArrayField.symbol, it.symbol)
                    }
                }
            }
        }

        private fun updateInitBlockArrayInitialization(
            parentContainer: IrDeclarationContainer,
            oldFieldSymbol: IrFieldSymbol,
            volatileFieldSymbol: IrFieldSymbol
        ): IrSetField? {
            for (declaration in parentContainer.declarations) {
                if (declaration is IrAnonymousInitializer) {
                    declaration.body.statements.singleOrNull {
                        it is IrSetField && it.symbol == oldFieldSymbol
                    }?.let {
                        it as IrSetField
                        val size = (it.value as IrConstructorCall).getArraySizeArgument()
                        with(atomicSymbols.createBuilder(volatileFieldSymbol)) {
                            irJavaAtomicArrayField(
                                oldFieldSymbol.owner.name,
                                atomicArrayClass,
                                oldFieldSymbol.owner.isFinal,
                                oldFieldSymbol.owner.isStatic,
                                oldFieldSymbol.owner.annotations,
                                size,

                            )
                        }
                        with(atomicSymbols.createBuilder(volatileFieldSymbol) {
                            irJavaAtomicArrayField(
                                atomicArrayField.name,oldFiel
                                atomicArrayClass,
                                atomicArrayField.isFinal,
                                atomicArrayField.isStatic,
                                atomicArrayField.annotations,
                                size,
                                initializer.dispatchReceiver,
                                parentContainer
                            )

                        }
                        // todo IrExpressionBodyImpl(
                        //                newJavaAtomicArray(arrayClass, size, dispatchReceiver)
                        //            )
                        val irSetField = buildSetField(volatileFieldSymbol, it.receiver, initializationValue)
                        declaration.body.statements.add(irSetField)
                        declaration.body.statements.remove(it)
                    }
                }
            }
            return null
        }

        private fun buildWrapperClass(atomicProperty: IrProperty, parentContainer: IrDeclarationContainer): IrClass =
            atomicSymbols.buildClass(
                FqName(getVolatileWrapperClassName(atomicProperty)),
                ClassKind.CLASS,
                parentContainer
            ).apply {
                val irClass = this
                irClass.visibility = atomicProperty.visibility
                addConstructor {
                    isPrimary = true
                }.apply {
                    body = atomicSymbols.createBuilder(symbol).irBlockBody(startOffset, endOffset) {
                        +irDelegatingConstructorCall(context.irBuiltIns.anyClass.owner.constructors.single())
                        +IrInstanceInitializerCallImpl(startOffset, endOffset, irClass.symbol, context.irBuiltIns.unitType)
                    }
                    this.visibility = DescriptorVisibilities.PRIVATE // constructor of the wrapper class should be private
                }
            }

        // todo rename
        private fun IrProperty.isTopLevel(): Boolean =
            parent is IrClass && (parent as IrClass).name.asString().endsWith(VOLATILE_WRAPPER_SUFFIX)
    }

    private inner class JvmAtomicExtensionTransformer : AtomicExtensionTransformer() {

        override fun IrDeclarationContainer.transformAllAtomicExtensions() {
            declarations.filter { it is IrFunction && it.isAtomicExtension() }.forEach { atomicExtension ->
                atomicExtension as IrFunction
                declarations.add(transformAtomicExtension(atomicExtension, this, false))
                declarations.add(transformAtomicExtension(atomicExtension, this, true))
                declarations.remove(atomicExtension)
            }
        }

        private fun transformAtomicExtension(atomicExtension: IrFunction, parent: IrDeclarationContainer, isArrayReceiver: Boolean): IrFunction {
            val mangledName = mangleAtomicExtensionName(atomicExtension.name.asString(), isArrayReceiver)
            val valueType = atomicExtension.extensionReceiverParameter!!.type.atomicToValueType()
            return pluginContext.irFactory.buildFun {
                name = Name.identifier(mangledName)
                isInline = true
                visibility = atomicExtension.visibility
            }.apply {
                val newDeclaration = this
                extensionReceiverParameter = null
                dispatchReceiverParameter = atomicExtension.dispatchReceiverParameter?.deepCopyWithSymbols(this)
                if (isArrayReceiver) {
                    addValueParameter(DISPATCH_RECEIVER, irBuiltIns.anyNType)
                    addValueParameter(ATOMIC_HANDLER, atomicSymbols.getAtomicArrayClassByValueType(valueType).defaultType)
                    addValueParameter(INDEX, irBuiltIns.intType)
                } else {
                    addValueParameter(DISPATCH_RECEIVER, irBuiltIns.anyNType)
                    addValueParameter(ATOMIC_HANDLER, atomicSymbols.getFieldUpdaterType(valueType))
                }
                atomicExtension.valueParameters.forEach { addValueParameter(it.name, it.type) }
                // the body will be transformed later by `AtomicFUTransformer`
                body = atomicExtension.body?.deepCopyWithSymbols(this)
                body?.transform(
                    object : IrElementTransformerVoid() {
                        override fun visitReturn(expression: IrReturn): IrExpression = super.visitReturn(
                            if (expression.returnTargetSymbol == atomicExtension.symbol) {
                                with(atomicSymbols.createBuilder(newDeclaration.symbol)) {
                                    irReturn(expression.value)
                                }
                            } else {
                                expression
                            }
                        )
                    }, null
                )
                returnType = atomicExtension.returnType
                this.parent = parent
            }
        }
    }

    private data class AtomicFieldInfo(val dispatchReceiver: IrExpression?, val atomicHandler: IrExpression)

    private inner class JvmAtomicFunctionCallTransformer : AtomicFunctionCallTransformer() {

        override fun transformedCallOnAtomic(
            expression: IrCall,
            functionName: String,
            valueType: IrType,
            castType: IrType?,
            getPropertyReceiver: IrExpression,
            parentFunction: IrFunction?
        ): IrExpression {
            with(atomicSymbols.createBuilder(expression.symbol)) {
                getAtomicFieldInfo(getPropertyReceiver, parentFunction)?.let { (dispatchReceiver, atomicHandler) ->
                    // aClass.<get-a>().CAS(expected, updated) -> a$FU.CAS(aClass, expected, updated)
                    return callFieldUpdater(
                        fieldUpdaterSymbol = atomicSymbols.getJucaAFUClass(valueType),
                        functionName = functionName,
                        dispatchReceiver = atomicHandler,
                        obj = dispatchReceiver,
                        valueArguments = expression.getValueArguments(),
                        castType = castType,
                        isBooleanReceiver = valueType.isBoolean()
                    )
                } ?: return expression
            }
        }

        override fun transformedCallOnAtomicArrayElement(
            expression: IrCall,
            functionName: String,
            valueType: IrType,
            getPropertyReceiver: IrExpression,
            parentFunction: IrFunction?
        ): IrCall {
            with(atomicSymbols.createBuilder(expression.symbol)) {
                getAtomicFieldInfo(getPropertyReceiver, parentFunction)?.let { (_, atomicHandler) ->
                    // aClass.intArr[4].CAS(expected, updated) -> aClass.javaIntArr.CAS(4, expected, updated)
                    return callAtomicArray(
                        arrayClassSymbol = atomicHandler.type.classOrNull!!,
                        functionName = functionName,
                        dispatchReceiver = atomicHandler,
                        index = getPropertyReceiver.getArrayElementIndex(parentFunction),
                        valueArguments = expression.getValueArguments(),
                        isBooleanReceiver = valueType.isBoolean()
                    )
                } ?: return expression
            }
        }

        override fun transformedAtomicfuInlineFunctionCall(
            expression: IrCall,
            functionName: String,
            valueType: IrType,
            getPropertyReceiver: IrExpression,
            isArrayReceiver: Boolean,
            parentFunction: IrFunction?
        ): IrCall {
            with(atomicSymbols.createBuilder(expression.symbol)) {
                getAtomicFieldInfo(getPropertyReceiver, parentFunction)?.let { (dispatchReceiver, atomicHandler) ->
                    // If the inline atomicfu loop function was invoked
                    // a.loop { value -> a.compareAndSet(value, 777) }
                    // then loop function is generated to replace this declaration.
                    // `AtomicInt.loop(action: (Int) -> Unit)` for example will be replaced with
                    // inline fun <T> atomicfu$loop(atomicHandler: AtomicIntegerFieldUpdater, action: (Int) -> Unit) {
                    //     while (true) {
                    //         val cur = atomicfu$handler.get()
                    //         atomicfu$action(cur)
                    //     }
                    // }
                    // And the invocation in place will be transformed:
                    // a.atomicfu$loop(atomicHandler, action)
                    requireNotNull(parentFunction) { "Parent function of this call ${expression.render()} is null" }
                    val loopFunc = parentFunction.parentDeclarationContainer.getOrBuildInlineLoopFunction(
                        functionName = functionName,
                        valueType = if (valueType.isBoolean()) irBuiltIns.intType else valueType,
                        isArrayReceiver = isArrayReceiver
                    )
                    val action = (expression.getValueArgument(0) as IrFunctionExpression).apply {
                        function.body?.transform(this@JvmAtomicFunctionCallTransformer, parentFunction)
                        if (function.valueParameters[0].type.isBoolean()) {
                            function.valueParameters[0].type = irBuiltIns.intType
                            function.returnType = irBuiltIns.intType
                        }
                    }
                    return irCallWithArgs(
                        symbol = loopFunc.symbol,
                        dispatchReceiver = parentFunction.containingFunction.dispatchReceiverParameter?.capture(),
                        extensionReceiver = null,
                        valueArguments = if (isArrayReceiver) {
                            val index = getPropertyReceiver.getArrayElementIndex(parentFunction)
                            listOf(atomicHandler, index, action)
                        } else {
                            listOf(atomicHandler, action, dispatchReceiver)
                        }
                    )
                } ?: return expression
            }
        }

        override fun transformedAtomicExtensionCall(
            expression: IrCall,
            transformedAtomicExtension: IrSimpleFunction,
            getPropertyReceiver: IrExpression,
            isArrayReceiver: Boolean,
            parentFunction: IrFunction?
        ): IrCall {
            with(atomicSymbols.createBuilder(expression.symbol)) {
                getAtomicFieldInfo(getPropertyReceiver, parentFunction)?.let { (dispatchReceiver, atomicHandler) ->
                    // Transform invocation of the kotlinx.atomicfu.Atomic* class extension functions,
                    // delegating them to the corresponding transformed atomic extensions:
                    // for atomic property recevers:
                    // inline fun foo$atomicfu(dispatchReceiver: Any?, handler: j.u.c.a.AtomicIntegerFieldUpdater, arg': Int) { ... }
                    // for atomic array element receivers:
                    // inline fun foo$atomicfu$array(dispatchReceiver: Any?, handler: j.u.c.a.AtomicIntegerArray, index: Int, arg': Int) { ... }

                    // The invocation on the atomic property will be transformed:
                    // a.foo(arg) -> a.foo$atomicfu(dispatchReceiver, atomicHandler, arg)
                    // The invocation on the atomic array element will be transformed:
                    // a.foo(arg) -> a.foo$atomicfu$array(dispatchReceiver, atomicHandler, index, arg)
                    return callAtomicExtension(
                        symbol = transformedAtomicExtension.symbol,
                        dispatchReceiver = expression.dispatchReceiver,
                        syntheticValueArguments = if (isArrayReceiver) {
                            listOf(dispatchReceiver, atomicHandler, getPropertyReceiver.getArrayElementIndex(parentFunction))
                        } else {
                            listOf(dispatchReceiver, atomicHandler)
                        },
                        valueArguments = expression.getValueArguments()
                    )
                } ?: return expression
            }
        }

        override fun visitGetValue(expression: IrGetValue, data: IrFunction?): IrExpression {
            // TODO: abstract out or leave like this: needs refactor and verification!!!!!
            // For transformed atomic extension functions
            // replace old value parameters with the new parameters of the transformed declaration:
            // inline fun foo$atomicfu(dispatchReceiver: Any?, handler: j.u.c.a.AtomicIntegerFieldUpdater, arg': Int) {
            //     arg -> arg`
            //}
            if (expression.symbol is IrValueParameterSymbol) {
                val valueParameter = expression.symbol.owner as IrValueParameter
                val parent = valueParameter.parent
                if (data != null && data.isTransformedAtomicExtension() &&
                    parent is IrFunctionImpl && !parent.isTransformedAtomicExtension() &&
                    parent.origin != IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
                ) {
                    val index = valueParameter.index
                    if (index < 0 && !valueParameter.type.isAtomicValueType()) {
                        // index == -1 for `this` parameter
                        return data.dispatchReceiverParameter?.capture() ?: error { "Dispatch receiver of ${data.render()} is null" }
                    }
                    if (index >= 0) {
                        val shift = if (data.name.asString().endsWith(ATOMIC_ARRAY_RECEIVER_SUFFIX)) 3 else 2
                        val transformedValueParameter = data.valueParameters[index + shift]
                        return buildGetValue(
                            expression.startOffset,
                            expression.endOffset,
                            transformedValueParameter.symbol
                        )
                    }
                }
            }
            return super.visitGetValue(expression, data)
        }

        override fun IrExpression.isArrayElementReceiver(parentFunction: IrFunction?): Boolean {
            val receiver = this
            return when {
                receiver is IrCall -> {
                    receiver.isArrayElementGetter()
                }
                receiver.isThisReceiver() -> {
                    if (parentFunction != null && parentFunction.isTransformedAtomicExtension()) {
                        val atomicHandler = parentFunction.valueParameters[1].capture()
                        atomicSymbols.isAtomicArrayHandlerType(atomicHandler.type)
                    } else false
                }
                else -> false
            }
        }

        override fun IrFunction.isTransformedAtomicExtension(): Boolean {
            val isArrayReceiver = name.asString().endsWith(ATOMIC_ARRAY_RECEIVER_SUFFIX)
            return if (isArrayReceiver) checkSyntheticArrayElementExtensionParameter() else checkSyntheticAtomicExtensionParameters()
        }

        private fun JvmAtomicfuIrBuilder.getAtomicFieldInfo(
            receiver: IrExpression,
            parentFunction: IrFunction?
        ): AtomicFieldInfo? {
            // For the given function call receiver of atomic type returns:
            // the dispatchReceiver and the atomic handler of the corresponding property
            when {
                receiver is IrCall -> {
                    // Receiver is a property getter call
                    val isArrayReceiver = receiver.isArrayElementGetter()
                    val getAtomicProperty = if (isArrayReceiver) receiver.dispatchReceiver as IrCall else receiver
                    val atomicProperty = getAtomicProperty.getCorrespondingProperty()
                    val dispatchReceiver = getAtomicProperty.dispatchReceiver.let {dispatchReceiver ->
                        val isObjectReceiver = dispatchReceiver?.type?.classOrNull?.owner?.kind == ClassKind.OBJECT
                        if (dispatchReceiver == null || isObjectReceiver) {
                            if (getAtomicProperty.symbol.owner.returnType.isAtomicValueType()) {
                                // for top-level atomic properties get wrapper class instance as a parent
                                getProperty(getStaticVolatileWrapperInstance(atomicProperty), null)
                            } else if (isObjectReceiver && getAtomicProperty.symbol.owner.returnType.isAtomicArrayType()) {
                                dispatchReceiver
                            } else null
                        } else dispatchReceiver
                    }
                    // atomic property is handled by the Atomic*FieldUpdater instance
                    // atomic array elements handled by the Atomic*Array instance
                    val atomicHandler = propertyToAtomicHandler[atomicProperty]
                        ?: error("No atomic handler found for the atomic property ${atomicProperty.render()}")
                    return AtomicFieldInfo(
                        dispatchReceiver = dispatchReceiver,
                        atomicHandler = getProperty(
                            atomicHandler,
                            if (isArrayReceiver && dispatchReceiver?.type?.classOrNull?.owner?.kind != ClassKind.OBJECT) dispatchReceiver else null
                        )
                    )
                }
                receiver.isThisReceiver() -> {
                    // Receiver is <this> extension receiver of transformed atomic extension declaration.
                    // The old function before `AtomicExtensionTransformer` application:
                    // inline fun foo(dispatchReceiver: Any?, handler: j.u.c.a.AtomicIntegerFieldUpdater, arg': Int) {
                    //    this().lazySet(arg)
                    //}
                    // By this moment the atomic extension has it's signature transformed,
                    // but still has the untransformed body copied from the old declaration:
                    // inline fun foo$atomicfu(dispatchReceiver: Any?, handler: j.u.c.a.AtomicIntegerFieldUpdater, arg': Int) {
                    //    this().lazySet(arg) <----
                    //}
                    // The dispatchReceiver and the atomic handler for this receiver are the corresponding arguments
                    // passed to the transformed declaration
                    return if (parentFunction != null && parentFunction.isTransformedAtomicExtension()) {
                        val params = parentFunction.valueParameters.take(2).map { it.capture() }
                        AtomicFieldInfo(params[0], params[1])
                    } else null
                }
                else -> error("Unsupported type of atomic receiver expression: ${receiver.render()}")
            }
        }

        private fun IrExpression.getArrayElementIndex(parentFunction: IrFunction?): IrExpression =
            when {
                this is IrCall -> getValueArgument(0)!!
                this.isThisReceiver() -> {
                    require(parentFunction != null)
                    parentFunction.valueParameters[2].capture()
                }
                else -> error("Unsupported type of atomic receiver expression: ${this.render()}")
            }

        private fun IrFunction.checkSyntheticArrayElementExtensionParameter(): Boolean {
            if (valueParameters.size < 3) return false
            return valueParameters[0].name.asString() == DISPATCH_RECEIVER && valueParameters[0].type == irBuiltIns.anyNType &&
                    valueParameters[1].name.asString() == ATOMIC_HANDLER && atomicSymbols.isAtomicArrayHandlerType(valueParameters[1].type) &&
                    valueParameters[2].name.asString() == INDEX && valueParameters[2].type == irBuiltIns.intType
        }

        private fun IrFunction.checkSyntheticAtomicExtensionParameters(): Boolean {
            if (valueParameters.size < 2) return false
            return valueParameters[0].name.asString() == DISPATCH_RECEIVER && valueParameters[0].type == irBuiltIns.anyNType &&
                    valueParameters[1].name.asString() == ATOMIC_HANDLER && atomicSymbols.isAtomicFieldUpdaterType(valueParameters[1].type)
        }

        private fun IrDeclarationContainer.getOrBuildInlineLoopFunction(
            functionName: String,
            valueType: IrType,
            isArrayReceiver: Boolean
        ): IrSimpleFunction {
            val parent = this
            val mangledName = mangleAtomicExtensionName(functionName, isArrayReceiver)
            val updaterType =
                if (isArrayReceiver) atomicSymbols.getAtomicArrayType(valueType) else atomicSymbols.getFieldUpdaterType(valueType)
            findDeclaration<IrSimpleFunction> {
                it.name.asString() == mangledName && it.valueParameters[0].type == updaterType
            }?.let { return it }
            return pluginContext.irFactory.buildFun {
                name = Name.identifier(mangledName)
                isInline = true
                visibility = DescriptorVisibilities.PRIVATE
            }.apply {
                dispatchReceiverParameter = (parent as? IrClass)?.thisReceiver?.deepCopyWithSymbols(this)
                if (functionName == LOOP) {
                    if (isArrayReceiver) generateAtomicfuArrayLoop(valueType) else generateAtomicfuLoop(valueType)
                } else {
                    if (isArrayReceiver) generateAtomicfuArrayUpdate(functionName, valueType) else generateAtomicfuUpdate(
                        functionName,
                        valueType
                    )
                }
                this.parent = parent
                parent.declarations.add(this)
            }
        }

        private fun IrSimpleFunction.generateAtomicfuLoop(valueType: IrType) {
            addValueParameter(ATOMIC_HANDLER, atomicSymbols.getFieldUpdaterType(valueType))
            addValueParameter(ACTION, atomicSymbols.function1Type(valueType, irBuiltIns.unitType))
            addValueParameter(DISPATCH_RECEIVER, irBuiltIns.anyNType)
            body = with(atomicSymbols.createBuilder(symbol)) {
                atomicfuLoopBody(valueType, valueParameters)
            }
            returnType = irBuiltIns.unitType
        }

        private fun IrSimpleFunction.generateAtomicfuArrayLoop(valueType: IrType) {
            val atomicfuArrayClass = atomicSymbols.getAtomicArrayClassByValueType(valueType)
            addValueParameter(ATOMIC_HANDLER, atomicfuArrayClass.defaultType)
            addValueParameter(INDEX, irBuiltIns.intType)
            addValueParameter(ACTION, atomicSymbols.function1Type(valueType, irBuiltIns.unitType))
            body = with(atomicSymbols.createBuilder(symbol)) {
                atomicfuArrayLoopBody(atomicfuArrayClass, valueParameters)
            }
            returnType = irBuiltIns.unitType
        }

        private fun IrSimpleFunction.generateAtomicfuUpdate(functionName: String, valueType: IrType) {
            addValueParameter(ATOMIC_HANDLER, atomicSymbols.getFieldUpdaterType(valueType))
            addValueParameter(ACTION, atomicSymbols.function1Type(valueType, valueType))
            addValueParameter(DISPATCH_RECEIVER, irBuiltIns.anyNType)
            body = with(atomicSymbols.createBuilder(symbol)) {
                atomicfuUpdateBody(functionName, valueParameters, valueType)
            }
            returnType = if (functionName == UPDATE) irBuiltIns.unitType else valueType
        }

        private fun IrSimpleFunction.generateAtomicfuArrayUpdate(functionName: String, valueType: IrType) {
            val atomicfuArrayClass = atomicSymbols.getAtomicArrayClassByValueType(valueType)
            addValueParameter(ATOMIC_HANDLER, atomicfuArrayClass.defaultType)
            addValueParameter(INDEX, irBuiltIns.intType)
            addValueParameter(ACTION, atomicSymbols.function1Type(valueType, valueType))
            body = with(atomicSymbols.createBuilder(symbol)) {
                atomicfuArrayUpdateBody(functionName, atomicfuArrayClass, valueParameters)
            }
            returnType = if (functionName == UPDATE) irBuiltIns.unitType else valueType
        }
    }

    private fun getStaticVolatileWrapperInstance(atomicProperty: IrProperty): IrProperty {
        val volatileWrapperClass = atomicProperty.parent as IrClass
        return (volatileWrapperClass.parent as IrDeclarationContainer).declarations.singleOrNull {
            it is IrProperty && it.backingField != null &&
                    it.backingField!!.type.classOrNull == volatileWrapperClass.symbol
        } as? IrProperty
            ?: error("Static instance of ${volatileWrapperClass.name.asString()} is missing in ${volatileWrapperClass.parent}")
    }

    private fun getVolatileWrapperClassName(property: IrProperty) =
        property.name.asString().capitalizeAsciiOnly() + '$' +
                (if (property.parent is IrFile) (property.parent as IrFile).name else property.parent.kotlinFqName.asString()).substringBefore('.') +
                VOLATILE_WRAPPER_SUFFIX
}
