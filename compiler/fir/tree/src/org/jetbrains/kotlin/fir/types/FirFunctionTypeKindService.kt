/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.builtins.functions.FunctionTypeKindExtractor
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

abstract class FirFunctionTypeKindService : FirSessionComponent {
    protected abstract val extractor: FunctionTypeKindExtractor

    fun getKindByClassNamePrefix(packageFqName: FqName, className: String): FunctionTypeKind? {
        return extractor.getFunctionalClassKindWithArity(packageFqName, className)?.kind
    }

    fun hasKindWithSpecificPackage(packageFqName: FqName): Boolean {
        return extractor.hasKindWithSpecificPackage(packageFqName)
    }

    /**
     * Returns all package names for which [getKindByClassNamePrefix] may return a [FunctionTypeKind].
     */
    fun getFunctionKindPackageNames(): Set<FqName> = extractor.getFunctionKindPackageNames()

    /**
     * Whether [getKindByClassNamePrefix] may return a [FunctionTypeKind] added by a compiler plugin.
     */
    fun hasExtensionKinds(): Boolean = extractor.hasExtensionKinds()

    abstract fun extractSingleSpecialKindForFunction(functionSymbol: FirFunctionSymbol<*>): FunctionTypeKind?
    abstract fun extractAllSpecialKindsForFunction(functionSymbol: FirFunctionSymbol<*>): List<FunctionTypeKind>
    abstract fun extractAllSpecialKindsForFunctionTypeRef(typeRef: FirFunctionTypeRef): List<FunctionTypeKind>
}

val FirSession.functionTypeService: FirFunctionTypeKindService by FirSession.sessionComponentAccessor()

/**
 * A [ClassId] can only be a name for a generated function class if it ends with a digit. See [FunctionTypeKind].
 *
 * Checking this first is usually faster than checking `functionTypeService.getKindByClassNamePrefix` or a class cache.
 */
fun ClassId.mayBeSyntheticFunctionClassName(): Boolean = relativeClassName.asString().lastOrNull()?.isDigit() == true
