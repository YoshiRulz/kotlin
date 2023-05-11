/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.backend.native

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addExtensionReceiver
import org.jetbrains.kotlin.backend.common.serialization.proto.IrConst
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrExpressionBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrPropertyReferenceImpl
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlinx.atomicfu.compiler.backend.*
import org.jetbrains.kotlinx.atomicfu.compiler.backend.updateSetter
import org.jetbrains.kotlinx.atomicfu.compiler.backend.common.AbstractAtomicfuTransformer


private const val ATOMICFU = "atomicfu"
private const val LOOP = "loop"
private const val UPDATE = "update"
private const val ACTION = "$ATOMICFU\$action"

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
        }
    }

    private inner class NativeAtomicPropertiesTransformer : AtomicPropertiesTransformer() {

        override fun IrProperty.transformInClassAtomic(parentContainer: IrClass) =
            toVolatileProperty(parentContainer)

        override fun IrProperty.transformStaticAtomic(parentContainer: IrDeclarationContainer) {
            toVolatileProperty(parentContainer)
//            parentContainer.declarations.remove(this)
//            generateVolatileProperty(this, parentContainer)
            //parentContainer.declarations.add(volatileProperty)
        }

        override fun IrProperty.transformDelegatedAtomic(parentContainer: IrDeclarationContainer) {
            // delegated atomics are left as boxes for now
        }

        override fun IrProperty.transformAtomicArray(parentContainer: IrDeclarationContainer) {
            // todo: just skip them (as a box) and do not transform any subsequent calls on this array
            // todo: design API for atomic array intrinsics
        }

        private fun generateVolatileProperty(atomicProperty: IrProperty, parentContainer: IrDeclarationContainer) {
            val atomicField =
                requireNotNull(atomicProperty.backingField) { "BackingField of atomic property $atomicProperty should not be null" }
            val fieldType = atomicField.type.atomicToValueType()
            atomicField.initializer?.expression?.let {
                val initValue = (it as IrCall).getAtomicFactoryValueArgument()
                with(atomicSymbols.createBuilder(atomicProperty.symbol)) {
                    irVolatileField(
                        Name.identifier(atomicProperty.name.asString() + "\$volatile"),
                        fieldType,
                        initValue,
                        atomicField.annotations,
                        parentContainer
                    ).apply {
                        parent = parentContainer
                    }
                }
//                irBuiltIns.irFactory.buildProperty {
//                    name = volatileField.name
//                    visibility = atomicProperty.visibility
//                    isVar = true
//                }.apply {
//                    backingField = volatileField
//                    //addStaticGetter(irBuiltIns)
//                    parent = parentContainer
//                    parentContainer.declarations.add(this)
//                }
            }
        }

        private fun IrProperty.toVolatileProperty(parentContainer: IrDeclarationContainer) {
            // Atomic box is replaced with a volatile property
            // val a = atomic(0) ->
            // @Volatile var a: Int = 0
            setVolatileBackingField(parentContainer)
            updateGetter(parentContainer, irBuiltIns)
            updateSetter(parentContainer, irBuiltIns)
        }
    }

    private inner class NativeAtomicExtensionTransformer : AtomicExtensionTransformer() {

        override fun IrDeclarationContainer.transformAllAtomicExtensions() {
            declarations.filter { it is IrSimpleFunction && it.isAtomicExtension()  }.forEach {
                declarations.add((it as IrSimpleFunction).deepCopyWithSymbols(this).transformAtomicExtension())
                declarations.remove(it)
            }
        }

        private fun IrSimpleFunction.transformAtomicExtension(): IrFunction {
            val mangledName = mangleAtomicExtensionName(this.name.asString(), false)
            val valueType = extensionReceiverParameter!!.type.atomicToValueType()
            this.name = Name.identifier(mangledName)
            addExtensionReceiver(buildSimpleType(irBuiltIns.kMutableProperty0Class, listOf(if (valueType.isBoolean()) irBuiltIns.intType else valueType)))
            return this
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
        ): IrExpression =
            with(atomicSymbols.createBuilder(expression.symbol)) {
                /**
                 * Skipping untransformed atomic receivers
                 */
                val pf = (if (parentFunction?.origin == IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA) parentFunction.parent else parentFunction) as? IrSimpleFunction
                if (getPropertyReceiver is IrCall && getPropertyReceiver.getCorrespondingProperty().backingField!!.type.isAtomicValueType() || // todo or is array
                    (pf != null && pf.isAtomicExtension() && !pf.isTransformedAtomicExtension())) {
                    return expression
                }

                requireNotNull(parentFunction) { "Parent function of the call ${expression.render()} is null" }
                // skip untransformed atomic fields and array elements
                return getPropertyRefReceiver(getPropertyReceiver, parentFunction)?.let { propertyRef ->
                    irCallAtomicNativeIntrinsic(
                        functionName = functionName,
                        propertyRef = propertyRef,
                        valueType = valueType,
                        valueArguments = expression.getValueArguments()
                    )
                } ?: expression
            }

        override fun transformedAtomicExtensionCall(
            expression: IrCall,
            transformedAtomicExtension: IrSimpleFunction,
            getPropertyReceiver: IrExpression,
            isArrayReceiver: Boolean,
            parentFunction: IrFunction?
        ): IrCall =
            with(atomicSymbols.createBuilder(expression.symbol)) {
                // volatile a:Int = 77
                //
                // fun KProperty<Int>.foo$atomicfu(update: Int) {
                //    this.compareAndSetField(value, update)
                // }
                //
                // a.foo(45) --> this::a.foo(45)

                /**
                 * Skipping untransformed atomic receivers
                 */
                val pf = (if (parentFunction?.origin == IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA) parentFunction.parent else parentFunction) as? IrSimpleFunction
                if (isArrayReceiver || (getPropertyReceiver is IrCall && getPropertyReceiver.getCorrespondingProperty().backingField!!.type.isAtomicValueType()) ||
                    (pf != null && pf.isAtomicExtension() && !pf.isTransformedAtomicExtension())) {
                    return expression
                }
                requireNotNull(parentFunction) { "Parent function of the call ${expression.render()} is null" }
                val irCall = getPropertyRefReceiver(getPropertyReceiver, parentFunction)?.let { propertyRef ->
                    irCallWithArgs(
                        symbol = transformedAtomicExtension.symbol,
                        dispatchReceiver = expression.dispatchReceiver,
                        extensionReceiver = propertyRef,
                        valueArguments = expression.getValueArguments()
                    )
                } ?: expression
                return irCall
            }

        override fun transformedAtomicfuInlineFunctionCall(
            expression: IrCall,
            functionName: String,
            valueType: IrType,
            getPropertyReceiver: IrExpression,
            isArrayReceiver: Boolean,
            parentFunction: IrFunction?
        ): IrCall {
            with(atomicSymbols.createBuilder(expression.symbol)) {
                /**
                 * Skipping untransformed atomic receivers
                 */
                val pf = (if (parentFunction?.origin == IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA) parentFunction.parent else parentFunction) as? IrSimpleFunction
                if (isArrayReceiver || (getPropertyReceiver is IrCall && getPropertyReceiver.getCorrespondingProperty().backingField!!.type.isAtomicValueType()) ||
                    (pf != null && pf.isAtomicExtension() && !pf.isTransformedAtomicExtension())) {
                    return expression
                }
                requireNotNull(parentFunction) { "Parent function of the call ${expression.render()} is null" }
                val loopFunc = parentFunction.parentDeclarationContainer.getOrBuildInlineLoopFunction(
                    functionName = functionName,
                    valueType = if (valueType.isBoolean()) irBuiltIns.intType else valueType,
                    isArrayReceiver = isArrayReceiver
                )
                val action = (expression.getValueArgument(0) as IrFunctionExpression).apply {
                    function.body?.transform(this@NativeAtomicFunctionCallTransformer, parentFunction)
                    // todo check for type in extension receiver here
                    if (function.valueParameters[0].type.isBoolean()) {
                        function.valueParameters[0].type = irBuiltIns.intType
                        function.returnType = irBuiltIns.intType
                    }
                }
                return getPropertyRefReceiver(getPropertyReceiver, parentFunction)?.let { propertyRef ->
                    irCallWithArgs(
                        symbol = loopFunc.symbol,
                        dispatchReceiver = parentFunction.containingFunction.dispatchReceiverParameter?.capture(),
                        extensionReceiver = propertyRef,
                        valueArguments = listOf(action)
                    )
                } ?: expression
            }
        }

        override fun visitGetValue(expression: IrGetValue, data: IrFunction?): IrExpression {
            // TODO: abstract out or leave like this: needs refactor and verification!!!!!
            // For transformed atomic extension functions
            // replace old value parameters with the new parameters of the transformed declaration:
            // inline fun foo$atomicfu(dispatchReceiver: Any?, handler: j.u.c.a.AtomicIntegerFieldUpdater, arg': Int) {
            //     arg -> arg`
            //}
            if (expression.symbol is IrValueParameterSymbol) {
                val valueParameter = expression.symbol.owner as IrValueParameter
                val parent = valueParameter.parent
                if (data != null && data.isTransformedAtomicExtension() &&
                    parent is IrFunctionImpl && !parent.isTransformedAtomicExtension() &&
                    parent.origin != IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
                ) {
                    val index = valueParameter.index
                    if (index < 0 && !valueParameter.type.isAtomicValueType()) {
                        // index == -1 for `this` parameter
                        return data.dispatchReceiverParameter?.capture() ?: error { "Dispatch receiver of ${data.render()} is null" }
                    }
                    if (index >= 0) {
                        val transformedValueParameter = data.valueParameters[index]
                        return buildGetValue(
                            expression.startOffset,
                            expression.endOffset,
                            transformedValueParameter.symbol
                        )
                    }
                }
            }
            return super.visitGetValue(expression, data)
        }

        private fun IrDeclarationContainer.getOrBuildInlineLoopFunction(
            functionName: String,
            valueType: IrType,
            isArrayReceiver: Boolean
        ): IrSimpleFunction {
            val parent = this
            val mangledName = mangleAtomicExtensionName(functionName, isArrayReceiver)
            findDeclaration<IrSimpleFunction> {
                it.name.asString() == mangledName &&
                        it.extensionReceiverParameter != null &&
                        it.extensionReceiverParameter!!.type.classOrNull == irBuiltIns.kMutableProperty0Class &&
                        (it.extensionReceiverParameter!!.type as IrSimpleType).arguments.firstOrNull() == valueType
            }?.let { return it }
            return pluginContext.irFactory.buildFun {
                name = Name.identifier(mangledName)
                isInline = true
                visibility = DescriptorVisibilities.PRIVATE
            }.apply {
                addExtensionReceiver(buildSimpleType(irBuiltIns.kMutableProperty0Class, listOf(valueType)))
                dispatchReceiverParameter = (parent as? IrClass)?.thisReceiver?.deepCopyWithSymbols(this)
                addValueParameter(ACTION, atomicSymbols.function1Type(valueType, irBuiltIns.unitType))
                with(atomicSymbols.createBuilder(symbol)) {
                    if (functionName == LOOP) {
                        body = atomicfuLoopBody(valueType, extensionReceiverParameter!!.capture(), valueParameters[0])
                        returnType = irBuiltIns.unitType
                    } else {
                        body = atomicfuUpdateBody(functionName, valueType, extensionReceiverParameter!!.capture(), valueParameters[0])
                        returnType = if (functionName == UPDATE) irBuiltIns.unitType else valueType
                    }
                }
                this.parent = parent
                parent.declarations.add(this)
            }
        }

        private fun IrFunction.isAtomicExtension(): Boolean =
            extensionReceiverParameter != null && isInline && extensionReceiverParameter!!.type.isAtomicValueType()

        override fun IrFunction.isTransformedAtomicExtension(): Boolean =
            extensionReceiverParameter != null && extensionReceiverParameter!!.type.classOrNull == irBuiltIns.kMutableProperty0Class

        override fun transformedCallOnAtomicArrayElement(
            expression: IrCall,
            functionName: String,
            valueType: IrType,
            getPropertyReceiver: IrExpression,
            parentFunction: IrFunction?
        ): IrExpression {
            // todo: not support
            return expression
        }

        private fun getPropertyRefReceiver(
            getPropertyReceiver: IrExpression,
            parentFunction: IrFunction
        ): IrExpression? = when {
            getPropertyReceiver is IrCall -> buildPropertyReference(
                property = getPropertyReceiver.getCorrespondingProperty(),
                classReceiver = getPropertyReceiver.dispatchReceiver
            )
            getPropertyReceiver.isThisReceiver() -> {
                val propertyExtensionReceiver = requireNotNull(parentFunction.extensionReceiverParameter) { "Extension receiver of function $parentFunction should not be null" }
                propertyExtensionReceiver.capture()
            }
            else -> null
        }

        private fun buildPropertyReference(property: IrProperty, classReceiver: IrExpression?): IrPropertyReferenceImpl {
            val backingField = requireNotNull(property.backingField) { "Backing field of the property $property should not be null" }
            return IrPropertyReferenceImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                type = buildSimpleType(irBuiltIns.kMutableProperty0Class, listOf(backingField.type)),
                symbol = property.symbol,
                typeArgumentsCount = 0,
                field = backingField.symbol,
                getter = property.getter?.symbol,
                setter = property.setter?.symbol
            ).apply {
                dispatchReceiver = classReceiver
            }
        }

        override fun IrExpression.isArrayElementReceiver(parentFunction: IrFunction?): Boolean {
            return if (this is IrCall) this.isArrayElementGetter() else false
        }
    }
}
