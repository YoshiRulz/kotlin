/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.actualizer

import org.jetbrains.kotlin.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.IrTypeSystemContext
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.util.callableId
import org.jetbrains.kotlin.ir.util.classIdOrFail
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.mpp.DeclarationSymbolMarker
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.calls.mpp.AbstractExpectActualCompatibilityChecker
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualCompatibility

internal class ExpectActualCollector(
    private val mainFragment: IrModuleFragment,
    private val dependentFragments: List<IrModuleFragment>,
    private val typeSystemContext: IrTypeSystemContext,
    private val diagnosticsReporter: KtDiagnosticReporterWithImplicitIrBasedContext,
) {
    data class Result(val expectToActualMap: MutableMap<IrSymbol, IrSymbol>, val actualDeclarations: ActualDeclarations)

    fun collect(): Result {
        val result = mutableMapOf<IrSymbol, IrSymbol>()
        // Collect and link classes at first to make it possible to expand type aliases on the members linking
        val actualDeclarations = collectActualDeclarations()
        matchAllExpectDeclarations(result, actualDeclarations)
        return Result(result, actualDeclarations)
    }

    private fun collectActualDeclarations(): ActualDeclarations {
        val fragmentsWithActuals = dependentFragments.drop(1) + mainFragment
        return ActualDeclarationsCollector.collectActualsFromFragments(fragmentsWithActuals)
    }

    private fun matchAllExpectDeclarations(
        destination: MutableMap<IrSymbol, IrSymbol>,
        actualDeclarations: ActualDeclarations,
    ) {
        val linkCollector = ExpectActualLinkCollector(destination, actualDeclarations, typeSystemContext, diagnosticsReporter)
        dependentFragments.forEach { linkCollector.visitModuleFragment(it) }
    }
}

internal data class ActualDeclarations(
    // mapping from classId of actual class/typealias to itself/typealias expansion
    val actualClasses: Map<ClassId, IrClassSymbol>,
    // mapping from classId to actual typealias
    val actualTypeAliasesWithoutExpansion: Map<ClassId, IrTypeAliasSymbol>,
    val actualTopLevels: Map<CallableId, List<IrDeclarationWithName>>,
) {
    fun getActualWithoutExpansion(classId: ClassId): IrClassLikeSymbol? {
        return actualTypeAliasesWithoutExpansion[classId] ?: actualClasses[classId]
    }
}

private class ActualDeclarationsCollector {
    companion object {
        fun collectActualsFromFragments(fragments: List<IrModuleFragment>): ActualDeclarations {
            val collector = ActualDeclarationsCollector()
            for (fragment in fragments) {
                collector.collect(fragment)
            }
            return ActualDeclarations(
                collector.actualClasses,
                collector.actualTypeAliasesWithoutExpansion,
                collector.actualTopLevels
            )
        }
    }

    val actualClasses: MutableMap<ClassId, IrClassSymbol> = mutableMapOf()
    val actualTypeAliasesWithoutExpansion: MutableMap<ClassId, IrTypeAliasSymbol> = mutableMapOf()
    val actualTopLevels: MutableMap<CallableId, MutableList<IrDeclarationWithName>> = mutableMapOf()

    private val visitedActualClasses = mutableSetOf<IrClass>()

    private fun collect(element: IrElement) {
        when (element) {
            is IrModuleFragment -> {
                for (file in element.files) {
                    collect(file)
                }
            }
            is IrTypeAlias -> {
                if (!element.isActual) return

                val classId = element.classIdOrFail
                val expandedTypeSymbol = element.expandedType.classifierOrFail as IrClassSymbol

                actualClasses[classId] = expandedTypeSymbol
                actualTypeAliasesWithoutExpansion[classId] = element.symbol

                collect(expandedTypeSymbol.owner)
            }
            is IrClass -> {
                if (element.isExpect || !visitedActualClasses.add(element)) return

                actualClasses[element.classIdOrFail] = element.symbol
                for (declaration in element.declarations) {
                    collect(declaration)
                }
            }
            is IrDeclarationContainer -> {
                for (declaration in element.declarations) {
                    collect(declaration)
                }
            }
            is IrEnumEntry -> {
                recordActualCallable(element) // If enum entry is located inside expect enum, then this code is not executed
            }
            is IrProperty -> {
                if (element.isExpect) return
                recordActualCallable(element)
            }
            is IrFunction -> {
                if (element.isExpect) return
                recordActualCallable(element)
            }
        }
    }

    private fun recordActualCallable(callableDeclaration: IrDeclarationWithName) {
        val callableId = callableDeclaration.callableId!!
        if (callableId.classId == null) {
            actualTopLevels
                .getOrPut(callableId) { mutableListOf() }
                .add(callableDeclaration)
        }
    }
}

private class ExpectActualLinkCollector(
    private val destination: MutableMap<IrSymbol, IrSymbol>,
    private val actualDeclarations: ActualDeclarations,
    typeSystemContext: IrTypeSystemContext,
    private val diagnosticsReporter: KtDiagnosticReporterWithImplicitIrBasedContext,
) : IrElementVisitorVoid {
    private val context = MatchingContext(typeSystemContext)

    override fun visitFunction(declaration: IrFunction) {
        if (declaration.isExpect) {
            matchExpectCallable(declaration)
        }
    }

    override fun visitProperty(declaration: IrProperty) {
        if (declaration.isExpect) {
            matchExpectCallable(declaration)
        }
    }

    private fun matchExpectCallable(declaration: IrDeclarationWithName) {
        val callableId = declaration.callableId!!
        val actualSymbols = actualDeclarations.actualTopLevels[callableId]?.map { it.symbol }.orEmpty()
        matchExpectDeclaration(declaration.symbol, actualSymbols)
    }

    override fun visitClass(declaration: IrClass) {
        if (!declaration.isExpect) return
        val classId = declaration.classIdOrFail
        val expectClassSymbol = declaration.symbol
        val actualClassLikeSymbol = actualDeclarations.getActualWithoutExpansion(classId)
        matchExpectDeclaration(expectClassSymbol, listOfNotNull(actualClassLikeSymbol))
    }

    private fun matchExpectDeclaration(expectSymbol: IrSymbol, actualSymbols: List<IrSymbol>) {
        AbstractExpectActualCompatibilityChecker.matchSingleExpectTopLevelDeclarationAgainstPotentialActuals(
            expectSymbol,
            actualSymbols,
            context
        )
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    private inner class MatchingContext(
        typeSystemContext: IrTypeSystemContext,
    ) : IrExpectActualMatchingContext(typeSystemContext, actualDeclarations.actualClasses) {
        override fun onMatchedClasses(expectClassSymbol: IrClassSymbol, actualClassSymbol: IrClassSymbol) {
            destination[expectClassSymbol] = actualClassSymbol
            recordTypeParametersMapping(destination, expectClassSymbol.owner, actualClassSymbol.owner)
        }

        override fun onMatchedCallables(expectSymbol: IrSymbol, actualSymbol: IrSymbol) {
            destination.recordActualForExpectClass(expectSymbol, actualSymbol)
        }

        override fun onMismatchedMembersFromClassScope(
            expectSymbol: DeclarationSymbolMarker,
            actualSymbolsByIncompatibility: Map<ExpectActualCompatibility.Incompatible<*>, List<DeclarationSymbolMarker>>,
        ) {
            require(expectSymbol is IrSymbol)
            if (actualSymbolsByIncompatibility.isEmpty() && !expectSymbol.owner.containsOptionalExpectation()) {
                diagnosticsReporter.reportMissingActual(expectSymbol)
            }
            for ((incompatibility, actualMemberSymbols) in actualSymbolsByIncompatibility) {
                for (actualSymbol in actualMemberSymbols) {
                    require(actualSymbol is IrSymbol)
                    diagnosticsReporter.reportIncompatibleExpectActual(expectSymbol, actualSymbol, incompatibility)
                }
            }
        }
    }
}
