// FIR_IDENTICAL

// MUTE_SIGNATURE_COMPARISON_K2: JS_IR
// MUTE_SIGNATURE_COMPARISON_K2: NATIVE
// ^ KT-57818

const val I0 = 0
const val I1 = 1
const val I2 = I0 + I1 + I1

const val STR1 = "String1"
const val STR2 = "String" + "2"
const val STR3 = STR1 + STR2
const val STR4 = "$STR1$STR2"
