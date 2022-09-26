/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.backend.common

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.parents
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.jetbrains.kotlinx.atomicfu.compiler.backend.capture
import org.jetbrains.kotlinx.atomicfu.compiler.backend.getValueArguments

private const val AFU_PKG = "kotlinx.atomicfu"
private const val TRACE_BASE_TYPE = "TraceBase"
private const val ATOMIC_VALUE_FACTORY = "atomic"
private const val INVOKE = "invoke"
private const val APPEND = "append"
private const val GET = "get"

abstract class AbstractAtomicfuTransformer(
    val pluginContext: IrPluginContext
) {
    abstract val atomicSymbols: AbstractAtomicSymbols

    protected val irBuiltIns = pluginContext.irBuiltIns

    protected val AFU_VALUE_TYPES: Map<String, IrType> = mapOf(
        "AtomicInt" to irBuiltIns.intType,
        "AtomicLong" to irBuiltIns.longType,
        "AtomicBoolean" to irBuiltIns.booleanType,
        "AtomicRef" to irBuiltIns.anyNType
    )

    protected val ATOMICFU_INLINE_FUNCTIONS = setOf("loop", "update", "getAndUpdate", "updateAndGet")
    protected val ATOMIC_VALUE_TYPES = setOf("AtomicInt", "AtomicLong", "AtomicBoolean", "AtomicRef")
    protected val ATOMIC_ARRAY_TYPES = setOf("AtomicIntArray", "AtomicLongArray", "AtomicBooleanArray", "AtomicArray")

    fun transform(moduleFragment: IrModuleFragment) {
        transformAtomicProperties(moduleFragment)
        transformAtomicExtensions(moduleFragment)
        transformAtomicFunctions(moduleFragment)
        for (irFile in moduleFragment.files) {
            irFile.patchDeclarationParents()
        }
    }

    protected abstract fun transformAtomicProperties(moduleFragment: IrModuleFragment)

    protected abstract fun transformAtomicExtensions(moduleFragment: IrModuleFragment)

    protected abstract fun transformAtomicFunctions(moduleFragment: IrModuleFragment)

    protected abstract inner class AtomicPropertiesTransformer : IrElementTransformer<IrFunction?> {
        override fun visitClass(declaration: IrClass, data: IrFunction?): IrStatement {
            declaration.declarations.filter(::isKotlinxAtomicfuProperty).forEach {
                (it as IrProperty).transformKotlinxAtomicfuProperty()
            }
            return super.visitClass(declaration, data)
        }

        override fun visitFile(declaration: IrFile, data: IrFunction?): IrFile {
            declaration.declarations.filter(::isKotlinxAtomicfuProperty).forEach {
                (it as IrProperty).transformKotlinxAtomicfuProperty()
            }
            return super.visitFile(declaration, data)
        }

        private fun IrProperty.transformKotlinxAtomicfuProperty() {
            val atomicfuProperty = this
            val parentContainer = atomicfuProperty.parents.firstIsInstance<IrDeclarationContainer>()
            val isTopLevel = parentContainer is IrFile || (parentContainer is IrClass && parentContainer.kind == ClassKind.OBJECT)
            when {
                isAtomic() -> {
                    if (isTopLevel) {
                        transformStaticAtomic(parentContainer)
                    } else {
                        transformInClassAtomic(parentContainer as IrClass)
                    }
                }
                isDelegatedToAtomic() -> transformDelegatedAtomic(parentContainer)
                isAtomicArray() -> transformAtomicArray(parentContainer)
                isTrace() -> parentContainer.declarations.remove(atomicfuProperty)
                else -> {}
            }
        }

        abstract fun IrProperty.transformStaticAtomic(parentContainer: IrDeclarationContainer)

        abstract fun IrProperty.transformInClassAtomic(parentContainer: IrClass)

        abstract fun IrProperty.transformDelegatedAtomic(parentContainer: IrDeclarationContainer)

        abstract fun IrProperty.transformAtomicArray(parentContainer: IrDeclarationContainer)

        protected fun buildVolatileField(
            property: IrProperty,
            parentContainer: IrDeclarationContainer
        ): IrField {
            // Generate a new backing field for the given property: a volatile variable of the atomic value type
            // val a = atomic(0)
            // volatile var a: Int = 0
            val atomicField = requireNotNull(property.backingField) { "BackingField of atomic property $property is null" }
            val fieldType = atomicField.type.atomicToValueType()
            val initValue =
                (atomicField.initializer?.expression ?: atomicField.getFieldInitializerFromInitBlock(parentContainer)?.value)?.let {
                    (it as IrCall).getAtomicFactoryValueArgument()
                } ?: error("Atomic property $property was not initialized")
            return with(atomicSymbols.createBuilder(atomicField.symbol)) {
                irVolatileField(
                    property.name,
                    if (fieldType.isBoolean()) irBuiltIns.intType else fieldType,
                    initValue,
                    atomicField.annotations,
                    parentContainer
                )
            }
        }

        protected fun IrField.getFieldInitializerFromInitBlock(
            parentContainer: IrDeclarationContainer
        ): IrSetField? {
            val field = this
            for (declaration in parentContainer.declarations) {
                if (declaration is IrAnonymousInitializer) {
                    return declaration.body.statements.singleOrNull {
                        it is IrSetField && it.symbol == field.symbol
                    }.also {
                        declaration.body.statements.remove(it)
                    } as IrSetField?
                }
            }
            return null
        }

        protected fun IrCall.getAtomicFactoryValueArgument() =
            getValueArgument(0)?.deepCopyWithSymbols()
                ?: error("Atomic factory should take at least one argument: ${this.render()}")

        protected fun IrFunctionAccessExpression.getArraySizeArgument() =
            getValueArgument(0)?.deepCopyWithSymbols()
                ?: error("Atomic array constructor should take at least one argument: ${this.render()}")
    }

    protected abstract inner class AtomicExtensionTransformer : IrElementTransformerVoid() {
        override fun visitFile(declaration: IrFile): IrFile {
            declaration.transformAllAtomicExtensions()
            return super.visitFile(declaration)
        }

        override fun visitClass(declaration: IrClass): IrStatement {
            declaration.transformAllAtomicExtensions()
            return super.visitClass(declaration)
        }

        private fun IrDeclarationContainer.transformAllAtomicExtensions() {
            declarations.filter { it is IrFunction && it.isAtomicExtension() }.forEach { atomicExtension ->
                atomicExtension as IrFunction
                declarations.add(atomicExtension.transformAtomicExtension(this, false))
                declarations.add(atomicExtension.transformAtomicExtension(this, true))
                declarations.remove(atomicExtension)
            }
        }

        abstract fun IrFunction.transformAtomicExtension(
            parent: IrDeclarationContainer,
            isArrayReceiver: Boolean
        ): IrFunction
    }

    protected abstract inner class AtomicFunctionCallTransformer : IrElementTransformer<IrFunction?> {

        override fun visitFunction(declaration: IrFunction, data: IrFunction?): IrStatement {
            return super.visitFunction(declaration, declaration)
        }

        override fun visitCall(expression: IrCall, data: IrFunction?): IrElement {
            (expression.extensionReceiver ?: expression.dispatchReceiver)?.transform(this, data)?.let {
                val receiver = if (it is IrTypeOperatorCallImpl) it.argument else it
                if (receiver.type.isAtomicValueType()) {
                    val valueType = if (it is IrTypeOperatorCallImpl) {
                        // If receiverExpression is a cast `s as AtomicRef<String>`
                        // then valueType is the type argument of Atomic* class `String`
                        (it.type as IrSimpleType).arguments[0] as IrSimpleType
                    } else {
                        receiver.type.atomicToValueType()
                    }
                    // this maybe AtomicInt.loop { CAS }
                    // called on intArray[0].loop { CAS } -> then CAS receiver
                    val isArrayReceiver = receiver.isArrayElementReceiver(data)
                    if (expression.symbol.fromKotlinxAtomicfuPackage()) {
                        // Transform invocations of atomic functions on atomics or atomic array elements
                        val functionName = expression.symbol.owner.name.asString()
                        if (functionName in ATOMICFU_INLINE_FUNCTIONS) {
                            val loopCall = transformedAtomicfuInlineFunctionCall(
                                expression = expression,
                                functionName = functionName,
                                valueType = valueType,
                                receiver = receiver,
                                isArrayReceiver = isArrayReceiver,
                                parentFunction = data
                            )
                            return super.visitCall(loopCall, data)
                        }
                        val irCall = if (isArrayReceiver) {
                            transformedCallOnAtomicArrayElement(
                                expression = expression,
                                functionName = functionName,
                                valueType = valueType,
                                receiver = receiver,
                                parentFunction = data
                            )
                        } else {
                            transformedCallOnAtomic(
                                expression = expression,
                                functionName = functionName,
                                valueType = valueType,
                                castType = if (it is IrTypeOperatorCall) valueType else null,
                                receiver = receiver,
                                parentFunction = data
                            )
                        }
                        return super.visitExpression(irCall, data)
                    }
                    if (expression.symbol.owner.isInline && expression.extensionReceiver != null) {
                        // Transform invocation of Atomic* extension functions, delegating them to the corresponding transformed atomic extensions:
                        val declaration = expression.symbol.owner
                        val parent = declaration.parent as IrDeclarationContainer
                        val transformedAtomicExtension = parent.getTransformedAtomicExtension(declaration, isArrayReceiver)
                        val irCall = transformedAtomicExtensionCall(
                            expression = expression,
                            transformedAtomicExtension = transformedAtomicExtension,
                            receiver = receiver,
                            isArrayReceiver = isArrayReceiver,
                            parentFunction = data
                        )
                        return super.visitCall(irCall, data)
                    }
                    return super.visitCall(expression, data)
                }
            }
            return super.visitCall(expression, data)
        }

        override fun visitBlockBody(body: IrBlockBody, data: IrFunction?): IrBody {
            // Erase messages added by the Trace object from the function body:
            // val trace = Trace(size)
            // Messages may be added via trace invocation:
            // trace { "Doing something" }
            // or via multi-append of arguments:
            // trace.append(index, "CAS", value)
            body.statements.removeIf {
                it.isTraceCall()
            }
            return super.visitBlockBody(body, data)
        }

        override fun visitContainerExpression(expression: IrContainerExpression, data: IrFunction?): IrExpression {
            // Erase messages added by the Trace object from blocks.
            expression.statements.removeIf {
                it.isTraceCall()
            }
            return super.visitContainerExpression(expression, data)
        }

        abstract fun transformedCallOnAtomic(
            expression: IrCall,
            functionName: String,
            valueType: IrType,
            castType: IrType?,
            receiver: IrExpression,
            parentFunction: IrFunction?
        ): IrExpression

        abstract fun transformedCallOnAtomicArrayElement(
            expression: IrCall,
            functionName: String,
            valueType: IrType,
            receiver: IrExpression,
            parentFunction: IrFunction?
        ): IrExpression

        abstract fun transformedAtomicfuInlineFunctionCall(
            expression: IrCall,
            functionName: String,
            valueType: IrType,
            receiver: IrExpression,
            isArrayReceiver: Boolean,
            parentFunction: IrFunction?
        ): IrCall

        abstract fun transformedAtomicExtensionCall(
            expression: IrCall,
            transformedAtomicExtension: IrSimpleFunction,
            receiver: IrExpression,
            isArrayReceiver: Boolean,
            parentFunction: IrFunction?
        ): IrCall

        abstract fun IrDeclarationContainer.getTransformedAtomicExtension(
            declaration: IrSimpleFunction,
            isArrayReceiver: Boolean
        ): IrSimpleFunction

        abstract fun IrExpression.isArrayElementReceiver(
            parentFunction: IrFunction?
        ): Boolean
    }

    // util transformer functions

    protected fun IrType.fromKotlinxAtomicfuPackage() =
        classFqName?.let { it.parent().asString() == AFU_PKG } ?: false

    // todo replace with fromKotlinxAtomicfu
    protected fun IrSimpleFunctionSymbol.fromKotlinxAtomicfuPackage(): Boolean =
        owner.parentClassOrNull?.classId?.let {
            it.packageFqName.asString() == AFU_PKG
        } ?: false

    protected fun isKotlinxAtomicfuProperty(declaration: IrDeclaration): Boolean =
        declaration is IrProperty &&
                declaration.backingField?.type?.fromKotlinxAtomicfuPackage() ?: false

    // TODO: rename
    protected fun IrProperty.isAtomic(): Boolean =
        !isDelegated && backingField?.type?.isAtomicValueType() ?: false

    protected fun IrProperty.isDelegatedToAtomic(): Boolean =
        isDelegated && backingField?.type?.isAtomicValueType() ?: false

    protected fun IrProperty.isAtomicArray(): Boolean =
        backingField?.type?.isAtomicArrayType() ?: false

    protected fun IrProperty.isTrace(): Boolean =
        backingField?.type?.isTraceBaseType() ?: false

    // TODO: rename
    protected fun IrType.isAtomicValueType() =
        classFqName?.let {
            it.parent().asString() == AFU_PKG && it.shortName().asString() in ATOMIC_VALUE_TYPES
        } ?: false

    protected fun IrType.isAtomicArrayType() =
        classFqName?.let {
            it.parent().asString() == AFU_PKG && it.shortName().asString() in ATOMIC_ARRAY_TYPES
        } ?: false

    protected fun IrType.isTraceBaseType() =
        classFqName?.let {
            it.parent().asString() == AFU_PKG && it.shortName().asString() == TRACE_BASE_TYPE
        } ?: false

    protected fun IrCall.isTraceInvoke(): Boolean =
        symbol.fromKotlinxAtomicfuPackage() &&
                symbol.owner.name.asString() == INVOKE &&
                symbol.owner.dispatchReceiverParameter?.type?.isTraceBaseType() == true

    protected fun IrCall.isTraceAppend(): Boolean =
        symbol.fromKotlinxAtomicfuPackage() &&
                symbol.owner.name.asString() == APPEND &&
                symbol.owner.dispatchReceiverParameter?.type?.isTraceBaseType() == true

    protected fun IrStatement.isTraceCall() = this is IrCall && (isTraceInvoke() || isTraceAppend())

    protected fun IrCall.isArrayElementGetter(): Boolean =
        dispatchReceiver?.let {
            it.type.isAtomicArrayType() && symbol.owner.name.asString() == GET
        } ?: false

    protected fun IrType.atomicToValueType(): IrType =
        classFqName?.let {
            AFU_VALUE_TYPES[it.shortName().asString()]
        } ?: error("No corresponding value type was found for this atomic type: ${this.render()}")

    protected fun IrCall.isAtomicFactory(): Boolean =
        symbol.fromKotlinxAtomicfuPackage() && symbol.owner.name.asString() == ATOMIC_VALUE_FACTORY &&
                type.isAtomicValueType()

    protected fun IrFunction.isAtomicExtension(): Boolean =
        extensionReceiverParameter?.let { it.type.isAtomicValueType() && this.isInline } ?: false
}
