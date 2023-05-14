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
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.jetbrains.kotlinx.atomicfu.compiler.backend.buildSetField
import org.jetbrains.kotlinx.atomicfu.compiler.backend.getInitBlockForField

private const val ATOMICFU = "atomicfu"
private const val ATOMIC_ARRAY_RECEIVER_SUFFIX = "\$array"
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
        // start statistics
        val startCounter = AtomicBoxesCounter()
        for (irFile in moduleFragment.files) {
            irFile.transform(startCounter, null)
        }
        println("moduleFragment: ${moduleFragment.name}: Start atomic boxes = ${startCounter.counter}")

        transformAtomicProperties(moduleFragment)
        transformAtomicExtensions(moduleFragment)
        transformAtomicFunctions(moduleFragment)
        for (irFile in moduleFragment.files) {
            irFile.patchDeclarationParents()
        }
        // end statistics
        val endCounter = AtomicBoxesCounter()
        for (irFile in moduleFragment.files) {
            irFile.transform(endCounter, null)
            if (moduleFragment.name.asString().contains("nativeBox")) {
                if (irFile.dump().contains("kotlinx.atomicfu")) {
                    // todo a more intellectual test that leaves atomic arrays and extensions that are called on untransformed properties
                    println("Atomicfu reference detected in tests in irFile: \n ${irFile.dump()}")
                }
            }
            //if (irFile.name == "BufferedChannel.kt") {
                //println("-------------------------------- AAAAAAAAAAA FILE RENDER: --------------------------------")
                //error(irFile.dump())
                //println("-------------------------------- END --------------------------------")
            //}
//            println("---------- start of the dump ${irFile.name} -------------")
            println(irFile.dump())
//            println("---------- end of the dump -------------")
        }
        println("moduleFragment: ${moduleFragment.name}: End atomic boxes = ${endCounter.counter}")
    }

    protected abstract fun transformAtomicProperties(moduleFragment: IrModuleFragment)

    protected abstract fun transformAtomicExtensions(moduleFragment: IrModuleFragment)

    protected abstract fun transformAtomicFunctions(moduleFragment: IrModuleFragment)

    private inner class AtomicBoxesCounter : IrElementTransformer<IrFunction?> {
        var counter: Int = 0

        override fun visitProperty(declaration: IrProperty, data: IrFunction?): IrStatement {
            declaration.backingField?.let {
                if (it.type.classFqName?.parent()?.asString() == AFU_PKG) counter++
            }
            return super.visitProperty(declaration, data)
        }
    }

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

        protected fun buildVolatileBackingField(atomicProperty: IrProperty, parentContainer: IrDeclarationContainer): IrField {
            // Generate a new backing field for the given property: a volatile variable of the atomic value type
            // val a = atomic(0)
            // volatile var a: Int = 0
            val atomicField = requireNotNull(atomicProperty.backingField) { "BackingField of atomic property $atomicProperty should not be null" }
            val fieldType = atomicField.type.atomicToValueType()
            val initializer = atomicField.initializer?.expression
            val initBlock = if (initializer == null) atomicField.getInitBlockForField(parentContainer) else null
            val atomicFactoryCall = initializer
                ?: initBlock?.getValueFromInitBlock(atomicField.symbol)
                ?: error("Atomic property ${atomicProperty.dump()} should be initialized")
            require(atomicFactoryCall is IrCall) { "Atomic property ${atomicProperty.render()} should be initialized with atomic factory call" }
            val initValue = atomicFactoryCall.getAtomicFactoryValueArgument()
            return with(atomicSymbols.createBuilder(atomicField.symbol)) {
                irVolatileField(
                    atomicProperty.name,
                    if (fieldType.isBoolean()) irBuiltIns.intType else fieldType, // boolean fields can only be updated with AtomicIntegerFieldUpdater
                    if (initializer == null) null else initValue,
                    atomicField.annotations,
                    parentContainer
                ).also {
                    initBlock?.updateFieldInitialization(atomicField.symbol, it.symbol, initValue)
                }
            }
        }

        protected fun IrAnonymousInitializer.getValueFromInitBlock(
            oldFieldSymbol: IrFieldSymbol
        ): IrExpression? =
            body.statements.singleOrNull { it is IrSetField && it.symbol == oldFieldSymbol }?.let { (it as IrSetField).value }

        protected fun IrAnonymousInitializer.updateFieldInitialization(
            oldFieldSymbol: IrFieldSymbol,
            volatileFieldSymbol: IrFieldSymbol,
            initExpr: IrExpression
        ) {
            body.statements.singleOrNull {
                it is IrSetField && it.symbol == oldFieldSymbol
            }?.let {
                it as IrSetField
                val irSetField = pluginContext.buildSetField(volatileFieldSymbol, it.receiver, initExpr)
                body.statements.add(irSetField)
                body.statements.remove(it)
            }
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

        abstract fun IrDeclarationContainer.transformAllAtomicExtensions()
    }

    protected abstract inner class AtomicFunctionCallTransformer : IrElementTransformer<IrFunction?> {

        override fun visitFunction(declaration: IrFunction, data: IrFunction?): IrStatement {
            return super.visitFunction(declaration, declaration)
        }

        override fun visitCall(expression: IrCall, data: IrFunction?): IrElement {
            (expression.extensionReceiver ?: expression.dispatchReceiver)?.transform(this, data)?.let {
                val propertyGetterCall = if (it is IrTypeOperatorCallImpl) it.argument else it
                 if (propertyGetterCall.type.isAtomicValueType()) {
                    val valueType = if (it is IrTypeOperatorCallImpl) {
                        // If receiverExpression is a cast `s as AtomicRef<String>`
                        // then valueType is the type argument of Atomic* class `String`
                        (it.type as IrSimpleType).arguments[0] as IrSimpleType
                    } else {
                        propertyGetterCall.type.atomicToValueType()
                    }
                    // this maybe AtomicInt.loop { CAS }
                    // called on intArray[0].loop { CAS } -> then CAS receiver
                    val isArrayReceiver = propertyGetterCall.isArrayElementReceiver(data)
                    if (expression.symbol.fromKotlinxAtomicfuPackage()) {
                        // Transform invocations of atomic functions on atomics or atomic array elements
                        val functionName = expression.symbol.owner.name.asString()
                        if (functionName in ATOMICFU_INLINE_FUNCTIONS) {
                            val loopCall = transformedAtomicfuInlineFunctionCall(
                                expression = expression,
                                functionName = functionName,
                                valueType = valueType,
                                getPropertyReceiver = propertyGetterCall,
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
                                getPropertyReceiver = propertyGetterCall,
                                parentFunction = data
                            )
                        } else {
                            transformedCallOnAtomic(
                                expression = expression,
                                functionName = functionName,
                                valueType = valueType,
                                castType = if (it is IrTypeOperatorCall) valueType else null,
                                getPropertyReceiver = propertyGetterCall,
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
                            getPropertyReceiver = propertyGetterCall,
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
            getPropertyReceiver: IrExpression,
            parentFunction: IrFunction?
        ): IrExpression

        abstract fun transformedCallOnAtomicArrayElement(
            expression: IrCall,
            functionName: String,
            valueType: IrType,
            getPropertyReceiver: IrExpression,
            parentFunction: IrFunction?
        ): IrExpression

        abstract fun transformedAtomicfuInlineFunctionCall(
            expression: IrCall,
            functionName: String,
            valueType: IrType,
            getPropertyReceiver: IrExpression,
            isArrayReceiver: Boolean,
            parentFunction: IrFunction?
        ): IrCall

        abstract fun transformedAtomicExtensionCall(
            expression: IrCall,
            transformedAtomicExtension: IrSimpleFunction,
            getPropertyReceiver: IrExpression,
            isArrayReceiver: Boolean,
            parentFunction: IrFunction?
        ): IrCall

        private fun IrDeclarationContainer.getTransformedAtomicExtension(
            declaration: IrSimpleFunction,
            isArrayReceiver: Boolean
        ): IrSimpleFunction =
            findDeclaration {
                it.name.asString() == mangleAtomicExtensionName(declaration.name.asString(), isArrayReceiver) &&
                        it.isTransformedAtomicExtension()
            } ?: error("Could not find corresponding transformed declaration for the atomic extension ${declaration.render()}, $isArrayReceiver")

        abstract fun IrFunction.isTransformedAtomicExtension(): Boolean

        abstract fun IrExpression.isArrayElementReceiver(
            parentFunction: IrFunction?
        ): Boolean
    }

    // util transformer functions

    protected fun IrType.fromKotlinxAtomicfuPackage() =
        classFqName?.let { it.parent().asString() == AFU_PKG } ?: false

    // todo replace with fromKotlinxAtomicfu
    protected fun IrSimpleFunctionSymbol.fromKotlinxAtomicfuPackage(): Boolean =
        // todo check if this works for all cases
        owner.parentDeclarationContainer.kotlinFqName.asString().startsWith(AFU_PKG)
//        owner.parentClassOrNull?.classId?.let {
//            it.packageFqName.asString() == AFU_PKG
//        } ?: false

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

    protected fun IrCall.getCorrespondingProperty(): IrProperty =
        symbol.owner.correspondingPropertySymbol?.owner
            ?: error("Atomic property accessor ${this.render()} expected to have non-null correspondingPropertySymbol")

    protected fun mangleAtomicExtensionName(name: String, isArrayReceiver: Boolean) =
        if (isArrayReceiver) "$name$$ATOMICFU$ATOMIC_ARRAY_RECEIVER_SUFFIX" else "$name$$ATOMICFU"

    protected fun IrExpression.isThisReceiver() =
        this is IrGetValue && symbol.owner.name.asString() == "<this>"

    protected val IrDeclaration.parentDeclarationContainer: IrDeclarationContainer
        get() = parents.filterIsInstance<IrDeclarationContainer>().firstOrNull()
            ?: error("In the sequence of parents for ${this.render()} no IrDeclarationContainer was found")

    protected val IrFunction.containingFunction: IrFunction
        get() {
            if (this.origin != IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA) return this
            return parents.filterIsInstance<IrFunction>().firstOrNull {
                it.origin != IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
            } ?: error("In the sequence of parents for the local function ${this.render()} no containing function was found")
        }
}
