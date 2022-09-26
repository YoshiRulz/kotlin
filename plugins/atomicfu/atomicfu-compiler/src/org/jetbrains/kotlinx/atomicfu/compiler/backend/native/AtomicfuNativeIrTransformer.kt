/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.backend.native

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlinx.atomicfu.compiler.backend.*
import org.jetbrains.kotlinx.atomicfu.compiler.backend.updateSetter
import org.jetbrains.kotlinx.atomicfu.compiler.backend.common.AbstractAtomicfuTransformer

class AtomicfuNativeIrTransformer(
    pluginContext: IrPluginContext,
    override val atomicSymbols: NativeAtomicSymbols
) : AbstractAtomicfuTransformer(pluginContext) {

    override fun transformAtomicProperties(moduleFragment: IrModuleFragment) {
        for (irFile in moduleFragment.files) {
            irFile.transform(NativeAtomicPropertiesTransformer(), null)
        }
    }

    override fun transformAtomicExtensions(moduleFragment: IrModuleFragment) {
        for (irFile in moduleFragment.files) {
            irFile.transform(AtomicExtensionTransformer(), null)
        }
    }

    override fun transformAtomicFunctions(moduleFragment: IrModuleFragment) {
        for (irFile in moduleFragment.files) {
            irFile.transform(NativeAtomicFunctionCallTransformer(), null)
            println(irFile.dump())
        }
    }

    private inner class NativeAtomicPropertiesTransformer : AtomicPropertiesTransformer() {

        override fun IrProperty.transformInClassAtomic(parentContainer: IrClass) =
            toVolatileProperty(parentContainer)

        override fun IrProperty.transformStaticAtomic(parentContainer: IrDeclarationContainer) {
            TODO("Not yet implemented")
        }

        override fun IrProperty.transformDelegatedAtomic(parentContainer: IrDeclarationContainer) {
            // todo but may be abstracted out
            TODO("Not yet implemented")
        }

        override fun IrProperty.transformAtomicArray(parentContainer: IrDeclarationContainer) {
            TODO("Not yet implemented")
        }

        private fun IrProperty.toVolatileProperty(parentClass: IrClass) {
            // Atomic box is replaced with a volatile property
            // For now supported for primitive types (int, long, boolean) and for linuxX64 and macosX64
            // val a = atomic(0)
            // @Volatile var a: Int = 0
            backingField = buildVolatileField(this, parentClass)
            updateGetter(parentClass, irBuiltIns)
            updateSetter(parentClass, irBuiltIns)
        }
    }

    private inner class AtomicExtensionTransformer : IrElementTransformerVoid()

    private inner class NativeAtomicFunctionCallTransformer : AtomicFunctionCallTransformer() {

        override fun transformedCallOnAtomic(
            expression: IrCall,
            functionName: String,
            valueType: IrType,
            castType: IrType?,
            receiver: IrExpression,
            parentFunction: IrFunction?
        ): IrExpression {
            TODO("Not yet implemented")
        }

        override fun transformedCallOnAtomicArrayElement(
            expression: IrCall,
            functionName: String,
            valueType: IrType,
            receiver: IrExpression,
            parentFunction: IrFunction?
        ): IrExpression {
            TODO("Not yet implemented")
        }

        override fun transformedAtomicfuInlineFunctionCall(
            expression: IrCall,
            functionName: String,
            valueType: IrType,
            receiver: IrExpression,
            isArrayReceiver: Boolean,
            parentFunction: IrFunction?
        ): IrCall {
            TODO("Not yet implemented")
        }

        override fun transformedAtomicExtensionCall(
            expression: IrCall,
            transformedAtomicExtension: IrSimpleFunction,
            receiver: IrExpression,
            isArrayReceiver: Boolean,
            parentFunction: IrFunction?
        ): IrCall {
            TODO("Not yet implemented")
        }

        override fun IrDeclarationContainer.getTransformedAtomicExtension(
            declaration: IrSimpleFunction,
            isArrayReceiver: Boolean
        ): IrSimpleFunction {
            TODO("Not yet implemented")
        }


        override fun IrExpression.isArrayElementReceiver(parentFunction: IrFunction?): Boolean {
            return false
        }
    }
}
