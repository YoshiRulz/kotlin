/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.builder.FirSimpleFunctionBuilder
import org.jetbrains.kotlin.fir.declarations.builder.buildSimpleFunction
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.impl.FirImplicitBooleanTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitIntTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitNullableAnyTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitStringTypeRef
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

class FirClassAnySynthesizedMemberScope(
    private val useSiteMemberScope: FirClassUseSiteMemberScope,
    private val lookupTag: ConeClassLikeLookupTag,
    private val baseModuleData: FirModuleData,
    private val dispatchReceiverType: ConeClassLikeType,
    classSource: KtSourceElement?,
) : FirTypeScope() {
    // Pair stores here the synthesized Any member symbol at .first and its overridden symbol at .second
    private val synthesizedIndex = mutableMapOf<Name, Pair<FirNamedFunctionSymbol, FirNamedFunctionSymbol>>()

    private val synthesizedSource = classSource?.fakeElement(KtFakeSourceElementKind.DataClassGeneratedMembers)

    override fun processClassifiersByNameWithSubstitution(name: Name, processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit) {
        useSiteMemberScope.processClassifiersByNameWithSubstitution(name, processor)
    }

    override fun processDeclaredConstructors(processor: (FirConstructorSymbol) -> Unit) {
        useSiteMemberScope.processDeclaredConstructors(processor)
    }

    override fun processDirectOverriddenPropertiesWithBaseScope(
        propertySymbol: FirPropertySymbol,
        processor: (FirPropertySymbol, FirTypeScope) -> ProcessorAction
    ): ProcessorAction {
        return useSiteMemberScope.processDirectOverriddenPropertiesWithBaseScope(propertySymbol, processor)
    }

    override fun processDirectOverriddenFunctionsWithBaseScope(
        functionSymbol: FirNamedFunctionSymbol,
        processor: (FirNamedFunctionSymbol, FirTypeScope) -> ProcessorAction,
    ): ProcessorAction {
        val (_, overridden) = synthesizedIndex[functionSymbol.name]?.takeIf { it.first == functionSymbol }
            ?: return useSiteMemberScope.processDirectOverriddenFunctionsWithBaseScope(functionSymbol, processor)
        return processor(overridden, useSiteMemberScope)
    }

    override fun getCallableNames(): Set<Name> {
        return useSiteMemberScope.getCallableNames()
    }

    override fun getClassifierNames(): Set<Name> {
        return useSiteMemberScope.getClassifierNames()
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        useSiteMemberScope.processPropertiesByName(name, processor)
    }

    override fun processFunctionsByName(name: Name, processor: (FirNamedFunctionSymbol) -> Unit) {
        if (name !in ANY_MEMBER_NAMES) {
            useSiteMemberScope.processFunctionsByName(name, processor)
            return
        }
        useSiteMemberScope.processFunctionsByName(name) { fromUseSiteScope ->
            if (fromUseSiteScope.rawStatus.modality == Modality.FINAL) {
                processor(fromUseSiteScope)
            } else {
                val matchedSomeAnyMember = when (name) {
                    OperatorNameConventions.HASH_CODE, OperatorNameConventions.TO_STRING ->
                        fromUseSiteScope.valueParameterSymbols.isEmpty() && !fromUseSiteScope.isExtension &&
                                fromUseSiteScope.fir.contextReceivers.isEmpty()
                    else ->
                        fromUseSiteScope.fir.isEquals()
                }
                val hasSameReceiver =
                    dispatchReceiverType.lookupTag == (fromUseSiteScope.dispatchReceiverType as? ConeClassLikeType)?.lookupTag
                if (!matchedSomeAnyMember || hasSameReceiver) {
                    processor(fromUseSiteScope)
                } else {
                    val (synthesized, _) = synthesizedIndex.getOrPut(name) {
                        when (name) {
                            OperatorNameConventions.EQUALS -> generateEqualsFunction()
                            OperatorNameConventions.HASH_CODE -> generateHashCodeFunction()
                            OperatorNameConventions.TO_STRING -> generateToStringFunction()
                            else -> shouldNotBeCalled()
                        }.symbol to fromUseSiteScope
                    }
                    processor(synthesized)
                }
            }
        }
    }

    private fun generateEqualsFunction() =
        buildSimpleFunction {
            generateSyntheticFunction(OperatorNameConventions.EQUALS, isOperator = true)
            returnTypeRef = FirImplicitBooleanTypeRef(source)
            this.valueParameters.add(
                buildValueParameter {
                    this.name = Name.identifier("other")
                    origin = FirDeclarationOrigin.Synthetic
                    moduleData = baseModuleData
                    this.returnTypeRef = FirImplicitNullableAnyTypeRef(null)
                    this.symbol = FirValueParameterSymbol(this.name)
                    containingFunctionSymbol = this@buildSimpleFunction.symbol
                    isCrossinline = false
                    isNoinline = false
                    isVararg = false
                }
            )
        }

    private fun generateHashCodeFunction() =
        buildSimpleFunction {
            generateSyntheticFunction(OperatorNameConventions.HASH_CODE)
            returnTypeRef = FirImplicitIntTypeRef(source)
        }

    private fun generateToStringFunction() =
        buildSimpleFunction {
            generateSyntheticFunction(OperatorNameConventions.TO_STRING)
            returnTypeRef = FirImplicitStringTypeRef(source)
        }

    private fun FirSimpleFunctionBuilder.generateSyntheticFunction(
        name: Name,
        isOperator: Boolean = false,
    ) {
        this.source = synthesizedSource
        moduleData = baseModuleData
        origin = FirDeclarationOrigin.Synthetic
        this.name = name
        status = FirResolvedDeclarationStatusImpl(Visibilities.Public, Modality.OPEN, EffectiveVisibility.Public).apply {
            this.isOperator = isOperator
        }
        symbol = FirNamedFunctionSymbol(CallableId(lookupTag.classId, name))
        dispatchReceiverType = this@FirClassAnySynthesizedMemberScope.dispatchReceiverType
    }

    companion object {
        private val ANY_MEMBER_NAMES = hashSetOf(
            OperatorNameConventions.HASH_CODE, OperatorNameConventions.EQUALS, OperatorNameConventions.TO_STRING
        )
    }
}