/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.wasm.internal

//
// NOTE: THIS FILE IS AUTO-GENERATED by the generators/wasm/WasmIntrinsicGenerator.kt
//

@ExcludedFromCodegen
@Suppress("unused")
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
internal annotation class WasmOp(val name: String) {
    companion object {
        const val I32_EQZ = "I32_EQZ"
        const val I64_EQZ = "I64_EQZ"
        const val I32_CLZ = "I32_CLZ"
        const val I32_CTZ = "I32_CTZ"
        const val I32_POPCNT = "I32_POPCNT"
        const val I64_CLZ = "I64_CLZ"
        const val I64_CTZ = "I64_CTZ"
        const val I64_POPCNT = "I64_POPCNT"
        const val F32_ABS = "F32_ABS"
        const val F32_NEG = "F32_NEG"
        const val F32_CEIL = "F32_CEIL"
        const val F32_FLOOR = "F32_FLOOR"
        const val F32_TRUNC = "F32_TRUNC"
        const val F32_NEAREST = "F32_NEAREST"
        const val F32_SQRT = "F32_SQRT"
        const val F64_ABS = "F64_ABS"
        const val F64_NEG = "F64_NEG"
        const val F64_CEIL = "F64_CEIL"
        const val F64_FLOOR = "F64_FLOOR"
        const val F64_TRUNC = "F64_TRUNC"
        const val F64_NEAREST = "F64_NEAREST"
        const val F64_SQRT = "F64_SQRT"
        const val I32_WRAP_I64 = "I32_WRAP_I64"
        const val I32_TRUNC_F32_S = "I32_TRUNC_F32_S"
        const val I32_TRUNC_F32_U = "I32_TRUNC_F32_U"
        const val I32_TRUNC_F64_S = "I32_TRUNC_F64_S"
        const val I32_TRUNC_F64_U = "I32_TRUNC_F64_U"
        const val I64_EXTEND_I32_S = "I64_EXTEND_I32_S"
        const val I64_EXTEND_I32_U = "I64_EXTEND_I32_U"
        const val I64_TRUNC_F32_S = "I64_TRUNC_F32_S"
        const val I64_TRUNC_F32_U = "I64_TRUNC_F32_U"
        const val I64_TRUNC_F64_S = "I64_TRUNC_F64_S"
        const val I64_TRUNC_F64_U = "I64_TRUNC_F64_U"
        const val F32_CONVERT_I32_S = "F32_CONVERT_I32_S"
        const val F32_CONVERT_I32_U = "F32_CONVERT_I32_U"
        const val F32_CONVERT_I64_S = "F32_CONVERT_I64_S"
        const val F32_CONVERT_I64_U = "F32_CONVERT_I64_U"
        const val F32_DEMOTE_F64 = "F32_DEMOTE_F64"
        const val F64_CONVERT_I32_S = "F64_CONVERT_I32_S"
        const val F64_CONVERT_I32_U = "F64_CONVERT_I32_U"
        const val F64_CONVERT_I64_S = "F64_CONVERT_I64_S"
        const val F64_CONVERT_I64_U = "F64_CONVERT_I64_U"
        const val F64_PROMOTE_F32 = "F64_PROMOTE_F32"
        const val I32_REINTERPRET_F32 = "I32_REINTERPRET_F32"
        const val I64_REINTERPRET_F64 = "I64_REINTERPRET_F64"
        const val F32_REINTERPRET_I32 = "F32_REINTERPRET_I32"
        const val F64_REINTERPRET_I64 = "F64_REINTERPRET_I64"
        const val I32_EXTEND8_S = "I32_EXTEND8_S"
        const val I32_EXTEND16_S = "I32_EXTEND16_S"
        const val I64_EXTEND8_S = "I64_EXTEND8_S"
        const val I64_EXTEND16_S = "I64_EXTEND16_S"
        const val I64_EXTEND32_S = "I64_EXTEND32_S"
        const val I32_TRUNC_SAT_F32_S = "I32_TRUNC_SAT_F32_S"
        const val I32_TRUNC_SAT_F32_U = "I32_TRUNC_SAT_F32_U"
        const val I32_TRUNC_SAT_F64_S = "I32_TRUNC_SAT_F64_S"
        const val I32_TRUNC_SAT_F64_U = "I32_TRUNC_SAT_F64_U"
        const val I64_TRUNC_SAT_F32_S = "I64_TRUNC_SAT_F32_S"
        const val I64_TRUNC_SAT_F32_U = "I64_TRUNC_SAT_F32_U"
        const val I64_TRUNC_SAT_F64_S = "I64_TRUNC_SAT_F64_S"
        const val I64_TRUNC_SAT_F64_U = "I64_TRUNC_SAT_F64_U"
        const val I32_EQ = "I32_EQ"
        const val I32_NE = "I32_NE"
        const val I32_LT_S = "I32_LT_S"
        const val I32_LT_U = "I32_LT_U"
        const val I32_GT_S = "I32_GT_S"
        const val I32_GT_U = "I32_GT_U"
        const val I32_LE_S = "I32_LE_S"
        const val I32_LE_U = "I32_LE_U"
        const val I32_GE_S = "I32_GE_S"
        const val I32_GE_U = "I32_GE_U"
        const val I64_EQ = "I64_EQ"
        const val I64_NE = "I64_NE"
        const val I64_LT_S = "I64_LT_S"
        const val I64_LT_U = "I64_LT_U"
        const val I64_GT_S = "I64_GT_S"
        const val I64_GT_U = "I64_GT_U"
        const val I64_LE_S = "I64_LE_S"
        const val I64_LE_U = "I64_LE_U"
        const val I64_GE_S = "I64_GE_S"
        const val I64_GE_U = "I64_GE_U"
        const val F32_EQ = "F32_EQ"
        const val F32_NE = "F32_NE"
        const val F32_LT = "F32_LT"
        const val F32_GT = "F32_GT"
        const val F32_LE = "F32_LE"
        const val F32_GE = "F32_GE"
        const val F64_EQ = "F64_EQ"
        const val F64_NE = "F64_NE"
        const val F64_LT = "F64_LT"
        const val F64_GT = "F64_GT"
        const val F64_LE = "F64_LE"
        const val F64_GE = "F64_GE"
        const val I32_ADD = "I32_ADD"
        const val I32_SUB = "I32_SUB"
        const val I32_MUL = "I32_MUL"
        const val I32_DIV_S = "I32_DIV_S"
        const val I32_DIV_U = "I32_DIV_U"
        const val I32_REM_S = "I32_REM_S"
        const val I32_REM_U = "I32_REM_U"
        const val I32_AND = "I32_AND"
        const val I32_OR = "I32_OR"
        const val I32_XOR = "I32_XOR"
        const val I32_SHL = "I32_SHL"
        const val I32_SHR_S = "I32_SHR_S"
        const val I32_SHR_U = "I32_SHR_U"
        const val I32_ROTL = "I32_ROTL"
        const val I32_ROTR = "I32_ROTR"
        const val I64_ADD = "I64_ADD"
        const val I64_SUB = "I64_SUB"
        const val I64_MUL = "I64_MUL"
        const val I64_DIV_S = "I64_DIV_S"
        const val I64_DIV_U = "I64_DIV_U"
        const val I64_REM_S = "I64_REM_S"
        const val I64_REM_U = "I64_REM_U"
        const val I64_AND = "I64_AND"
        const val I64_OR = "I64_OR"
        const val I64_XOR = "I64_XOR"
        const val I64_SHL = "I64_SHL"
        const val I64_SHR_S = "I64_SHR_S"
        const val I64_SHR_U = "I64_SHR_U"
        const val I64_ROTL = "I64_ROTL"
        const val I64_ROTR = "I64_ROTR"
        const val F32_ADD = "F32_ADD"
        const val F32_SUB = "F32_SUB"
        const val F32_MUL = "F32_MUL"
        const val F32_DIV = "F32_DIV"
        const val F32_MIN = "F32_MIN"
        const val F32_MAX = "F32_MAX"
        const val F32_COPYSIGN = "F32_COPYSIGN"
        const val F64_ADD = "F64_ADD"
        const val F64_SUB = "F64_SUB"
        const val F64_MUL = "F64_MUL"
        const val F64_DIV = "F64_DIV"
        const val F64_MIN = "F64_MIN"
        const val F64_MAX = "F64_MAX"
        const val F64_COPYSIGN = "F64_COPYSIGN"
        const val I32_CONST = "I32_CONST"
        const val I64_CONST = "I64_CONST"
        const val F32_CONST = "F32_CONST"
        const val F64_CONST = "F64_CONST"
        const val I32_LOAD = "I32_LOAD"
        const val I64_LOAD = "I64_LOAD"
        const val F32_LOAD = "F32_LOAD"
        const val F64_LOAD = "F64_LOAD"
        const val I32_LOAD8_S = "I32_LOAD8_S"
        const val I32_LOAD8_U = "I32_LOAD8_U"
        const val I32_LOAD16_S = "I32_LOAD16_S"
        const val I32_LOAD16_U = "I32_LOAD16_U"
        const val I64_LOAD8_S = "I64_LOAD8_S"
        const val I64_LOAD8_U = "I64_LOAD8_U"
        const val I64_LOAD16_S = "I64_LOAD16_S"
        const val I64_LOAD16_U = "I64_LOAD16_U"
        const val I64_LOAD32_S = "I64_LOAD32_S"
        const val I64_LOAD32_U = "I64_LOAD32_U"
        const val I32_STORE = "I32_STORE"
        const val I64_STORE = "I64_STORE"
        const val F32_STORE = "F32_STORE"
        const val F64_STORE = "F64_STORE"
        const val I32_STORE8 = "I32_STORE8"
        const val I32_STORE16 = "I32_STORE16"
        const val I64_STORE8 = "I64_STORE8"
        const val I64_STORE16 = "I64_STORE16"
        const val I64_STORE32 = "I64_STORE32"
        const val MEMORY_SIZE = "MEMORY_SIZE"
        const val MEMORY_GROW = "MEMORY_GROW"
        const val MEMORY_INIT = "MEMORY_INIT"
        const val DATA_DROP = "DATA_DROP"
        const val MEMORY_COPY = "MEMORY_COPY"
        const val MEMORY_FILL = "MEMORY_FILL"
        const val TABLE_GET = "TABLE_GET"
        const val TABLE_SET = "TABLE_SET"
        const val TABLE_GROW = "TABLE_GROW"
        const val TABLE_SIZE = "TABLE_SIZE"
        const val TABLE_FILL = "TABLE_FILL"
        const val TABLE_INIT = "TABLE_INIT"
        const val ELEM_DROP = "ELEM_DROP"
        const val TABLE_COPY = "TABLE_COPY"
        const val UNREACHABLE = "UNREACHABLE"
        const val NOP = "NOP"
        const val BLOCK = "BLOCK"
        const val LOOP = "LOOP"
        const val IF = "IF"
        const val ELSE = "ELSE"
        const val END = "END"
        const val BR = "BR"
        const val BR_IF = "BR_IF"
        const val BR_TABLE = "BR_TABLE"
        const val RETURN = "RETURN"
        const val CALL = "CALL"
        const val CALL_INDIRECT = "CALL_INDIRECT"
        const val TRY = "TRY"
        const val CATCH = "CATCH"
        const val CATCH_ALL = "CATCH_ALL"
        const val DELEGATE = "DELEGATE"
        const val THROW = "THROW"
        const val RETHROW = "RETHROW"
        const val DROP = "DROP"
        const val SELECT = "SELECT"
        const val SELECT_TYPED = "SELECT_TYPED"
        const val LOCAL_GET = "LOCAL_GET"
        const val LOCAL_SET = "LOCAL_SET"
        const val LOCAL_TEE = "LOCAL_TEE"
        const val GLOBAL_GET = "GLOBAL_GET"
        const val GLOBAL_SET = "GLOBAL_SET"
        const val REF_NULL = "REF_NULL"
        const val REF_IS_NULL = "REF_IS_NULL"
        const val REF_FUNC = "REF_FUNC"
        const val CALL_REF = "CALL_REF"
        const val RETURN_CALL_REF = "RETURN_CALL_REF"
        const val REF_AS_NOT_NULL = "REF_AS_NOT_NULL"
        const val BR_ON_NULL = "BR_ON_NULL"
        const val BR_ON_NON_NULL = "BR_ON_NON_NULL"
        const val STRUCT_NEW = "STRUCT_NEW"
        const val STRUCT_NEW_DEFAULT = "STRUCT_NEW_DEFAULT"
        const val STRUCT_GET = "STRUCT_GET"
        const val STRUCT_GET_S = "STRUCT_GET_S"
        const val STRUCT_GET_U = "STRUCT_GET_U"
        const val STRUCT_SET = "STRUCT_SET"
        const val ARRAY_NEW = "ARRAY_NEW"
        const val ARRAY_NEW_DEFAULT = "ARRAY_NEW_DEFAULT"
        const val ARRAY_GET = "ARRAY_GET"
        const val ARRAY_GET_S = "ARRAY_GET_S"
        const val ARRAY_GET_U = "ARRAY_GET_U"
        const val ARRAY_SET = "ARRAY_SET"
        const val ARRAY_LEN = "ARRAY_LEN"
        const val ARRAY_COPY = "ARRAY_COPY"
        const val ARRAY_NEW_DATA = "ARRAY_NEW_DATA"
        const val ARRAY_NEW_FIXED = "ARRAY_NEW_FIXED"
        const val I31_NEW = "I31_NEW"
        const val I31_GET_S = "I31_GET_S"
        const val I31_GET_U = "I31_GET_U"
        const val REF_EQ = "REF_EQ"
        const val REF_TEST = "REF_TEST"
        const val REF_TEST_NULL = "REF_TEST_NULL"
        const val REF_CAST = "REF_CAST"
        const val REF_CAST_NULL = "REF_CAST_NULL"
        const val BR_ON_CAST = "BR_ON_CAST"
        const val BR_ON_CAST_FAIL = "BR_ON_CAST_FAIL"
        const val EXTERN_INTERNALIZE = "EXTERN_INTERNALIZE"
        const val EXTERN_EXTERNALIZE = "EXTERN_EXTERNALIZE"
        const val STRING_CONST = "STRING_CONST"
        const val STRING_CONCAT = "STRING_CONCAT"
        const val STRING_EQ = "STRING_EQ"
        const val STRING_AS_ITER = "STRING_AS_ITER"
        const val STRING_NEW_WTF16_ARRAY = "STRING_NEW_WTF16_ARRAY"
        const val STRING_MEASURE_UTF8 = "STRING_MEASURE_UTF8"
        const val STRING_MEASURE_WTF8 = "STRING_MEASURE_WTF8"
        const val STRING_MEASURE_WTF16 = "STRING_MEASURE_WTF16"
        const val STRING_ENCODE_WTF16_ARRAY = "STRING_ENCODE_WTF16_ARRAY"
        const val STRING_NEW_LOSSY_UTF8_ARRAY = "STRING_NEW_LOSSY_UTF8_ARRAY"
        const val STRING_ENCODE_LOSSY_UTF8_ARRAY = "STRING_ENCODE_LOSSY_UTF8_ARRAY"
        const val STRINGVIEW_ITER_NEXT = "STRINGVIEW_ITER_NEXT"
        const val STRINGVIEW_ITER_ADVANCE = "STRINGVIEW_ITER_ADVANCE"
        const val STRINGVIEW_ITER_REWIND = "STRINGVIEW_ITER_REWIND"
        const val STRINGVIEW_ITER_SLICE = "STRINGVIEW_ITER_SLICE"
        const val STRINGVIEW_AS_WTF16 = "STRINGVIEW_AS_WTF16"
        const val STRINGVIEW_WTF16_LENGTH = "STRINGVIEW_WTF16_LENGTH"
        const val STRINGVIEW_WTF16_GET_CODEUNIT = "STRINGVIEW_WTF16_GET_CODEUNIT"
        const val STRINGVIEW_WTF16_SLICE = "STRINGVIEW_WTF16_SLICE"
        const val GET_UNIT = "GET_UNIT"
        const val PSEUDO_COMMENT_PREVIOUS_INSTR = "PSEUDO_COMMENT_PREVIOUS_INSTR"
        const val PSEUDO_COMMENT_GROUP_START = "PSEUDO_COMMENT_GROUP_START"
        const val PSEUDO_COMMENT_GROUP_END = "PSEUDO_COMMENT_GROUP_END"
    }
}
