// FIR_IDENTICAL
// WITH_STDLIB

// MUTE_SIGNATURE_COMPARISON_K2: JS_IR
// ^ KT-57818

val testSimpleUIntLiteral = 1u

val testSimpleUIntLiteralWithOverflow = 0xFFFF_FFFFu

val testUByteWithExpectedType: UByte = 1u

val testUShortWithExpectedType: UShort = 1u

val testUIntWithExpectedType: UInt = 1u

val testULongWithExpectedType: ULong = 1u

val testToUByte = 1.toUByte()

val testToUShort = 1.toUShort()

val testToUInt = 1.toUInt()

val testToULong = 1.toULong()
