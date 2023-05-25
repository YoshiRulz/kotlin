/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.builtins.numbers.primitives

import org.jetbrains.kotlin.generators.builtins.PrimitiveType
import org.jetbrains.kotlin.generators.builtins.UnsignedType

internal const val END_LINE = "\n"

internal fun UnsignedType.castToIfNecessary(otherType: UnsignedType): String {
    if (this == otherType) return ""

    if (this.ordinal < otherType.ordinal) {
        return ".to${otherType.capitalized}()"
    }

    return ""
}

internal val UnsignedType.prefixUppercase: String
    get() = when (this) {
        UnsignedType.UBYTE, UnsignedType.USHORT, UnsignedType.UINT -> "U32"
        UnsignedType.ULONG -> "U64"
    }

internal val PrimitiveType.prefixUppercase: String
    get() = when (this) {
        PrimitiveType.BYTE, PrimitiveType.SHORT, PrimitiveType.INT -> "I32"
        PrimitiveType.LONG -> "I64"
        PrimitiveType.FLOAT -> "F32"
        PrimitiveType.DOUBLE -> "F64"
        else -> error("Unexpected primitive type")
    }

internal val UnsignedType.prefixLowercase: String
    get() = prefixUppercase.lowercase()

internal val PrimitiveType.prefixLowercase: String
    get() = prefixUppercase.lowercase()

internal fun PrimitiveType.castToIfNecessary(otherType: PrimitiveType): String {
    if (this !in PrimitiveType.onlyNumeric || otherType !in PrimitiveType.onlyNumeric) {
        throw IllegalArgumentException("Cannot cast to non-numeric type")
    }

    if (this == otherType) return ""

    if (this.ordinal < otherType.ordinal) {
        return ".to${otherType.capitalized}()"
    }

    return ""
}

internal fun operatorSign(methodName: String): String {
    return when (methodName) {
        "plus" -> "+"
        "minus" -> "-"
        "times" -> "*"
        "div" -> "/"
        "rem" -> "%"
        else -> throw IllegalArgumentException("Unsupported binary operation: ${methodName}")
    }
}

internal fun String.toPrimitiveType(): PrimitiveType {
    return PrimitiveType.valueOf(this.uppercase())
}
