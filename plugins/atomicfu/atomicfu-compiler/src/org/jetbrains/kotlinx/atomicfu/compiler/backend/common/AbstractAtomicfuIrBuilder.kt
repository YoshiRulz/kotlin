/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.backend.common

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addGetter
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrExpressionBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.name.Name

abstract class AbstractAtomicfuIrBuilder(
    private val irBuiltIns: IrBuiltIns,
    symbol: IrSymbol,
    startOffset: Int,
    endOffset: Int
) : IrBuilderWithScope(IrGeneratorContextBase(irBuiltIns), Scope(symbol), startOffset, endOffset) {

    abstract val atomicSymbols: AbstractAtomicSymbols

    fun irCallWithArgs(symbol: IrSimpleFunctionSymbol, dispatchReceiver: IrExpression?, valueArguments: List<IrExpression?>) =
        irCall(symbol).apply {
            this.dispatchReceiver = dispatchReceiver
            valueArguments.forEachIndexed { i, arg ->
                putValueArgument(i, arg)
            }
        }

    fun irVolatileField(
        name: Name,
        type: IrType,
        initValue: IrExpression,
        annotations: List<IrConstructorCall>,
        parentContainer: IrDeclarationContainer
    ): IrField =
        context.irFactory.buildField {
            this.name = name
            this.type = type
            isFinal = false
            isStatic = parentContainer is IrFile
            visibility = DescriptorVisibilities.PRIVATE
        }.apply {
            initializer = IrExpressionBodyImpl(initValue)
            this.annotations = annotations + atomicSymbols.volatileAnnotationConstructorCall
            this.parent = parentContainer
        }

    fun IrExpression.toBoolean() = irNotEquals(this, irInt(0)) as IrCall
}
