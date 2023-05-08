/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.checkUpperBoundViolated
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.createSubstitutorForUpperBoundViolationCheck
import org.jetbrains.kotlin.fir.analysis.checkers.toTypeArgumentsWithSourceInfo
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isUnit
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol

object FirTypeArgumentsOfQualifierOfGetClassCallChecker : FirGetClassCallChecker() {
    override fun check(expression: FirGetClassCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val lhs = expression.argument as? FirResolvedQualifier ?: return
        checkResolvedQualifier(lhs, context, reporter, containingElementIsGetClassCall = true)
    }
}

object FirTypeArgumentsOfQualifierOfCallableReferenceChecker : FirCallableReferenceAccessChecker() {
    override fun check(expression: FirCallableReferenceAccess, context: CheckerContext, reporter: DiagnosticReporter) {
        val lhs = expression.explicitReceiver as? FirResolvedQualifier ?: return
        checkResolvedQualifier(lhs, context, reporter, containingElementIsGetClassCall = false)
    }
}

private fun checkResolvedQualifier(
    expression: FirResolvedQualifier,
    context: CheckerContext,
    reporter: DiagnosticReporter,
    containingElementIsGetClassCall: Boolean
) {
    val correspondingClass = expression.symbol
    val typeParameters = when {
        correspondingClass is FirTypeAliasSymbol -> {
            /*
             * If qualified typealias stays in position of expression (expression.coneType != Unit) and this typealias
             *   points to some object, then we should assume that it has no type parameters, even if typealias itself
             *   has those parameters
             *
             * object SomeObject
             *
             * typealias AliasedObject<T> = SomeObject
             *
             * val x = AliasedObject // ok
             */
            when {
                expression.typeRef.coneType.isUnit -> correspondingClass.typeParameterSymbols
                expression.typeRef.coneType.fullyExpandedType(context.session)
                    .toRegularClassSymbol(context.session)
                    ?.classKind == ClassKind.OBJECT -> emptyList()
                else -> correspondingClass.typeParameterSymbols
            }
        }
        expression.resolvedToCompanionObject -> emptyList()
        else -> correspondingClass?.typeParameterSymbols.orEmpty()
    }
    val typeArguments = expression.typeArguments.toTypeArgumentsWithSourceInfo()
    if (typeArguments.size != typeParameters.size) {
        val shouldReportError = typeArguments.isNotEmpty() || !containingElementIsGetClassCall
        if (shouldReportError) {
            val symbol = expression.symbol
            if (symbol != null) {
                reporter.reportOn(expression.source, FirErrors.WRONG_NUMBER_OF_TYPE_ARGUMENTS, typeParameters.size, symbol, context)
            }
        }
        return
    }
    if (typeArguments.any { it.coneTypeProjection !is ConeKotlinType }) {
        return
    }
    val substitutor = createSubstitutorForUpperBoundViolationCheck(typeParameters, typeArguments, context.session)
    checkUpperBoundViolated(
        context,
        reporter,
        typeParameters,
        typeArguments,
        substitutor,
    )
}
