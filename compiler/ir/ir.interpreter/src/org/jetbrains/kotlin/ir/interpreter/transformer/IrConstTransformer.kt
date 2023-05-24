/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.transformer

import org.jetbrains.kotlin.constant.ErrorValue
import org.jetbrains.kotlin.constant.EvaluatedConstTracker
import org.jetbrains.kotlin.incremental.components.InlineConstTracker
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.interpreter.IrInterpreter
import org.jetbrains.kotlin.ir.interpreter.IrInterpreterConfiguration
import org.jetbrains.kotlin.ir.interpreter.checker.*
import org.jetbrains.kotlin.ir.interpreter.preprocessor.IrInterpreterKCallableNamePreprocessor
import org.jetbrains.kotlin.ir.interpreter.preprocessor.IrInterpreterPreprocessorData
import org.jetbrains.kotlin.ir.interpreter.property
import org.jetbrains.kotlin.ir.interpreter.toConstantValue
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer

// Note: `preprocessors` and `transformers` are using per file cache. In K2 we are visiting one file twice: after fir2ir and in lowering.
// If in between some new declaration appear we will not process them. But this should not be a problem because lowering is placed at the top.
// The only problem can occur when and if we move lowering below `FunctionInlining`. In that case we must visit only `IrReturnableBlock`s and
// somehow "reset" cache.
private val preprocessors = setOf(IrInterpreterKCallableNamePreprocessor())

private val transformers = setOf(
    IrConstExpressionTransformer(),
    IrConstDeclarationAnnotationTransformer(),
    IrConstTypeAnnotationTransformer()
)

private val lock = Object()

fun IrFile.transformConst(
    interpreter: IrInterpreter,
    mode: EvaluationMode,
    evaluatedConstTracker: EvaluatedConstTracker? = null,
    inlineConstTracker: InlineConstTracker? = null,
    onWarning: (IrFile, IrElement, IrErrorExpression) -> Unit = { _, _, _ -> },
    onError: (IrFile, IrElement, IrErrorExpression) -> Unit = { _, _, _ -> },
    suppressExceptions: Boolean = false,
) {
    val preprocessedFile = preprocessors.fold(this) { file, preprocessor ->
        preprocessor.preprocess(file, IrInterpreterPreprocessorData(mode, interpreter.irBuiltIns))
    }

    val checkers = setOf(
        IrInterpreterNameChecker(),
        IrInterpreterCommonChecker(),
    )

    synchronized(lock) {
        checkers.forEach { checker ->
            val data = IrConstTransformerData(
                preprocessedFile, interpreter, checker, mode,
                evaluatedConstTracker, inlineConstTracker,
                onWarning, onError, suppressExceptions
            )
            transformers.forEach { it.transform(data) }
        }
    }
}

data class IrConstTransformerData(
    val irFile: IrFile,
    val interpreter: IrInterpreter,
    val checker: IrInterpreterChecker,
    val mode: EvaluationMode,
    val evaluatedConstTracker: EvaluatedConstTracker?,
    val inlineConstTracker: InlineConstTracker?,
    val onWarning: (IrFile, IrElement, IrErrorExpression) -> Unit,
    val onError: (IrFile, IrElement, IrErrorExpression) -> Unit,
    val suppressExceptions: Boolean,
)

// Note: We are using `IrElementTransformer` here instead of `IrElementTransformerVoid` to avoid conflicts with `IrTypeVisitorVoid`
// that is used later in `IrConstTypeAnnotationTransformer`.
internal abstract class IrConstTransformer : IrElementTransformer<Nothing?> {
    protected lateinit var irFile: IrFile
        private set

    protected lateinit var interpreter: IrInterpreter
        private set

    private lateinit var data: IrConstTransformerData

    fun transform(data: IrConstTransformerData) {
        this.data = data
        this.irFile = data.irFile
        this.interpreter = data.interpreter
        irFile.accept(this, null)
    }

    private fun IrExpression.warningIfError(original: IrExpression): IrExpression {
        if (this is IrErrorExpression) {
            data.onWarning(irFile, original, this)
            return original
        }
        return this
    }

    private fun IrExpression.reportIfError(original: IrExpression): IrExpression {
        if (this is IrErrorExpression) {
            data.onError(irFile, original, this)
            return when (data.mode) {
                // need to pass any const value to be able to get some bytecode and then report error
                EvaluationMode.ONLY_INTRINSIC_CONST -> IrConstImpl.constNull(startOffset, endOffset, type)
                else -> original
            }
        }
        return this
    }

    protected fun IrExpression.canBeInterpreted(
        configuration: IrInterpreterConfiguration = interpreter.environment.configuration
    ): Boolean {
        return try {
            this.accept(data.checker, IrInterpreterCheckerData(data.mode, interpreter.irBuiltIns, configuration))
        } catch (e: Throwable) {
            if (data.suppressExceptions) {
                return false
            }
            throw AssertionError("Error occurred while optimizing an expression:\n${this.dump()}", e)
        }
    }

    protected fun IrExpression.interpret(failAsError: Boolean): IrExpression {
        val result = try {
            interpreter.interpret(this, irFile)
        } catch (e: Throwable) {
            if (data.suppressExceptions) {
                return this
            }
            throw AssertionError("Error occurred while optimizing an expression:\n${this.dump()}", e)
        }

        data.evaluatedConstTracker?.save(
            result.startOffset, result.endOffset, irFile.nameWithPackage,
            constant = if (result is IrErrorExpression) ErrorValue.create(result.description)
            else (result as IrConst<*>).toConstantValue()
        )

        if (result is IrConst<*>) {
            val field = when (this) {
                is IrGetField -> this.symbol.owner
                is IrCall -> this.symbol.owner.property?.backingField
                else -> null
            }

            if (field != null) data.inlineConstTracker.reportOnIr(irFile, field, result)
        }

        return if (failAsError) result.reportIfError(this) else result.warningIfError(this)
    }
}

fun InlineConstTracker?.reportOnIr(irFile: IrFile, field: IrField, value: IrConst<*>) {
    if (this == null) return
    if (field.origin != IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB) return

    val path = irFile.path
    val owner = field.parentAsClass.classId?.asString()?.replace(".", "$")?.replace("/", ".") ?: return
    val name = field.name.asString()
    val constType = value.kind.asString

    report(path, owner, name, constType)
}
