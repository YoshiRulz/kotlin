/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.backend.common

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.declarations.buildValueParameter
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.util.addChild
import org.jetbrains.kotlin.ir.util.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.ir.util.getSimpleFunction
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

abstract class AbstractAtomicSymbols(
    val context: IrPluginContext,
    val moduleFragment: IrModuleFragment
) {
    val irBuiltIns: IrBuiltIns = context.irBuiltIns
    protected val irFactory: IrFactory = IrFactoryImpl

    val invoke0Symbol = irBuiltIns.functionN(0).getSimpleFunction("invoke")!!
    val invoke1Symbol = irBuiltIns.functionN(1).getSimpleFunction("invoke")!!

    abstract val volatileAnnotationClass: IrClass
    val volatileAnnotationConstructorCall: IrConstructorCall
        get() {
            val volatileAnnotationConstructor = buildAnnotationConstructor(volatileAnnotationClass)
            return IrConstructorCallImpl.fromSymbolOwner(volatileAnnotationConstructor.returnType, volatileAnnotationConstructor.symbol)
        }

    abstract fun createBuilder(
        symbol: IrSymbol,
        startOffset: Int = UNDEFINED_OFFSET,
        endOffset: Int = UNDEFINED_OFFSET
    ): AbstractAtomicfuIrBuilder

    protected fun buildAnnotationConstructor(annotationClass: IrClass): IrConstructor =
        annotationClass.addConstructor { isPrimary = true }

    protected fun createPackage(packageName: String): IrPackageFragment =
        IrExternalPackageFragmentImpl.createEmptyExternalPackageFragment(
            moduleFragment.descriptor,
            FqName(packageName)
        )

    protected fun createClass(
        irPackage: IrPackageFragment,
        shortName: String,
        classKind: ClassKind,
        classModality: Modality,
        isValueClass: Boolean = false,
    ): IrClassSymbol = irFactory.buildClass {
        name = Name.identifier(shortName)
        kind = classKind
        modality = classModality
        isValue = isValueClass
    }.apply {
        parent = irPackage
        createImplicitParameterDeclarationWithWrappedDescriptor()
    }.symbol

    // TODO: review the visibility of the function
    fun buildClass(
        fqName: FqName,
        classKind: ClassKind,
        parent: IrDeclarationContainer
    ): IrClass = irFactory.buildClass {
        name = fqName.shortName()
        kind = classKind
    }.apply {
        val irClass = this
        this.parent = parent
        parent.addChild(irClass)
        thisReceiver = buildValueParameter(irClass) {
            name = Name.identifier("\$this")
            type = IrSimpleTypeImpl(irClass.symbol, false, emptyList(), emptyList())
        }
    }
}
