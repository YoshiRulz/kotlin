/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.transformer

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.visitors.IrTypeTransformerVoid

internal class IrConstTypeAnnotationTransformer : IrConstAnnotationTransformer(),
    IrTypeTransformerVoid<Nothing?> {

    override fun <Type : IrType?> transformType(container: IrElement, type: Type, data: Nothing?): Type {
        if (type == null) return type

        transformAnnotations(type)
        if (type is IrSimpleType) {
            type.arguments.mapNotNull { it.typeOrNull }.forEach { transformType(container, it, data) }
        }
        return type
    }
}
