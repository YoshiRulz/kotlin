/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrFileSymbolImpl
import org.jetbrains.kotlin.utils.memoryOptimizedPlus
import org.jetbrains.kotlin.ir.util.NaiveSourceBasedFileEntryImpl
import org.jetbrains.kotlin.ir.util.transformDeclarationsFlat
import org.jetbrains.kotlin.ir.util.transformFlat

fun moveAllClassesToSeparateFiles(context: JsIrBackendContext, moduleFragment: IrModuleFragment) {
    fun createFile(file: IrFile, klass: IrClass): IrFile =
        IrFileImpl(
            fileEntry = NaiveSourceBasedFileEntryImpl(file.generatePathFor(klass)),
            fqName = file.fqName,
            symbol = IrFileSymbolImpl(),
            module = file.module
        ).also {
            it.annotations = it.annotations memoryOptimizedPlus file.annotations
            it.declarations += klass
            klass.parent = it
        }

    moduleFragment.files.transformFlat { file ->
        // We don't have to split declarations with a single class
        if (file.declarations.size <= 1) return@transformFlat null

        val classesToMoveOut = mutableListOf<IrClass>()

        file.transformDeclarationsFlat {
            if (it is IrClass && it.visibility != DescriptorVisibilities.LOCAL) {
                classesToMoveOut += it
                emptyList()
            } else {
                null
            }
        }

        when {
            classesToMoveOut.isEmpty() -> null
            else -> listOf(file) + classesToMoveOut.map { klass ->
                createFile(file, klass).also { context.mapping.chunkToOriginalFile[it] = file }
            }
        }
    }
}

private const val KT_EXTENSION = ".kt"

private fun IrFile.generatePathFor(klass: IrClass): String {
    return "${path.removeSuffix(KT_EXTENSION)}__${klass.name}$KT_EXTENSION"
}