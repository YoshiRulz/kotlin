// MUTE_SIGNATURE_COMPARISON_K2: JS_IR
// ^ KT-57818
data class A(val x: Int, val y: Int)

var fn: (A) -> Int = { (_, y) -> 42 + y }
