/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.backend.native

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addExtensionReceiver
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.impl.IrPropertyReferenceImpl
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.Name
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
            irFile.transform(NativeAtomicExtensionTransformer(), null)
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
            // todo: just skip them (as a box) and do not transform any subsequent calls on this atomic
            TODO("Not yet implemented")
        }

        override fun IrProperty.transformDelegatedAtomic(parentContainer: IrDeclarationContainer) {
            // todo reuse JVM transformation
            TODO("Not yet implemented")
        }

        override fun IrProperty.transformAtomicArray(parentContainer: IrDeclarationContainer) {
            // todo: just skip them (as a box) and do not transform any subsequent calls on this array
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

    private inner class NativeAtomicExtensionTransformer : AtomicExtensionTransformer() {

        override fun IrDeclarationContainer.transformAllAtomicExtensions() {
            declarations.filter { it is IrFunction && it.isAtomicExtension() }.forEach { atomicExtension ->
                atomicExtension as IrFunction
                declarations.add(transformAtomicExtension(atomicExtension, this))
                declarations.remove(atomicExtension)
            }
        }

        private fun transformAtomicExtension(
            atomicExtension: IrFunction,
            parent: IrDeclarationContainer
        ): IrFunction {
            // This function changes type of atomic extension receiver to KProperty<T>.
            // The body of atomic extension function will be transformed later in `NativeAtomicFunctionCallTransformer`.
            // inline fun AtomicInt.foo() {
            //    compareAndSet(0, 56)
            //}
            // ---->
            //inline fun KProperty<Int>.foo() {
            //    this.compareAndSetField(0, 56) //  will be replaced with the intrinsic call in `NativeAtomicFunctionCallTransformer`.
            //}
            val mangledName = mangleAtomicExtensionName(atomicExtension.name.asString(), false)
//            val extensionReceiver = requireNotNull(atomicExtension.extensionReceiverParameter) { "Extension receiver of atomic extension function $atomicExtension should not be null" }
//            val valueType = extensionReceiver.type.atomicToValueType()
            return pluginContext.irFactory.buildFun {
                name = Name.identifier(mangledName)
                isInline = true
                visibility = atomicExtension.visibility
            }.apply {
                val newDeclaration = this
                addExtensionReceiver(buildSimpleType(irBuiltIns.kMutableProperty0Class, listOf(irBuiltIns.intType)))
                dispatchReceiverParameter = atomicExtension.dispatchReceiverParameter?.deepCopyWithSymbols(this)
                atomicExtension.valueParameters.forEach {
                    addValueParameter(it.name, it.type).also {
                        it.parent = newDeclaration
                    }
                }
                // the body will be transformed later by `AtomicFUTransformer`
                body = atomicExtension.body?.deepCopyWithSymbols(this)
                // todo abstract out this stuff
                body?.transform(
                    object : IrElementTransformerVoid() {

                        override fun visitGetValue(expression: IrGetValue): IrExpression {
                            // remap value parameters to the new
                            if (expression.symbol is IrValueParameterSymbol) {
                                val oldParam = expression.symbol.owner as IrValueParameter
                                // todo any trouble with index == -1
                                if (oldParam.index >= 0) {
                                    val newParam = newDeclaration.valueParameters[oldParam.index]
                                    return buildGetValue(
                                        expression.startOffset,
                                        expression.endOffset,
                                        newParam.symbol
                                    )
                                }
                            }
                            return super.visitGetValue(expression)
                        }

                        override fun visitReturn(expression: IrReturn): IrExpression = super.visitReturn(
                            if (expression.returnTargetSymbol == atomicExtension.symbol) {
                                with(atomicSymbols.createBuilder(newDeclaration.symbol)) {
                                    irReturn(expression.value)
                                }
                            } else {
                                expression
                            }
                        )
                    }, null
                )
                returnType = atomicExtension.returnType
                this.parent = parent
            }
        }
    }

    private inner class NativeAtomicFunctionCallTransformer : AtomicFunctionCallTransformer() {

        override fun transformedCallOnAtomic(
            expression: IrCall,
            functionName: String,
            valueType: IrType,
            castType: IrType?,
            getPropertyReceiver: IrExpression,
            parentFunction: IrFunction?
        ): IrExpression {
            with(atomicSymbols.createBuilder(expression.symbol)) {
                if (getPropertyReceiver is IrCall) {
                    val classReceiver = getPropertyReceiver.dispatchReceiver
                    val property = getPropertyReceiver.getCorrespondingProperty()
                    val propertyRef = buildPropertyReference(property, classReceiver)
                    return when (functionName) {
                        //"<get-value>" -> callGetter(property.getter!!.symbol, classReceiver, valueType)
                        "<get-value>" -> callGetter(atomicSymbols.kMutableProperty0Get, propertyRef, valueType)
                        "<set-value>", "lazySet" -> callSetter(property.setter!!.symbol, classReceiver, expression.getValueArgument(0))
                        else -> {
                            irCallAtomicNativeIntrinsic(
                                functionName = functionName,
                                propertyRef = propertyRef,
                                valueType = getPropertyReceiver.type.atomicToValueType(),
                                valueArguments = expression.getValueArguments()
                            )
                        }
                    }
                }
                if (getPropertyReceiver.isThisReceiver()) {
                    val propertyExtensionRecevier = requireNotNull(parentFunction?.extensionReceiverParameter) { "Extension receiver of function $parentFunction should be null" }
                    val propertyRef = propertyExtensionRecevier.capture()
                    return when (functionName) {
                        "<get-value>" -> callGetter(atomicSymbols.kMutableProperty0Get, propertyRef, valueType)
                        "<set-value>", "lazySet" -> callSetter(atomicSymbols.kMutableProperty0Set, propertyRef, expression.getValueArgument(0))
                        else -> {
                            irCallAtomicNativeIntrinsic(
                                functionName = functionName,
                                propertyRef = propertyRef,
                                valueType = getPropertyReceiver.type.atomicToValueType(),
                                valueArguments = expression.getValueArguments()
                            )
                        }
                    }
                }
            }
            return expression // in all other cases leave the function call untransformed
        }

        override fun IrFunction.isTransformedAtomicExtension(): Boolean =
            extensionReceiverParameter != null && extensionReceiverParameter!!.type.classOrNull == irBuiltIns.kMutableProperty0Class

        override fun transformedCallOnAtomicArrayElement(
            expression: IrCall,
            functionName: String,
            valueType: IrType,
            getPropertyReceiver: IrExpression,
            parentFunction: IrFunction?
        ): IrExpression {
            TODO("Not supported, leave the function untransformed")
        }

        override fun transformedAtomicfuInlineFunctionCall(
            expression: IrCall,
            functionName: String,
            valueType: IrType,
            getPropertyReceiver: IrExpression,
            isArrayReceiver: Boolean,
            parentFunction: IrFunction?
        ): IrCall {
            // todo should probably be delegated to transformedAtomicExtensionCall
            TODO("Not yet implemented, leave the function untransformed")
        }

        override fun transformedAtomicExtensionCall(
            expression: IrCall,
            transformedAtomicExtension: IrSimpleFunction,
            getPropertyReceiver: IrExpression,
            isArrayReceiver: Boolean,
            parentFunction: IrFunction?
        ): IrCall {
            // volatile a:Int = 77
            //
            // fun KProperty<Int>.foo$atomicfu(update: Int) {
            //    this.compareAndSetField(value, update)
            // }
            //
            // a.foo(45) --> this::a.foo(45)
            with(atomicSymbols.createBuilder(expression.symbol)) {
                if (getPropertyReceiver is IrCall) {
                    val classReceiver = getPropertyReceiver.dispatchReceiver
                    val property = getPropertyReceiver.getCorrespondingProperty()
                    val propertyRef = buildPropertyReference(property, classReceiver)
                    return irCallWithArgs(
                        symbol = transformedAtomicExtension.symbol,
                        dispatchReceiver = expression.dispatchReceiver, // todo: check?
                        extensionReceiver = propertyRef,
                        valueArguments = expression.getValueArguments()
                    )
                }
                if (getPropertyReceiver.isThisReceiver()) {
                    TODO("This is atomic extension call inside another atomic extension")
                }
            }
            return expression
        }

        private fun buildPropertyReference(property: IrProperty, classReceiver: IrExpression?): IrPropertyReferenceImpl {
            val backingField = requireNotNull(property.backingField) { "Backing field of the property $property should not be null" }
            return IrPropertyReferenceImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                type = buildSimpleType(irBuiltIns.kMutableProperty0Class, listOf(backingField.type)),
                symbol = property.symbol,
                typeArgumentsCount = 0, // todo what about KProperty<Ref>?
                field = backingField.symbol,
                getter = property.getter?.symbol,
                setter = property.setter?.symbol
            ).apply {
                dispatchReceiver = classReceiver
            }
        }

        override fun IrExpression.isArrayElementReceiver(parentFunction: IrFunction?): Boolean {
            return false
        }
    }
}
