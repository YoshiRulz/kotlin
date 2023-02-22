/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.backend.native

import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irLong
import org.jetbrains.kotlin.ir.declarations.IrProperty
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

class NativeAtomicfuIrBuilder(
    override val atomicSymbols: NativeAtomicSymbols,
    symbol: IrSymbol,
    startOffset: Int,
    endOffset: Int
): AbstractAtomicfuIrBuilder(atomicSymbols.irBuiltIns, symbol, startOffset, endOffset) {

    internal fun callGetter(getterSymbol: IrSimpleFunctionSymbol, receiver: IrExpression?, valueType: IrType): IrCall =
        irCall(getterSymbol).apply {
            dispatchReceiver = receiver
        }.let {
            if (valueType.isBoolean() && it.type.isInt()) it.toBoolean() else it
        }

    internal fun callSetter(setterSymbol: IrSimpleFunctionSymbol, receiver: IrExpression?, value: IrExpression?): IrCall =
        irCall(setterSymbol).apply {
            dispatchReceiver = receiver
            putValueArgument(0, value)
        }

    internal fun IrProperty.callGetter(classReceiver: IrExpression?, valueType: IrType): IrCall {
        val getter = requireNotNull(this.getter) { "Property getter should be defined ${this.render()}" }
        return irCall(getter.symbol).apply {
            dispatchReceiver = classReceiver
        }.let {
            if (valueType.isBoolean() && it.type.isInt()) it.toBoolean() else it
        }
    }

    internal fun IrProperty.callSetter(classReceiver: IrExpression?, value: IrExpression?): IrCall {
        val setter = requireNotNull(this.setter) { "Property setter should be defined ${this.render()}" }
        return irCall(setter.symbol).apply {
            dispatchReceiver = classReceiver
            putValueArgument(0, value)
        }
    }

    internal fun callKProperty0Get(propertyRef: IrExpression, valueType: IrType): IrCall =
        irCall(atomicSymbols.kProperty0Get).apply {
            dispatchReceiver = propertyRef
        }.let {
            if (valueType.isBoolean() && it.type.isInt()) it.toBoolean() else it
        }

    internal fun callKMutableProperty0Set(propertyRef: IrExpression, value: IrExpression?): IrCall =
        irCall(atomicSymbols.kMutableProperty0Set).apply {
            dispatchReceiver = propertyRef
            putValueArgument(0, value)
        }

    internal fun irCallAtomicNativeIntrinsic(
        functionName: String,
        propertyRef: IrExpression,
        valueType: IrType,
        valueArguments: List<IrExpression?>
    ): IrCall = when (functionName) {
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
