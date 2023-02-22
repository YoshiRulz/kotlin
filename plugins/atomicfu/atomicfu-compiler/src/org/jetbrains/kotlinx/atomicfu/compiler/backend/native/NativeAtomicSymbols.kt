/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.backend.native

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addExtensionReceiver
import org.jetbrains.kotlin.backend.jvm.functionByName
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addTypeParameter
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlinx.atomicfu.compiler.backend.common.AbstractAtomicSymbols

class NativeAtomicSymbols(
    context: IrPluginContext,
    moduleFragment: IrModuleFragment
) : AbstractAtomicSymbols(context, moduleFragment) {
    val kotlinNativeInternal: IrPackageFragment = createPackage("kotlin.native.internal")

    private val volatileConstructor = buildAnnotationConstructor(
        buildClass(JvmNames.VOLATILE_ANNOTATION_FQ_NAME, ClassKind.ANNOTATION_CLASS, kotlinNativeInternal)
    )

    override val volatileAnnotationClass: IrClass
        get() = buildClass(JvmNames.VOLATILE_ANNOTATION_FQ_NAME, ClassKind.ANNOTATION_CLASS, kotlinNativeInternal)

    // AtomicInt class

    val compareAndSetFieldIntrinsic =
        context.referenceFunctions(CallableId(FqName("kotlin.native.concurrent"), Name.identifier("compareAndSetField"))).single()

    val getAndSetFieldIntrinsic =
        context.referenceFunctions(CallableId(FqName("kotlin.native.concurrent"), Name.identifier("getAndSetField"))).single()

    val getAndAddIntFieldIntrinsic =
        context.referenceFunctions(CallableId(FqName("kotlin.native.concurrent"), Name.identifier("getAndAddField")))
            .single { it.owner.returnType.isInt() }

    val getAndAddLongFieldIntrinsic =
        context.referenceFunctions(CallableId(FqName("kotlin.native.concurrent"), Name.identifier("getAndAddField")))
            .single { it.owner.returnType.isLong() }

    val kProperty0Get = irBuiltIns.kProperty0Class.functionByName("get")

    val kMutableProperty0Set = irBuiltIns.kMutableProperty0Class.functionByName("set")

    val intPlusOperator = context.referenceFunctions(CallableId(StandardClassIds.Int, Name.identifier("plus")))
        .single { it.owner.valueParameters[0].type.isInt() }

    val longPlusOperator = context.referenceFunctions(CallableId(StandardClassIds.Long, Name.identifier("plus")))
        .single { it.owner.valueParameters[0].type.isLong() }

    override fun createBuilder(symbol: IrSymbol, startOffset: Int, endOffset: Int) =
        NativeAtomicfuIrBuilder(this, symbol, startOffset, endOffset)
}
