/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.providers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name


internal class LLFirCodeFragmentSymbolProvider(session: FirSession) : FirSymbolProvider(session) {
    private var generatedClass: FirRegularClass? = null
    override fun getClassLikeSymbolByClassId(classId: ClassId): FirClassLikeSymbol<*>? {
        return generatedClass?.takeIf { it.classId == classId }?.symbol
    }

    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
    }

    @FirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(destination: MutableList<FirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
    }

    @FirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(destination: MutableList<FirPropertySymbol>, packageFqName: FqName, name: Name) {
    }

    override fun getPackage(fqName: FqName): FqName? {
        return null
    }

    override fun computePackageSetWithTopLevelCallables(): Set<String>? {
        return emptySet()
    }

    override fun knownTopLevelClassifiersInPackage(packageFqName: FqName): Set<String>? {
        return emptySet()
    }

    override fun computeCallableNamesInPackage(packageFqName: FqName): Set<Name>? {
        return emptySet()
    }

    fun register(generatedClass: FirRegularClass) {
        this.generatedClass = generatedClass
    }
}