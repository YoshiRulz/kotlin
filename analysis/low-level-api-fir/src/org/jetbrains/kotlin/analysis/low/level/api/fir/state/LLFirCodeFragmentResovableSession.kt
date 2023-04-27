/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.state

import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.KtPsiSourceFile
import org.jetbrains.kotlin.KtPsiSourceFileLinesMapping
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.FirLazyBodiesCalculator
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.LLFirCodeFragmentSymbolProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.project.structure.KtCodeFragmentModule
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.getKtModule
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.diagnostics.KtPsiDiagnostic
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirFunctionTarget
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.builder.BodyBuildingMode
import org.jetbrains.kotlin.fir.builder.RawFirBuilder
import org.jetbrains.kotlin.fir.builder.buildFileAnnotationsContainer
import org.jetbrains.kotlin.fir.builder.buildPackageDirective
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotationResolvePhase
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.pipeline.runResolution
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.scopes.getDeclaredConstructors
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.scopes.kotlinScopeProvider
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirImplicitUnitTypeRef
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.lexer.KtKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens.IDENTIFIER
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.toKtPsiSourceElement
import org.jetbrains.kotlin.types.ConstantValueKind


internal val FirSession.codeFragmentSymbolProvider: LLFirCodeFragmentSymbolProvider by FirSession.sessionComponentAccessor()

private class DebuggeeSourceFileImportsFetcher(val file: KtFile) : KtVisitorVoid() {
    private val pathSegments = file.packageFqName.pathSegments().map { it.identifier }.toTypedArray()
    val fqNames = mutableSetOf<FqName>()

    /**
     * TODO: add imports from source file.
     */
    var scopeFqName = pathSegments
    private inline fun scope(name: String, body: () -> Unit) {
        val oldScope = scopeFqName
        scopeFqName = arrayOf(*scopeFqName, name)
        body()
        scopeFqName = oldScope
    }

    override fun visitElement(element: PsiElement) {
        element.acceptChildren(this)
    }

    override fun visitClassOrObject(classOrObject: KtClassOrObject) {
        classOrObject.name ?: return
        scope(classOrObject.name!!) {
            fqNames += FqName.fromSegments(scopeFqName.toList())
            classOrObject.companionObjects.forEach {
                it.acceptChildren(this)
            }
            classOrObject.acceptChildren(this)
        }
    }

    override fun visitProperty(property: KtProperty) {
        fqNames += FqName.fromSegments(listOf(*scopeFqName, property.name))
    }

    override fun visitNamedFunction(function: KtNamedFunction) {
        fqNames += FqName.fromSegments(listOf(*scopeFqName, function.name))
    }
}

internal class LabeledThis(val name: String?, val type: FirTypeRef)

internal class LLFirCodeFragmentResovableSession(
    ktModule: KtModule,
    useSiteSessionFactory: (KtModule) -> LLFirSession
) : LLFirResolvableResolveSession(ktModule, useSiteSessionFactory) {
    override fun getModuleKind(module: KtModule): ModuleKind {
        return ModuleKind.RESOLVABLE_MODULE
    }

    override fun getDiagnostics(element: KtElement, filter: DiagnosticCheckerFilter): List<KtPsiDiagnostic> {
        TODO("Not yet implemented")
    }

    override fun collectDiagnosticsForFile(ktFile: KtFile, filter: DiagnosticCheckerFilter): Collection<KtPsiDiagnostic> {
        TODO("Not yet implemented")
    }

    override fun getOrBuildFirFor(element: KtElement): FirElement? {
        val moduleComponents = getModuleComponentsForElement(element)
        val codeFragmentModule = element.getKtModule() as KtCodeFragmentModule
        val debugeeSourceFile = codeFragmentModule.sourceFile
        val debugeeFileFirSession = debugeeSourceFile.getFirResolveSession()
        val properties = mutableMapOf<String, FirTypeRef>()

        val needle = run {
            var needle_: KtElement? = null
            debugeeSourceFile.accept(object : KtVisitorVoid() {
                val place = codeFragmentModule.place.placeCalculator()
                override fun visitElement(element: PsiElement) {
                    if (needle_ == null)
                        element.acceptChildren(this)
                }

                override fun visitKtElement(element: KtElement) {
                    if (needle_ == null && element.startOffset >= place.startOffset && element.endOffset <= place.endOffset) {
                        needle_ = element
                    } else {
                        element.acceptChildren(this)
                    }
                }

                fun PsiElement.placeCalculator(): PsiElement = when {
                    this is KtKeywordToken ||
                            this is KtNameReferenceExpression ||
                            this is LeafPsiElement && (elementType == IDENTIFIER || elementType is KtKeywordToken) -> context!!.placeCalculator()
                            context is KtCallExpression -> context!!.placeCalculator()
                    else -> this
                }
            })
            needle_
        }


        val convertedFirExpression = OnAirResolver(debugeeSourceFile).resolve(
            debugeeFileFirSession,
            needle!!,
            element.children.first() as KtElement
        )

        val thisAccessors = mutableMapOf<KtThisExpression, LabeledThis>()

        convertedFirExpression?.accept(object : FirVisitorVoid() {
            override fun visitElement(element: FirElement) {
                element.acceptChildren(this)
            }

            override fun visitThisReference(thisReference: FirThisReference) {
                thisReference.source?.psi?.let {
                    thisAccessors.getOrPut(it as KtThisExpression) {
                        when (thisReference.boundSymbol) {
                            is FirAnonymousFunctionSymbol -> {
                                val symbol = thisReference.boundSymbol as FirAnonymousFunctionSymbol
                                LabeledThis(
                                    symbol.label!!.name,
                                    symbol.receiverParameter!!.typeRef
                                )
                            }
                            else -> TODO()
                        }
                    }
                }
                super.visitThisReference(thisReference)
            }

            override fun visitPropertyAccessor(propertyAccessor: FirPropertyAccessor) {
                super.visitPropertyAccessor(propertyAccessor)
            }
        })

        element.accept(object : KtVisitorVoid() {
            override fun visitElement(element: PsiElement) {
                element.acceptChildren(this)
            }

            override fun visitReferenceExpression(expression: KtReferenceExpression) {
                expression as? KtNameReferenceExpression ?: return
                expression.getReferencedName().let { name ->
                    codeFragmentModule.properties
                        .find { it.name == name }
                        ?.let {
                            properties[name] = it.resolveToFirSymbolOfType<FirPropertySymbol>(debugeeFileFirSession).fir.returnTypeRef
                        }
                }
            }
        })
        val importsFetcher = DebuggeeSourceFileImportsFetcher(debugeeSourceFile)
        debugeeSourceFile.accept(importsFetcher)
        val builder = object : RawFirBuilder(
            moduleComponents.session,
            moduleComponents.scopeProvider,
            bodyBuildingMode = BodyBuildingMode.NORMAL
        ) {
            fun build() = object : Visitor() {
                override fun visitPropertyAccessor(accessor: KtPropertyAccessor, data: Unit?): FirElement {
                    return super.visitPropertyAccessor(accessor, data)
                }

                /**
                 * TODO: add differenciation of `this`:
                 * expression: {
                 *      this.apply {
                 *          doSmth(this)
                 *      }
                 * }
                 * first `this` from debugee context, and second from expression's one.
                 */
                internal var generatedFunctionBuilder: FirSimpleFunctionBuilder? = null
                override fun visitThisExpression(expression: KtThisExpression, data: Unit): FirElement {
                    thisAccessors.get(expression)?.let {
                        val parameterName =
                            it.name?.let { label -> Name.identifier("${'$'}this${'$'}$label") } ?: Name.identifier("${'$'}this")
                        val thisParameter = buildValueParameter {
                            this.name = parameterName
                            this.returnTypeRef = it.type
                            moduleData = baseModuleData
                            origin = FirDeclarationOrigin.Source
                            symbol = FirValueParameterSymbol(parameterName)
                            containingFunctionSymbol = generatedFunctionBuilder!!.symbol
                            isCrossinline = false
                            isNoinline = false
                            isVararg = false
                        }
                        generatedFunctionBuilder!!.valueParameters += thisParameter
                        return buildPropertyAccessExpression {
                            typeRef = it.type
                            calleeReference = buildResolvedNamedReference {
                                name = parameterName // TODO: add alias names here.
                                resolvedSymbol = thisParameter.symbol
                            }
                        }
                    } ?: return super.visitThisExpression(expression, data)
                }

                override fun visitKtFile(file: KtFile, data: Unit): FirElement {
                    return buildFile {
                        symbol = FirFileSymbol()
                        source = file.toFirSourceElement()
                        moduleData = baseModuleData
                        origin = FirDeclarationOrigin.Source
                        name = file.name
                        sourceFile = KtPsiSourceFile(file)
                        sourceFileLinesMapping = KtPsiSourceFileLinesMapping(file)
                        packageDirective = buildPackageDirective {
                            packageFqName = FqName.ROOT
                            source = file.packageDirective?.toKtPsiSourceElement()
                        }
                        annotationsContainer = buildFileAnnotationsContainer {
                            moduleData = baseModuleData
                            containingFileSymbol = this@buildFile.symbol
                            source = file.toKtPsiSourceElement()
                            /**
                             * applying Suppress("INVISIBLE_*) to file, supposed to instruct frontend to ignore `private`
                             * modifier.
                             * TODO: investigate why it's not enough for
                             * [org.jetbrains.kotlin.idea.k2.debugger.test.cases.K2EvaluateExpressionTestGenerated.SingleBreakpoint.CompilingEvaluator.InaccessibleMembers]
                             */
                            annotations += buildAnnotationCall {
                                source = file.toFirSourceElement()
                                val annotationClassIdLookupTag = ClassId(
                                    StandardNames.FqNames.suppress.parent(),
                                    StandardNames.FqNames.suppress.shortName()
                                ).toLookupTag()
                                val annotationType = ConeClassLikeTypeImpl(
                                    annotationClassIdLookupTag,
                                    emptyArray(),
                                    isNullable = false
                                )
                                calleeReference = buildResolvedNamedReference {
                                    val annotationTypeSymbol = (annotationType.toSymbol(useSiteFirSession) as? FirRegularClassSymbol)
                                        ?: return@buildAnnotationCall

                                    val constructorSymbol =
                                        annotationTypeSymbol.unsubstitutedScope(
                                            useSiteFirSession,
                                            useSiteFirSession.getScopeSession(),
                                            withForcedTypeCalculator = false,
                                            memberRequiredPhase = null
                                        )
                                            .getDeclaredConstructors().firstOrNull() ?: return@buildAnnotationCall
                                    resolvedSymbol = constructorSymbol
                                    name = constructorSymbol.name
                                }
                                argumentList = buildArgumentList {
                                    arguments += buildVarargArgumentsExpression {
                                        initialiazeSuppressAnnotionArguments()
                                    }
                                }
                                useSiteTarget = AnnotationUseSiteTarget.FILE
                                annotationTypeRef = buildResolvedTypeRef {
                                    source = file.toFirSourceElement()
                                    type = annotationType
                                }
                                argumentMapping = buildAnnotationArgumentMapping {
                                    mapping[Name.identifier("names")] = buildVarargArgumentsExpression {
                                        initialiazeSuppressAnnotionArguments()
                                    }
                                }
                                annotationResolvePhase = FirAnnotationResolvePhase.Types
                            }
                        }

                        for (importDirective in file.importDirectives) {
                            imports += buildImport {
                                source = importDirective.toFirSourceElement()
                                importedFqName = importDirective.importedFqName
                                isAllUnder = importDirective.isAllUnder
                                aliasName = importDirective.aliasName?.let { Name.identifier(it) }
                                aliasSource = importDirective.alias?.nameIdentifier?.toFirSourceElement()
                            }
                        }
                        importsFetcher.fqNames.forEach { fqName ->
                            imports += buildImport {
                                source = file.toFirSourceElement()
                                importedFqName = fqName
                                isAllUnder = false
                            }
                        }
                        for (declaration in file.declarations) {
                            declarations += when (declaration) {
                                is KtDestructuringDeclaration -> buildErrorTopLevelDestructuringDeclaration(declaration.toFirSourceElement())
                                else -> convertElement(declaration) as FirDeclaration
                            }
                        }
                        val name = codeFragmentModule.codeFragmentClassName
                        val generatedClassId = ClassId(FqName.ROOT, name)
                        val generatedClass = buildRegularClass {
                            moduleData = baseModuleData
                            origin = FirDeclarationOrigin.Synthetic
                            this.name = name
                            symbol = FirRegularClassSymbol(generatedClassId)
                            status = FirResolvedDeclarationStatusImpl(
                                Visibilities.Public,
                                Modality.FINAL,
                                EffectiveVisibility.Public
                            ).apply {
                                isExpect = false
                                isActual = false
                                isCompanion = false
                                isInner = false
                                isData = false
                                isInline = false
                                isExternal = false
                                isFun = false
                            }
                            classKind = ClassKind.OBJECT
                            scopeProvider = this@LLFirCodeFragmentResovableSession.useSiteFirSession.kotlinScopeProvider
                            superTypeRefs += this@LLFirCodeFragmentResovableSession.useSiteFirSession.builtinTypes.anyType


                            val generatedConstructor = buildPrimaryConstructor {
                                source = file.toFirSourceElement()
                                moduleData = baseModuleData
                                origin = FirDeclarationOrigin.Source
                                symbol = FirConstructorSymbol(generatedClassId)
                                status = FirDeclarationStatusImpl(Visibilities.Public, Modality.FINAL).apply {
                                    isExpect = false
                                    isActual = false
                                    isInner = false
                                    isFromSealedClass = false
                                    isFromEnumClass = false
                                }
                                returnTypeRef = buildResolvedTypeRef {
                                    type = ConeClassLikeTypeImpl(
                                        this@buildRegularClass.symbol.toLookupTag(),
                                        emptyArray(),
                                        false
                                    )
                                }
                                delegatedConstructor = buildDelegatedConstructorCall {
                                    val superType = useSiteFirSession.builtinTypes.anyType.type
                                    constructedTypeRef = superType.toFirResolvedTypeRef()
                                    calleeReference = buildResolvedNamedReference {
                                        val superClassConstructorSymbol = superType.toRegularClassSymbol(useSiteFirSession)
                                            ?.declaredMemberScope(useSiteFirSession)
                                            ?.getDeclaredConstructors()
                                            ?.firstOrNull { it.valueParameterSymbols.isEmpty() }
                                            ?: error("shouldn't be here") //.toRegularClassSymbol(useSiteFirSession)!!
                                        this@buildResolvedNamedReference.name = superClassConstructorSymbol.name
                                        resolvedSymbol = superClassConstructorSymbol
                                    }
                                    isThis = false
                                }
                            }
                            val generatedFunctionReturnTarget = FirFunctionTarget(null, false)
                            val generatedFunction = buildSimpleFunction {
                                source = file.toFirSourceElement()
                                moduleData = baseModuleData
                                origin = FirDeclarationOrigin.Source
                                val functionName = codeFragmentModule.codeFragmentFunctionName
                                this.name = functionName
                                symbol = FirNamedFunctionSymbol(CallableId(FqName.ROOT, null, functionName))
                                generatedFunctionBuilder = this
                                val danglingExpression = file.children.filter {
                                    it is KtExpression || it is KtBlockExpression
                                }.map {
                                    super.convertElement(it as KtElement)
                                }.single()

                                val dangingReturnType = when (danglingExpression) {
                                    is FirBlock -> (danglingExpression.statements.last() as? FirExpression)?.typeRef
                                        ?: FirImplicitUnitTypeRef(file.toKtPsiSourceElement())
                                    else -> (danglingExpression as? FirExpression)?.typeRef
                                        ?: FirImplicitUnitTypeRef(file.toKtPsiSourceElement())
                                }
                                returnTypeRef = dangingReturnType
                                valueParameters += properties.map {
                                    buildValueParameter {
                                        val parameterName = Name.identifier(it.key)
                                        this.name = parameterName
                                        this.returnTypeRef = it.value
                                        moduleData = baseModuleData
                                        origin = FirDeclarationOrigin.Source
                                        symbol = FirValueParameterSymbol(parameterName)
                                        containingFunctionSymbol = this@buildSimpleFunction.symbol
                                        isCrossinline = false
                                        isNoinline = false
                                        isVararg = false
                                    }
                                }
                                val names = valueParameters.map { it.name.asString() }
                                codeFragmentModule.valueParameters.filter { !names.contains(it.name!!) }.forEach {
                                    valueParameters += buildValueParameter {
                                        val parameterName = it.name?.let { it1 -> Name.identifier(it1) } ?: return@forEach
                                        this.name = parameterName
                                        this.returnTypeRef =
                                            it.resolveToFirSymbolOfType<FirValueParameterSymbol>(debugeeFileFirSession).resolvedReturnTypeRef
                                        moduleData = baseModuleData
                                        origin = FirDeclarationOrigin.Source
                                        symbol = FirValueParameterSymbol(parameterName)
                                        containingFunctionSymbol = this@buildSimpleFunction.symbol
                                        isCrossinline = false
                                        isNoinline = false
                                        isVararg = false
                                    }
                                }
                                status = FirDeclarationStatusImpl(Visibilities.Public, Modality.FINAL).apply {
                                    isOperator = false
                                    isStatic = true
                                }
                                dispatchReceiverType = null//currentDispatchReceiverType()
                                body = buildBlock {
                                    statements += when (danglingExpression) {
                                        is FirBlock -> {
                                            buildReturnExpression {
                                                source = danglingExpression.source
                                                result = danglingExpression
                                                this.target = generatedFunctionReturnTarget
                                            }
                                        }
                                        is FirExpression -> buildReturnExpression {
                                            source = danglingExpression.source
                                            result = danglingExpression
                                            this.target = generatedFunctionReturnTarget
                                        }
                                        else -> TODO()
                                    }
                                }
                            }
                            generatedFunctionReturnTarget.bind(generatedFunction)
                            declarations.add(generatedConstructor)
                            declarations.add(generatedFunction)
                        }
                        declarations.add(generatedClass)
                        this@LLFirCodeFragmentResovableSession.useSiteFirSession.codeFragmentSymbolProvider.register(generatedClass)
                    }
                }
            }.convertElement(element)
        }
        val firFile = builder.build()
        FirLazyBodiesCalculator.calculateLazyBodies(firFile as FirFile)
        return firFile
    }

    private fun FirVarargArgumentsExpressionBuilder.initialiazeSuppressAnnotionArguments() {
        varargElementType =
            this@LLFirCodeFragmentResovableSession.useSiteFirSession.builtinTypes.stringType
        arguments += buildConstExpression(
            null,
            ConstantValueKind.String,
            "INVISIBLE_REFERENCE"
        )
        arguments += buildConstExpression(
            null,
            ConstantValueKind.String,
            "INVISIBLE_MEMBER"
        )
    }
}

internal class OnAirResolver(val debugeeSourceFile: KtFile) {
    fun resolve(session: LLFirResolveSession, place: KtElement, expression: KtElement): FirElement? {
        var convertedElement: FirElement? = null
        val builder = object : RawFirBuilder(session.useSiteFirSession, session.useSiteFirSession.kotlinScopeProvider) {
            fun build() = object : Visitor() {
                override fun convertElement(element: KtElement): FirElement? {
                    if (element == place) {
                        convertedElement = convertElement(expression)
                        return convertedElement
                    }
                    return super.convertElement(element)
                }
            }.convertElement(debugeeSourceFile)
        }
        val modifiedFile = builder.build() as? FirFile ?: return null
        val (_, _) = session.useSiteFirSession.runResolution(listOf(modifiedFile))
        return convertedElement
    }
}