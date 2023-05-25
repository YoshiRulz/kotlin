/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.builtins.numbers.primitives

import org.jetbrains.kotlin.generators.builtins.PrimitiveType
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
    companion object {
        private val MAX_VALUES = mapOf(
            UnsignedType.UBYTE to "${UByte.MAX_VALUE}u",
            UnsignedType.USHORT to "${UShort.MAX_VALUE}u",
            UnsignedType.UINT to "${UInt.MAX_VALUE}u",
            UnsignedType.ULONG to "${ULong.MAX_VALUE}u",
        )
    }

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

        isValue = false

        constructor {
            visibility = "private"
            param {
                name = "private val value"
                type = this@WasmSingleUnsignedGenerator.type.capitalized
            }
        }

        generateHashCode()
        generateEquals()
        generateCustomEquals()

        when (type) {
            UnsignedType.UBYTE, UnsignedType.USHORT ->
                generateReinterpret(UnsignedType.UINT.capitalized)
            UnsignedType.UINT -> {
                setOf(UnsignedType.UBYTE.capitalized, UnsignedType.USHORT.capitalized, PrimitiveType.INT.capitalized)
                    .forEach { generateReinterpret(it) }
            }
            UnsignedType.ULONG -> generateReinterpret(PrimitiveType.LONG.capitalized)
        }
    }

    override fun PropertyBuilder.modifyGeneratedCompanionObjectProperty() {
        when (name) {
            "MIN_VALUE" -> value = "0u"
            "MAX_VALUE" -> value = MAX_VALUES[this@WasmSingleUnsignedGenerator.type]
        }
    }

    override fun MethodBuilder.modifyGeneratedBinaryOperation() {
        annotations += "kotlin.internal.IntrinsicConstEvaluation"

        if (type.capitalized == parameterType && parameterType == returnType) {
            when (methodName) {
                "plus", "minus", "times" ->
                    "this.to${type.asSigned.capitalized}().$methodName(other.to${type.asSigned.capitalized}()).to${type.capitalized}()".addAsSingleLineBody()
                "div" -> {
                    isInline = false
                    annotations += "WasmOp(WasmOp.${type.asSigned.prefixUppercase}_DIV_U)"
                    "implementedAsIntrinsic".addAsSingleLineBody()
                }
                "rem" -> {
                    isInline = false
                    annotations += "WasmOp(WasmOp.${type.asSigned.prefixUppercase}_REM_U)"
                    "implementedAsIntrinsic".addAsSingleLineBody()
                }
            }
        }
    }

    override fun MethodBuilder.modifyGeneratedFloorDivModOperator() {
        annotations += "kotlin.internal.IntrinsicConstEvaluation"
    }

    override fun MethodBuilder.modifyGeneratedFloatingConversions() {
        annotations += "kotlin.internal.IntrinsicConstEvaluation"
        modifySignature { isInline = false }
        when (type) {
            UnsignedType.UBYTE, UnsignedType.USHORT, UnsignedType.UINT -> when (returnType) {
                // byte to byte conversion impossible here due to earlier check on type equality
                PrimitiveType.FLOAT.capitalized -> "wasm_f32_convert_i32_u(this.toInt())"
                PrimitiveType.DOUBLE.capitalized -> "wasm_f64_convert_i32_u(this.toInt())"
                else -> throw IllegalArgumentException("Unsupported type $returnType for generation conversion method from type $type")
            }
            UnsignedType.ULONG -> when (returnType) {
                PrimitiveType.FLOAT.capitalized -> "wasm_f32_convert_i64_u(this.toLong())"
                PrimitiveType.DOUBLE.capitalized -> "wasm_f64_convert_i64_u(this.toLong())"
                else -> throw IllegalArgumentException("Unsupported type $returnType for generation conversion method from type $type")
            }
        }.addAsSingleLineBody()
    }

    override fun MethodBuilder.modifyGeneratedConversions(unsignedType: UnsignedType) {
        annotations += "kotlin.internal.IntrinsicConstEvaluation"

        if (type == unsignedType) return

        when (type) {
            UnsignedType.UBYTE, UnsignedType.USHORT -> when (returnType) {
                UnsignedType.UBYTE.capitalized -> "this.toUInt().toUByte()"
                UnsignedType.USHORT.capitalized -> "this.toUInt().reinterpretAsUShort()"
                UnsignedType.UINT.capitalized -> "reinterpretAsUInt()"
                UnsignedType.ULONG.capitalized -> "this.toUInt().toULong()"
                else -> throw IllegalArgumentException("Unsupported type $returnType for generation conversion method from type $type")
            }
            UnsignedType.UINT -> when (returnType) {
                UnsignedType.UBYTE.capitalized -> "((this shl 24) shr 24).reinterpretAsUByte()"
                UnsignedType.USHORT.capitalized -> "((this shl 16) shr 16).reinterpretAsUShort()"
                UnsignedType.ULONG.capitalized -> "wasm_i64_extend_i32_u(this.toInt())"
                    .also { isInline = false }
                else -> throw IllegalArgumentException("Unsupported type $returnType for generation conversion method from type $type")
            }
            UnsignedType.ULONG -> when (returnType) {
                UnsignedType.UBYTE.capitalized, UnsignedType.USHORT.capitalized -> "this.toUInt().to${returnType}()"
                UnsignedType.UINT.capitalized -> "wasm_i32_wrap_i64(this.toLong()).toUInt()".also { isInline = false }
                else -> throw IllegalArgumentException("Unsupported type $returnType for generation conversion method from type $type")
            }
        }.addAsSingleLineBody()
    }

    override fun MethodBuilder.modifyGeneratedConversions(signedType: PrimitiveType) {
        annotations += "kotlin.internal.IntrinsicConstEvaluation"

        when (type) {
            UnsignedType.UBYTE, UnsignedType.USHORT -> when (returnType) {
                PrimitiveType.BYTE.capitalized, PrimitiveType.SHORT.capitalized -> "this.toInt().to${returnType}()"
                PrimitiveType.INT.capitalized -> "this.toUInt().reinterpretAsInt()"
                PrimitiveType.LONG.capitalized -> "this.toInt().toLong()"
                else -> throw IllegalArgumentException("Unsupported type $returnType for generation conversion method from type $type")
            }
            UnsignedType.UINT -> when (returnType) {
                PrimitiveType.BYTE.capitalized -> "((this shl 24) shr 24).toInt().reinterpretAsByte()"
                PrimitiveType.SHORT.capitalized -> "((this shl 16) shr 16).toInt().reinterpretAsShort()"
                PrimitiveType.INT.capitalized -> "reinterpretAsInt()"
                PrimitiveType.LONG.capitalized -> "wasm_i64_extend_i32_u(this.toInt()).toLong()".also { isInline = false }
                else -> throw IllegalArgumentException("Unsupported type $returnType for generation conversion method from type $type")
            }
            UnsignedType.ULONG -> when (returnType) {
                PrimitiveType.BYTE.capitalized, PrimitiveType.SHORT.capitalized -> "this.toInt().to${returnType}()"
                PrimitiveType.INT.capitalized -> "wasm_i32_wrap_i64(this.toLong())".also { isInline = false }
                PrimitiveType.LONG.capitalized -> "reinterpretAsLong()"
                else -> throw IllegalArgumentException("Unsupported type $returnType for generation conversion method from type $type")
            }
        }.addAsSingleLineBody()
    }

    override fun MethodBuilder.modifyGeneratedBitwiseOperators(operatorName: String) {
        annotations += "kotlin.internal.IntrinsicConstEvaluation"
        val typeToCast = when (type) {
            UnsignedType.UBYTE, UnsignedType.USHORT, UnsignedType.UINT -> PrimitiveType.INT
            UnsignedType.ULONG -> PrimitiveType.LONG
        }
        when (methodName) {
            "inv" -> "this.to${typeToCast.capitalized}().inv().to${type.capitalized}()"
            else -> {
                "(this.to${typeToCast.capitalized}() $methodName other.to${typeToCast.capitalized}()).to${type.capitalized}()"
            }
        }.addAsSingleLineBody()
    }

    override fun MethodBuilder.modifyGeneratedBitShiftOperators() {
        annotations += "kotlin.internal.IntrinsicConstEvaluation"

        if (methodName == "shr") {
            when (type) {
                UnsignedType.UBYTE, UnsignedType.USHORT -> "this.toUInt().shr(bitCount).to${type.capitalized}()"
                UnsignedType.UINT -> "this.toInt().ushr(bitCount).toUInt()"
                UnsignedType.ULONG -> "this.toLong().ushr(bitCount).toULong()"
            }.addAsSingleLineBody()
        } else {
            "this.to${type.asSigned.capitalized}().$methodName(bitCount).to${type.capitalized}()".addAsSingleLineBody()
        }
    }

    override fun MethodBuilder.modifyGeneratedUnaryOperation() {
        val cast = if (type == UnsignedType.UINT || type == UnsignedType.ULONG) "" else ".to${type.capitalized}()"

        when (methodName) {
            "inc" -> "this.plus(1u)$cast".addAsSingleLineBody()
            "dec" -> "this.minus(1u)$cast".addAsSingleLineBody()
        }
    }

    override fun MethodBuilder.modifyGeneratedRangeTo() {
        annotations.clear()
        modifySignature { isInline = false }
    }

    override fun MethodBuilder.modifyGeneratedCompareTo(otherType: UnsignedType) {
        annotations += "kotlin.internal.IntrinsicConstEvaluation"

        if (otherType == type) {
            val body = when (type) {
                UnsignedType.UBYTE -> "wasm_u32_compareTo(this.toInt(), $parameterName.toInt())"
                UnsignedType.USHORT -> "this.toInt().compareTo($parameterName.toInt())"
                UnsignedType.UINT, UnsignedType.ULONG -> "wasm_${type.prefixLowercase}_compareTo(this.to${type.asSigned.capitalized}(), $parameterName.to${type.asSigned.capitalized}())"
            }
            body.addAsSingleLineBody(bodyOnNewLine = true)
            return
        }

        "this${type.castToIfNecessary(otherType)}.compareTo($parameterName${otherType.castToIfNecessary(type)})".addAsSingleLineBody(bodyOnNewLine = true)
    }

    private fun ClassBuilder.generateHashCode() {
        method {
            signature {
                isOverride = true
                methodName = "hashCode"
                returnType = PrimitiveType.INT.capitalized
            }

            when (type) {
                UnsignedType.ULONG -> "((this shr 32) xor this).toInt()"
                else -> "this.toInt()"
            }.addAsSingleLineBody()
        }
    }

    override fun MethodBuilder.modifyGeneratedExtensionConversion(fromType: PrimitiveType) {
        when (fromType) {
            PrimitiveType.FLOAT, PrimitiveType.DOUBLE -> {
                isInline = false
                "wasm_${type.asSigned.prefixLowercase}_trunc_sat_${fromType.prefixLowercase}_u(this).to${type.capitalized}()"
            }
            PrimitiveType.BYTE, PrimitiveType.SHORT, PrimitiveType.INT -> when (type) {
                UnsignedType.UINT -> {
                    isInline = false
                    annotations += "WasmNoOpCast"
                    "implementedAsIntrinsic"
                }
                else -> "toUInt().to${type.capitalized}()"
            }
            PrimitiveType.LONG -> when (type) {
                UnsignedType.ULONG -> {
                    isInline = false
                    annotations += "WasmNoOpCast"
                    "implementedAsIntrinsic"
                }
                else -> "toULong().to${type.capitalized}()"
            }
            else -> error("Unexpected primitive type")
        }.addAsSingleLineBody()
    }

    override fun MethodBuilder.modifyGeneratedToStringHashCode() {
        when (type) {
            UnsignedType.UBYTE, UnsignedType.USHORT -> "toUInt().toString()".addAsSingleLineBody()
            UnsignedType.UINT -> "utoa32(this, 10)".addAsSingleLineBody()
            UnsignedType.ULONG -> "utoa64(this, 10)".addAsSingleLineBody()
        }
    }

    private fun ClassBuilder.generateEquals() {
        method {
            annotations += "kotlin.internal.IntrinsicConstEvaluation"

            signature {
                isOverride = true
                methodName = "equals"
                parameter {
                    name = "other"
                    type = "Any?"
                }
                returnType = "Boolean"
            }

            val additionalCheck = when (type) {
                UnsignedType.ULONG -> "wasm_i64_eq(this.toLong(), $parameterName.toLong())"
                UnsignedType.UINT -> "wasm_i32_eq(this.reinterpretAsInt(), $parameterName.reinterpretAsInt())"
                else -> "wasm_i32_eq(this.toInt(), $parameterName.toInt())"
            }

            "$parameterName is ${type.capitalized} && $additionalCheck".addAsSingleLineBody(bodyOnNewLine = true)
        }
    }

    private fun ClassBuilder.generateCustomEquals() {
        method {
            annotations += listOf(
                "kotlin.internal.IntrinsicConstEvaluation",
                "WasmOp(WasmOp.${type.asSigned.prefixUppercase}_EQ)"
            )

            signature {
                methodName = "equals"
                parameter {
                    name = "other"
                    type = this@WasmSingleUnsignedGenerator.type.capitalized
                }
                returnType = PrimitiveType.BOOLEAN.capitalized
            }

            "implementedAsIntrinsic".addAsSingleLineBody()
        }
    }

    private fun ClassBuilder.generateReinterpret(type: String) {
        method {
            annotations += "WasmNoOpCast"
            annotations += "PublishedApi"
            signature {
                visibility = MethodVisibility.INTERNAL
                methodName = "reinterpretAs$type"
                returnType = type
            }
            "implementedAsIntrinsic".addAsSingleLineBody(bodyOnNewLine = true)
        }
    }

    private var MethodBuilder.isInline: Boolean
        get() = inline
        set(shouldInline: Boolean) {
            modifySignature { isInline = shouldInline }
            val inlineOnlyAnnotation = "kotlin.internal.InlineOnly"
            if (shouldInline) {
                annotations.add(inlineOnlyAnnotation)
            } else {
                annotations.remove(inlineOnlyAnnotation)
            }
        }

    private val UnsignedType.prefixUppercase: String
        get() = when (this) {
            UnsignedType.UBYTE, UnsignedType.USHORT, UnsignedType.UINT -> "U32"
            UnsignedType.ULONG -> "U64"
        }

    private val PrimitiveType.prefixUppercase: String
        get() = when (this) {
            PrimitiveType.BYTE, PrimitiveType.SHORT, PrimitiveType.INT -> "I32"
            PrimitiveType.LONG -> "I64"
            PrimitiveType.FLOAT -> "F32"
            PrimitiveType.DOUBLE -> "F64"
            else -> error("Unexpected primitive type")
        }

    private val UnsignedType.prefixLowercase: String
        get() = prefixUppercase.lowercase()

    private val PrimitiveType.prefixLowercase: String
        get() = prefixUppercase.lowercase()
}
