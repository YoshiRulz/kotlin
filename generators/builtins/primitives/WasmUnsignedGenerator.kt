/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.builtins.numbers.primitives

import org.jetbrains.kotlin.generators.builtins.UnsignedType
import org.jetbrains.kotlin.generators.builtins.generateBuiltIns.BuiltInsGenerator
import org.jetbrains.kotlin.generators.builtins.unsigned.UnsignedTypeGenerator
import java.io.File
import java.io.PrintWriter

fun generateWasmUnsignedTypes(
    targetDir: File,
    generate: (File, (PrintWriter) -> BuiltInsGenerator) -> Unit
) {
    for (type in UnsignedType.values()) {
        generate(File(targetDir, "kotlin/${type.capitalized}.kt")) { WasmSingleUnsignedGenerator(type, it) }
    }
}

private class WasmSingleUnsignedGenerator(type: UnsignedType, out: PrintWriter) : UnsignedTypeGenerator(type, out) {
    override fun FileBuilder.modifyGeneratedFile() {
        suppress("OVERRIDE_BY_INLINE")
        suppress("NOTHING_TO_INLINE")
        suppress("unused")
        suppress("UNUSED_PARAMETER")
        import("kotlin.wasm.internal.*")
    }

    override fun ClassBuilder.modifyGeneratedClass() {
        annotations += listOf(
            "SinceKotlin(\"1.5\")",
            "WasExperimental(ExperimentalUnsignedTypes::class)",
            "WasmAutoboxed"
        )
    }

    override fun MethodBuilder.modifyGeneratedCompareTo(otherType: UnsignedType) {
        modifySignature { isInline = type == otherType }

        if (otherType == type) {
            val body = when (type) {
                UnsignedType.UBYTE -> "wasm_u32_compareTo(this.toInt(), $parameterName.toInt())"
                UnsignedType.USHORT -> "this.toInt().compareTo($parameterName.toInt())"
                UnsignedType.UINT, UnsignedType.ULONG -> "wasm_${type.prefixLowercase}_compareTo(this, $parameterName)"
            }
            body.addAsSingleLineBody(bodyOnNewLine = true)
            return
        }

        val thisCasted = "this.to${otherType.capitalized}()"
        "$thisCasted.compareTo($parameterName)".addAsSingleLineBody(bodyOnNewLine = true)
    }

    private val UnsignedType.prefixUppercase: String
        get() = when (this) {
            UnsignedType.UBYTE, UnsignedType.USHORT, UnsignedType.UINT -> "U32"
            UnsignedType.ULONG -> "U64"
        }

    private val UnsignedType.prefixLowercase: String
        get() = prefixUppercase.lowercase()
}
