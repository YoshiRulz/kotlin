/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.util

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.backend.generators.AnnotationGenerator
import org.jetbrains.kotlin.fir.backend.generators.CallAndReferenceGenerator
import org.jetbrains.kotlin.fir.backend.generators.DelegatedMemberGenerator
import org.jetbrains.kotlin.fir.backend.generators.FakeOverrideGenerator
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.descriptors.FirModuleDescriptor
import org.jetbrains.kotlin.fir.descriptors.FirPackageFragmentDescriptor
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.getContainingClass
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.types.ConeLookupTagBasedType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.ir.builders.declarations.buildValueParameter
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrExternalPackageFragmentSymbolImpl
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.load.kotlin.JvmPackagePartSource
import org.jetbrains.kotlin.name.Name

/**
 * This workaround over [Fir2IrConverter.createModuleFragmentWithSignaturesIfNeeded] to convert cross module FIR to IR.
 * Ideally we should maintain symbol table only, but at current stage we have to fill storages with dependent constructions.
 */
@OptIn(SymbolInternals::class)
fun createModuleFragmentWithSignaturesIfNeededWorkaround(
    session: FirSession,
    scopeSession: ScopeSession,
    firFiles: List<FirFile>,
    languageVersionSettings: LanguageVersionSettings,
    fir2IrExtensions: Fir2IrExtensions,
    irMangler: KotlinMangler.IrMangler,
    irFactory: IrFactory,
    visibilityConverter: Fir2IrVisibilityConverter,
    specialSymbolProvider: Fir2IrSpecialSymbolProvider,
    irGenerationExtensions: Collection<IrGenerationExtension>,
    generateSignatures: Boolean,
    kotlinBuiltIns: KotlinBuiltIns,
    commonMemberStorage: Fir2IrCommonMemberStorage,
    initializedIrBuiltIns: IrBuiltInsOverFir?
): Fir2IrResult {
    val moduleDescriptor = FirModuleDescriptor(session, kotlinBuiltIns)
    val components = Fir2IrComponentsStorage(
        session,
        scopeSession,
        commonMemberStorage.symbolTable,
        irFactory,
        commonMemberStorage.signatureComposer,
        fir2IrExtensions,
        generateSignatures
    )
    val converter = Fir2IrConverter(moduleDescriptor, components)

    components.converter = converter
    components.classifierStorage = Fir2IrClassifierStorage(components, commonMemberStorage)
    components.delegatedMemberGenerator = DelegatedMemberGenerator(components)
    components.declarationStorage = Fir2IrDeclarationStorage(components, moduleDescriptor, commonMemberStorage)

    components.visibilityConverter = visibilityConverter
    components.typeConverter = Fir2IrTypeConverter(components)
    val irBuiltIns = initializedIrBuiltIns ?: IrBuiltInsOverFir(
        components, languageVersionSettings, moduleDescriptor, irMangler,
        true
    )
    components.irBuiltIns = irBuiltIns
    val conversionScope = Fir2IrConversionScope()
    val fir2irVisitor = Fir2IrVisitor(components, conversionScope)
    components.builtIns = Fir2IrBuiltIns(components, specialSymbolProvider)
    components.annotationGenerator = AnnotationGenerator(components)
    components.fakeOverrideGenerator = FakeOverrideGenerator(components, conversionScope)
    components.callGenerator = CallAndReferenceGenerator(components, fir2irVisitor, conversionScope)
    components.irProviders = listOf(FirIrProvider(components))


    val irModuleFragment = IrModuleFragmentImpl(moduleDescriptor, irBuiltIns)

    val allFirFiles = buildList {
        addAll(firFiles)
        addAll(session.createFilesWithGeneratedDeclarations())
    }

    firFiles.first().accept(object : FirVisitorVoid() {
        override fun visitElement(element: FirElement) {
            element.acceptChildren(this)
        }

        override fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef) {
            val symbol =
                (resolvedTypeRef.type as? ConeLookupTagBasedType)?.lookupTag?.toSymbol(session) ?: return
            val firClass = symbol.fir as? FirRegularClass ?: return super.visitResolvedTypeRef(resolvedTypeRef) //TODO("$symbol")
            if (components.classifierStorage.getCachedIrClass(firClass) != null)
                return
            val sig = IdSignature.CommonSignature(firClass.symbol.classId.asFqNameString(), firClass.name.asString(), 0, 0)
            val irClass =
                components.classifierStorage.registerIrClass(
                    firClass,
                    IrExternalPackageFragmentImpl(
                        IrExternalPackageFragmentSymbolImpl(
                            FirPackageFragmentDescriptor(
                                firClass.symbol.classId.packageFqName,
                                FirModuleDescriptor(session, DefaultBuiltIns.Instance)
                            )
                        ), firClass.symbol.classId.packageFqName
                    ),
                    IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB
                ).also {
                    it.thisReceiver = buildValueParameter(it) {
                        name = Name.identifier("\$this")
                        type = IrSimpleTypeImpl(it.symbol, false, emptyList(), emptyList())
                    }
                }
            components.classifierStorage.processClassHeader(firClass)
            val irSymbol = irClass.symbol

            commonMemberStorage.symbolTable.declareClass(
                sig,
                { irSymbol }) {
                irClass
            }
            super.visitResolvedTypeRef(resolvedTypeRef)
        }

        override fun visitFunctionCall(functionCall: FirFunctionCall) {
            val symbol = functionCall.calleeReference.toResolvedCallableSymbol() as FirCallableSymbol<*>
            (symbol.fir.containerSource as? JvmPackagePartSource)?.facadeClassName
            if (symbol.fir is FirConstructor) {
                val firClass = symbol.fir.getContainingClass(session) ?: TODO()
                val irClass = components.classifierStorage.getCachedIrClass(firClass) ?: TODO()
                components.declarationStorage.getCachedIrConstructor(symbol.fir as FirConstructor)
                    ?: components.declarationStorage.createIrConstructor(symbol.fir as FirConstructor, irClass)
            }
            super.visitFunctionCall(functionCall)
        }

    })
    fir2IrExtensions.registerDeclarations(commonMemberStorage.symbolTable)
    converter.runSourcesConversion(
        allFirFiles, irModuleFragment, irGenerationExtensions, fir2irVisitor, fir2IrExtensions,
        false
    )
    return Fir2IrResult(irModuleFragment, components, moduleDescriptor)
}
