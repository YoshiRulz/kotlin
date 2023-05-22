/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.ABSTRACT_MEMBER_NOT_IMPLEMENTED_BY_ENUM_ENTRY
import org.jetbrains.kotlin.fir.declarations.FirEnumEntry
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.utils.isAbstract
import org.jetbrains.kotlin.fir.scopes.processAllCallables
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.utils.addToStdlib.lastIsInstanceOrNull

object FirNotImplementedOverrideSimpleEnumEntryChecker : FirEnumEntryChecker() {
    override fun check(declaration: FirEnumEntry, context: CheckerContext, reporter: DiagnosticReporter) {
        val source = declaration.source ?: return

        // Enum entries with an initializer are handled by FirNotImplementedOverrideChecker since they contain an AnonymousObject.
        if (declaration.initializer != null) return

        val containingEnum = context.containingDeclarations.lastIsInstanceOrNull<FirRegularClass>() ?: return
        val enumScope = containingEnum.unsubstitutedScope(context)

        val notImplementedSymbols = mutableListOf<FirCallableSymbol<*>>()
        enumScope.processAllCallables { symbol ->
            if (symbol.isAbstract) {
                notImplementedSymbols.add(symbol)
            }
        }

        if (notImplementedSymbols.isNotEmpty()) {
            val notImplemented = notImplementedSymbols.first()
            reporter.reportOn(source, ABSTRACT_MEMBER_NOT_IMPLEMENTED_BY_ENUM_ENTRY, declaration.symbol, notImplemented, context)
        }
    }
}
