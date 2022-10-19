
// IGNORE_BACKEND: JVM
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +SealedInlineClasses

// !JVM_DEFAULT_MODE: all
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
interface I {
    fun str(): String = "I"
}
OPTIONAL_JVM_INLINE_ANNOTATION
sealed value class I1: I

value object O1: I1()

OPTIONAL_JVM_INLINE_ANNOTATION
sealed value class I2: I1() {
    override fun str(): String = "I2"
}

value object O2: I2()

OPTIONAL_JVM_INLINE_ANNOTATION
value class I3(val value: String?): I2()


fun <T> boxValue(v: T): T = v

fun <T: I1> coerceToI1(v: T): I1 = v

fun <T: I2> coerceToI2(v: T): I2 = v



fun <T: I> coerceToI(v: T): I = v


fun box(): String {
    var res: String = ""
    res = O1.str()
    if (res != "I") return "FAIL 1: $res"

    res = boxValue(O1).str()
    if (res != "I") return "FAIL 2: $res"

    res = coerceToI1(O1).str()
    if (res != "I") return "FAIL 3: $res"

    res = coerceToI(O1).str()
    if (res != "I") return "FAIL 4: $res"

    res = O2.str()
    if (res != "I2") return "FAIL 5: $res"

    res = boxValue(O2).str()
    if (res != "I2") return "FAIL 6: $res"

    res = coerceToI2(O2).str()
    if (res != "I2") return "FAIL 7: $res"

    res = coerceToI1(O2).str()
    if (res != "I2") return "FAIL 8: $res"

    res = coerceToI(O2).str()
    if (res != "I2") return "FAIL 9: $res"

    val i3 = I3("A")
    res = i3.str()
    if (res != "I2") return "FAIL 10: $res"

    res = boxValue(i3).str()
    if (res != "I2") return "FAIL 11: $res"

    res = coerceToI2(i3).str()
    if (res != "I2") return "FAIL 12: $res"

    res = coerceToI1(i3).str()
    if (res != "I2") return "FAIL 13: $res"

    res = coerceToI(i3).str()
    if (res != "I2") return "FAIL 14: $res"

    return "OK"
}
