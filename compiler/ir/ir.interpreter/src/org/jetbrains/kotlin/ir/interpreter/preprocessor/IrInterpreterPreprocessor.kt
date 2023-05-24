/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.preprocessor

import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.interpreter.checker.EvaluationMode
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.ConcurrentSkipListSet

abstract class IrInterpreterPreprocessor: IrElementTransformer<IrInterpreterPreprocessorData> {
    // Use map here for performance. Can't use set because `IrFile` is not `Comparable`
    private val processed: MutableMap<IrFile, Unit> = ConcurrentHashMap()
    fun preprocess(file: IrFile, data: IrInterpreterPreprocessorData): IrFile {
        if (file in processed) return file

        processed[file] = Unit
        return file.transform(this, data)
    }
}

class IrInterpreterPreprocessorData(
    val mode: EvaluationMode,
    val irBuiltIns: IrBuiltIns
)