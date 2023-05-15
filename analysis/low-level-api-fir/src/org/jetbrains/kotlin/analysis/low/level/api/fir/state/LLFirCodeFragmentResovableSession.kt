/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.state

import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.KtPsiSourceFile
import org.jetbrains.kotlin.KtPsiSourceFileLinesMapping
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
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
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.builder.BodyBuildingMode
import org.jetbrains.kotlin.fir.builder.RawFirBuilder
import org.jetbrains.kotlin.fir.builder.buildFileAnnotationsContainer
import org.jetbrains.kotlin.fir.builder.buildPackageDirective
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.pipeline.runResolution
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
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
        return (element as? KtFile)?.let { moduleComponents.cache.fileCached(it) { buildFirFileFor(element, moduleComponents) } }
    }

    private fun buildFirFileFor(codeFragment: KtFile, moduleComponents: LLFirModuleResolveComponents): FirFile {
        val codeFragmentModule = codeFragment.getKtModule() as KtCodeFragmentModule
        val argumentReferences = mutableMapOf<String, FirTypeRef>()
        val receiverReferences = mutableMapOf<KtThisExpression, LabeledThis>()

        resolveCodeFragment(codeFragment, receiverReferences, argumentReferences)

        val builder = object : RawFirBuilder(
            moduleComponents.session,
            moduleComponents.scopeProvider,
            bodyBuildingMode = BodyBuildingMode.NORMAL
        ) {
            fun build() = object : Visitor() {
                var generatedFunctionBuilder: FirSimpleFunctionBuilder? = null
                override fun visitPropertyAccessor(accessor: KtPropertyAccessor, data: Unit?): FirElement {
                    return super.visitPropertyAccessor(accessor, data)
                }

                override fun visitThisExpression(expression: KtThisExpression, data: Unit): FirElement {
                    receiverReferences.get(expression)?.let {
                        val parameterName =
                            it.name?.let { label -> Name.identifier(label) } ?: Name.identifier("<this>")
                        val thisParameter =
                            generatedFunctionBuilder!!.valueParameters.singleOrNull { it.name == parameterName } ?: buildValueParameter {
                                this.name = parameterName
                                this.returnTypeRef = it.type
                                moduleData = baseModuleData
                                origin = FirDeclarationOrigin.Source
                                symbol = FirValueParameterSymbol(parameterName)
                                containingFunctionSymbol = generatedFunctionBuilder!!.symbol
                                isCrossinline = false
                                isNoinline = false
                                isVararg = false
                            }.also {
                                generatedFunctionBuilder!!.valueParameters += it
                                codeFragmentModule.parameterResolver(
                                    parameterName.asString(),
                                    parameterName.asString(),
                                    KtCodeFragmentModule.ParameterType.EXTENSION_RECEIVER
                                )
                            }
                        return buildPropertyAccessExpression {
                            typeRef = it.type
                            calleeReference = buildResolvedNamedReference {
                                name = parameterName
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
                        annotationsContainer = buildAnnotationContainerForFile(moduleData, "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

                        for (importDirective in file.importDirectives) {
                            imports += buildImport {
                                source = importDirective.toFirSourceElement()
                                importedFqName = importDirective.importedFqName
                                isAllUnder = importDirective.isAllUnder
                                aliasName = importDirective.aliasName?.let { Name.identifier(it) }
                                aliasSource = importDirective.alias?.nameIdentifier?.toFirSourceElement()
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
                                            ?: error("shouldn't be here")
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
                                valueParameters += argumentReferences.map {
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
                                        codeFragmentModule.parameterResolver(it.key, it.key, KtCodeFragmentModule.ParameterType.ORDINARY)
                                    }
                                }
                                status = FirDeclarationStatusImpl(Visibilities.Public, Modality.FINAL).apply {
                                    isOperator = false
                                    isStatic = true
                                }
                                dispatchReceiverType = null
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
            }.convertElement(codeFragment)
        }
        val firFile = builder.build()
        FirLazyBodiesCalculator.calculateLazyBodies(firFile as FirFile)
        return firFile
    }

    private fun FirFileBuilder.buildAnnotationContainerForFile(
        moduleData: FirModuleData,
        vararg diagnostics: String
    ): FirFileAnnotationsContainer {
        return buildFileAnnotationsContainer {
            this.moduleData = moduleData
            containingFileSymbol = this@buildAnnotationContainerForFile.symbol
            /**
             * applying Suppress("INVISIBLE_*) to file, supposed to instruct frontend to ignore `private`
             * modifier.
             * TODO: investigate why it's not enough for
             * [org.jetbrains.kotlin.idea.k2.debugger.test.cases.K2EvaluateExpressionTestGenerated.SingleBreakpoint.CompilingEvaluator.InaccessibleMembers]
             */
            annotations += buildAnnotationCall {
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
                        initialiazeSuppressAnnotionArguments(*diagnostics)
                    }
                }
                useSiteTarget = AnnotationUseSiteTarget.FILE
                annotationTypeRef = buildResolvedTypeRef {
                    type = annotationType
                }
                argumentMapping = buildAnnotationArgumentMapping {
                    mapping[Name.identifier("names")] = buildVarargArgumentsExpression {
                        initialiazeSuppressAnnotionArguments(*diagnostics)
                    }
                }
                annotationResolvePhase = FirAnnotationResolvePhase.Types
            }
        }
    }

    private fun FirVarargArgumentsExpressionBuilder.initialiazeSuppressAnnotionArguments(vararg diagnostics: String) {
        varargElementType =
            this@LLFirCodeFragmentResovableSession.useSiteFirSession.builtinTypes.stringType
        diagnostics.forEach {
            arguments += buildConstExpression(
                null,
                ConstantValueKind.String,
                it
            )
        }
    }
}

/**
 * Resolve types, arguments and receivers of code fragment in context of source code.
 */
private fun resolveCodeFragment(
    codeFragment: KtFile,
    receiverReferences: MutableMap<KtThisExpression, LabeledThis>,
    argumentReferences: MutableMap<String, FirTypeRef>
) {
    val codeFragmentModule = codeFragment.getKtModule() as KtCodeFragmentModule
    val debugeeSourceFile = codeFragmentModule.place.containingFile as KtFile
    val debugeeFileFirSession = debugeeSourceFile.getFirResolveSession()
    val placementContext = calculateAirContext(debugeeSourceFile, codeFragmentModule)

    val convertedFirExpression = OnAirResolver(debugeeSourceFile).resolve(
        debugeeFileFirSession,
        placementContext!!,
        codeFragment.children.first() as KtElement
    )

    convertedFirExpression?.accept(object : FirVisitorVoid() {
        override fun visitElement(element: FirElement) {
            element.acceptChildren(this)
        }

        override fun visitThisReference(thisReference: FirThisReference) {
            thisReference.source?.psi?.let {
                receiverReferences.getOrPut(it as KtThisExpression) {
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

        override fun visitPropertyAccessExpression(propertyAccessExpression: FirPropertyAccessExpression) {
            val name = (propertyAccessExpression.calleeReference as? FirNamedReference)?.name?.asString()
                ?: return super.visitPropertyAccessExpression(propertyAccessExpression)
            argumentReferences[name] = propertyAccessExpression.typeRef
        }
    })
}

/**
 * calculates gut context to place code fragment for later resolving.
 */
private fun calculateAirContext(
    debugeeSourceFile: KtFile,
    codeFragmentModule: KtCodeFragmentModule
): KtElement? {

    var contexCandidate: KtElement? = null
    debugeeSourceFile.accept(object : KtVisitorVoid() {
        val place = codeFragmentModule.place.calculateAcceptablePlace()
        override fun visitElement(element: PsiElement) {
            if (contexCandidate == null)
                element.acceptChildren(this)
        }

        override fun visitKtElement(element: KtElement) {
            if (contexCandidate == null && element.startOffset >= place.startOffset && element.endOffset <= place.endOffset) {
                contexCandidate = element
            } else {
                element.acceptChildren(this)
            }
        }

        fun PsiElement.calculateAcceptablePlace(): PsiElement = when {
            this is KtKeywordToken ||
                    this is KtNameReferenceExpression ||
                    this is LeafPsiElement && (elementType == IDENTIFIER || elementType is KtKeywordToken) -> context!!.calculateAcceptablePlace()
            context is KtCallExpression -> context!!.calculateAcceptablePlace()
            else -> this
        }
    })
    return contexCandidate
}

private class OnAirResolver(val debugeeSourceFile: KtFile) {
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