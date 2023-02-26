/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.backend.native

import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.psi2ir.generators.implicitCastTo
import org.jetbrains.kotlinx.atomicfu.compiler.backend.common.AbstractAtomicfuIrBuilder
import org.jetbrains.kotlinx.atomicfu.compiler.backend.getValueArguments

class NativeAtomicfuIrBuilder(
    override val atomicSymbols: NativeAtomicSymbols,
    symbol: IrSymbol,
    startOffset: Int,
    endOffset: Int
): AbstractAtomicfuIrBuilder(atomicSymbols.irBuiltIns, symbol, startOffset, endOffset) {

    internal fun irCallAtomicNativeIntrinsic(
        functionName: String,
        propertyRef: IrExpression,
        valueType: IrType,
        valueArguments: List<IrExpression?>
    ): IrCall = when (functionName) {
        "<get-value>" -> callGetter(atomicSymbols.kMutableProperty0Get, propertyRef, valueType)
        "<set-value>", "lazySet" -> callSetter(atomicSymbols.kMutableProperty0Set, propertyRef, valueArguments[0])
        "compareAndSet" -> compareAndSetField(propertyRef, valueType, valueArguments[0], valueArguments[1])
        "getAndSet" -> getAndSetField(propertyRef, valueType, valueArguments[0])
        "getAndAdd" -> getAndAddField(propertyRef, valueType, valueArguments[0])
        "getAndIncrement" -> getAndIncrementField(propertyRef, valueType)
        "getAndDecrement" -> getAndDecrementField(propertyRef, valueType)
        "addAndGet" -> addAndGetField(propertyRef, valueType, valueArguments[0])
        "incrementAndGet" -> incrementAndGetField(propertyRef, valueType)
        "decrementAndGet" -> decrementAndGetField(propertyRef, valueType)
        else -> error("Unsupported atomic function name $functionName")
    }.let {
        if (valueType.isBoolean() && it.type.isInt()) it.toBoolean() else it
    }

    private fun callGetter(getterSymbol: IrSimpleFunctionSymbol, receiver: IrExpression?, valueType: IrType): IrCall =
        irCall(getterSymbol).apply {
            dispatchReceiver = receiver
        }.let {
            if (valueType.isBoolean() && it.type.isInt()) it.toBoolean() else it
        }

    private fun callSetter(setterSymbol: IrSimpleFunctionSymbol, receiver: IrExpression?, value: IrExpression?): IrCall =
        irCall(setterSymbol).apply {
            dispatchReceiver = receiver
            putValueArgument(0, value)
        }

    /*
    inline fun <T> KMutableProperty0<Int>.loop$atomicfu(action: (Int) -> Unit) {
        while (true) {
            val cur = this.get()
            action(cur)
        }
    }
    */
    fun atomicfuLoopBody(valueType: IrType, thisPropertyRef: IrExpression, action: IrValueParameter) =
        irBlockBody {
            +irWhile().apply {
                condition = irTrue()
                body = irBlock {
                    val cur = createTmpVariable(
                        callGetter(atomicSymbols.kMutableProperty0Get, thisPropertyRef, valueType),
                        "atomicfu\$cur", false
                    )
                    +irCall(atomicSymbols.invoke1Symbol).apply {
                        dispatchReceiver = irGet(action)
                        putValueArgument(0, irGet(cur))
                    }
                }
            }
        }

    /*
    inline fun KMutableProperty0<Int>.update$atomicfu(action: (Int) -> Int) {
        while (true) {
            val cur = this.get()
            val upd = action(cur)
            if (this.compareAndSetField(cur, upd)) return
        }
    }


    inline fun KMutableProperty0<Int>.getAndUpdate$atomicfu(action: (Int) -> Int) {
        while (true) {
            val cur = this.get()
            val upd = action(cur)
            if (this.compareAndSetField(cur, upd)) return cur
        }
    }

    inline fun KMutableProperty0<Int>.getAndUpdate$atomicfu(action: (Int) -> Int) {
        while (true) {
            val cur = this.get()
            val upd = action(cur)
            if (this.compareAndSetField(cur, upd)) return upd
        }
    }
    */
    fun atomicfuUpdateBody(
        functionName: String,
        valueType: IrType,
        thisPropertyRef: IrExpression,
        action: IrValueParameter
    ) =
        irBlockBody {
            +irWhile().apply {
                condition = irTrue()
                body = irBlock {
                    val cur = createTmpVariable(
                        callGetter(atomicSymbols.kMutableProperty0Get, thisPropertyRef, valueType),
                        "atomicfu\$cur", false
                    )
                    val upd = createTmpVariable(
                        irCall(atomicSymbols.invoke1Symbol).apply {
                            dispatchReceiver = irGet(action)
                            putValueArgument(0, irGet(cur))
                        }, "atomicfu\$upd", false
                    )
                    +irIfThen(
                        type = atomicSymbols.irBuiltIns.unitType,
                        condition = irCallAtomicNativeIntrinsic(
                            functionName = "compareAndSet",
                            propertyRef = thisPropertyRef,
                            valueType = valueType,
                            valueArguments = listOf(irGet(cur), irGet(upd))
                        ),
                        thenPart = when (functionName) {
                            "update" -> irReturnUnit()
                            "getAndUpdate" -> irReturn(irGet(cur))
                            "updateAndGet" -> irReturn(irGet(upd))
                            else -> error("Unsupported atomicfu inline loop function name: $functionName")
                        }
                    )
                }
            }
        }

    private fun compareAndSetField(propertyRef: IrExpression, valueType: IrType, expected: IrExpression?, updated: IrExpression?) =
        callNativeAtomicIntrinsic(propertyRef, atomicSymbols.compareAndSetFieldIntrinsic, valueType, expected, updated)

    private fun getAndSetField(propertyRef: IrExpression, valueType: IrType, value: IrExpression?) =
        callNativeAtomicIntrinsic(propertyRef, atomicSymbols.getAndSetFieldIntrinsic, valueType, value)

    private fun getAndAddField(propertyRef: IrExpression, valueType: IrType, delta: IrExpression?): IrCall =
        when {
            valueType.isInt() ->
                callNativeAtomicIntrinsic(propertyRef, atomicSymbols.getAndAddIntFieldIntrinsic, null, delta)
            valueType.isLong() ->
                callNativeAtomicIntrinsic(
                    propertyRef,
                    atomicSymbols.getAndAddLongFieldIntrinsic,
                    null,
                    delta?.implicitCastTo(context.irBuiltIns.longType)
                )
            else -> error("kotlin.native.internal/getAndAddField intrinsic is not supported for values of type ${valueType.dumpKotlinLike()}")
        }

    private fun addAndGetField(propertyRef: IrExpression, valueType: IrType, delta: IrExpression?): IrCall =
        getAndAddField(propertyRef, valueType, delta).plus(delta)

    private fun getAndIncrementField(propertyRef: IrExpression, valueType: IrType): IrCall {
        val delta = if (valueType.isInt()) irInt(1) else irLong(1)
        return getAndAddField(propertyRef, valueType, delta)
    }

    private fun getAndDecrementField(propertyRef: IrExpression, valueType: IrType): IrCall {
        val delta = if (valueType.isInt()) irInt(-1) else irLong(-1)
        return getAndAddField(propertyRef, valueType, delta)
    }

    private fun incrementAndGetField(propertyRef: IrExpression, valueType: IrType): IrCall {
        val delta = if (valueType.isInt()) irInt(1) else irLong(1)
        return addAndGetField(propertyRef, valueType, delta)
    }

    private fun decrementAndGetField(propertyRef: IrExpression, valueType: IrType): IrCall {
        val delta = if (valueType.isInt()) irInt(-1) else irLong(-1)
        return addAndGetField(propertyRef, valueType, delta)
    }

    private fun callNativeAtomicIntrinsic(
        propertyRef: IrExpression,
        symbol: IrSimpleFunctionSymbol,
        typeArgument: IrType?,
        vararg valueArguments: IrExpression?
    ): IrCall =
        irCall(symbol).apply {
            extensionReceiver = propertyRef
            typeArgument?.let { putTypeArgument(0, it) }
            valueArguments.forEachIndexed { index, arg ->
                putValueArgument(index, arg)
            }
        }

    private fun IrCall.plus(other: IrExpression?): IrCall {
        val returnType = this.symbol.owner.returnType
        val plusOperatorSymbol = when {
            returnType.isInt() -> atomicSymbols.intPlusOperator
            returnType.isLong() -> atomicSymbols.longPlusOperator
            else -> error("Return type of the function ${this.symbol.owner.dump()} is expected to be Int or Long, but found $returnType")
        }
        return irCall(plusOperatorSymbol).apply {
            dispatchReceiver = this@plus
            putValueArgument(0, other)
        }
    }
}
