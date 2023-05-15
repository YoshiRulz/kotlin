// SKIP_KLIB_TEST
// IGNORE_BACKEND_K1: JS_IR
// WITH_STDLIB
// LANGUAGE: +ValueClasses

// MUTE_SIGNATURE_COMPARISON_K2: JS_IR
// ^ KT-57818

import kotlin.jvm.JvmInline

@JvmInline
value class Z(val s: String)

val equals = Z::equals
