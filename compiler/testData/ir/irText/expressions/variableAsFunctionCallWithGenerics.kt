// FIR_IDENTICAL

// MUTE_SIGNATURE_COMPARISON_K2: JS_IR
// ^ KT-57818

val <T : CharSequence> T.gk: () -> T
    get() = { -> this }

fun testGeneric1(x: String) = x.gk()

val <T> T.kt26531Val: () -> T
    get() = fun () = this

fun kt26531() = 7.kt26531Val()
