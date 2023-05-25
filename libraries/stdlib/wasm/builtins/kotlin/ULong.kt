/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// Auto-generated file. DO NOT EDIT!

@file:Suppress("OVERRIDE_BY_INLINE", "NOTHING_TO_INLINE", "unused", "UNUSED_PARAMETER")

package kotlin

import kotlin.wasm.internal.*

@SinceKotlin("1.5")
@WasExperimental(ExperimentalUnsignedTypes::class)
@WasmAutoboxed
public class ULong private constructor(private val value: ULong) : Comparable<ULong> {
    companion object {
        /**
         * A constant holding the minimum value an instance of ULong can have.
         */
        public const val MIN_VALUE: ULong = 0u

        /**
         * A constant holding the maximum value an instance of ULong can have.
         */
        public const val MAX_VALUE: ULong = 18446744073709551615u

        /**
         * The number of bytes used to represent an instance of ULong in a binary form.
         */
        public const val SIZE_BYTES: Int = 8

        /**
         * The number of bits used to represent an instance of ULong in a binary form.
         */
        public const val SIZE_BITS: Int = 64
    }

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline operator fun compareTo(other: UByte): Int =
        this.compareTo(other.toULong())

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline operator fun compareTo(other: UShort): Int =
        this.compareTo(other.toULong())

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline operator fun compareTo(other: UInt): Int =
        this.compareTo(other.toULong())

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    @kotlin.internal.InlineOnly
    @Suppress("OVERRIDE_BY_INLINE")
    @kotlin.internal.IntrinsicConstEvaluation
    public override inline operator fun compareTo(other: ULong): Int =
        wasm_u64_compareTo(this.toLong(), other.toLong())

    /** Adds the other value to this value. */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline operator fun plus(other: UByte): ULong = this.plus(other.toULong())

    /** Adds the other value to this value. */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline operator fun plus(other: UShort): ULong = this.plus(other.toULong())

    /** Adds the other value to this value. */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline operator fun plus(other: UInt): ULong = this.plus(other.toULong())

    /** Adds the other value to this value. */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline operator fun plus(other: ULong): ULong = this.toLong().plus(other.toLong()).toULong()

    /** Subtracts the other value from this value. */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline operator fun minus(other: UByte): ULong = this.minus(other.toULong())

    /** Subtracts the other value from this value. */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline operator fun minus(other: UShort): ULong = this.minus(other.toULong())

    /** Subtracts the other value from this value. */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline operator fun minus(other: UInt): ULong = this.minus(other.toULong())

    /** Subtracts the other value from this value. */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline operator fun minus(other: ULong): ULong = this.toLong().minus(other.toLong()).toULong()

    /** Multiplies this value by the other value. */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline operator fun times(other: UByte): ULong = this.times(other.toULong())

    /** Multiplies this value by the other value. */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline operator fun times(other: UShort): ULong = this.times(other.toULong())

    /** Multiplies this value by the other value. */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline operator fun times(other: UInt): ULong = this.times(other.toULong())

    /** Multiplies this value by the other value. */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline operator fun times(other: ULong): ULong = this.toLong().times(other.toLong()).toULong()

    /** Divides this value by the other value, truncating the result to an integer that is closer to zero. */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline operator fun div(other: UByte): ULong = this.div(other.toULong())

    /** Divides this value by the other value, truncating the result to an integer that is closer to zero. */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline operator fun div(other: UShort): ULong = this.div(other.toULong())

    /** Divides this value by the other value, truncating the result to an integer that is closer to zero. */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline operator fun div(other: UInt): ULong = this.div(other.toULong())

    /** Divides this value by the other value, truncating the result to an integer that is closer to zero. */
    @kotlin.internal.IntrinsicConstEvaluation
    @WasmOp(WasmOp.I64_DIV_U)
    public operator fun div(other: ULong): ULong = implementedAsIntrinsic

    /**
     * Calculates the remainder of truncating division of this value (dividend) by the other value (divisor).
     *
     * The result is always less than the divisor.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline operator fun rem(other: UByte): ULong = this.rem(other.toULong())

    /**
     * Calculates the remainder of truncating division of this value (dividend) by the other value (divisor).
     *
     * The result is always less than the divisor.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline operator fun rem(other: UShort): ULong = this.rem(other.toULong())

    /**
     * Calculates the remainder of truncating division of this value (dividend) by the other value (divisor).
     *
     * The result is always less than the divisor.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline operator fun rem(other: UInt): ULong = this.rem(other.toULong())

    /**
     * Calculates the remainder of truncating division of this value (dividend) by the other value (divisor).
     *
     * The result is always less than the divisor.
     */
    @kotlin.internal.IntrinsicConstEvaluation
    @WasmOp(WasmOp.I64_REM_U)
    public operator fun rem(other: ULong): ULong = implementedAsIntrinsic

    /**
     * Divides this value by the other value, flooring the result to an integer that is closer to negative infinity.
     *
     * For unsigned types, the results of flooring division and truncating division are the same.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline fun floorDiv(other: UByte): ULong = this.floorDiv(other.toULong())

    /**
     * Divides this value by the other value, flooring the result to an integer that is closer to negative infinity.
     *
     * For unsigned types, the results of flooring division and truncating division are the same.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline fun floorDiv(other: UShort): ULong = this.floorDiv(other.toULong())

    /**
     * Divides this value by the other value, flooring the result to an integer that is closer to negative infinity.
     *
     * For unsigned types, the results of flooring division and truncating division are the same.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline fun floorDiv(other: UInt): ULong = this.floorDiv(other.toULong())

    /**
     * Divides this value by the other value, flooring the result to an integer that is closer to negative infinity.
     *
     * For unsigned types, the results of flooring division and truncating division are the same.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline fun floorDiv(other: ULong): ULong = div(other)

    /**
     * Calculates the remainder of flooring division of this value (dividend) by the other value (divisor).
     *
     * The result is always less than the divisor.
     *
     * For unsigned types, the remainders of flooring division and truncating division are the same.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline fun mod(other: UByte): UByte = this.mod(other.toULong()).toUByte()

    /**
     * Calculates the remainder of flooring division of this value (dividend) by the other value (divisor).
     *
     * The result is always less than the divisor.
     *
     * For unsigned types, the remainders of flooring division and truncating division are the same.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline fun mod(other: UShort): UShort = this.mod(other.toULong()).toUShort()

    /**
     * Calculates the remainder of flooring division of this value (dividend) by the other value (divisor).
     *
     * The result is always less than the divisor.
     *
     * For unsigned types, the remainders of flooring division and truncating division are the same.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline fun mod(other: UInt): UInt = this.mod(other.toULong()).toUInt()

    /**
     * Calculates the remainder of flooring division of this value (dividend) by the other value (divisor).
     *
     * The result is always less than the divisor.
     *
     * For unsigned types, the remainders of flooring division and truncating division are the same.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline fun mod(other: ULong): ULong = rem(other)

    /**
     * Returns this value incremented by one.
     *
     * @sample samples.misc.Builtins.inc
     */
    @kotlin.internal.InlineOnly
    public inline operator fun inc(): ULong = this.plus(1u)

    /**
     * Returns this value decremented by one.
     *
     * @sample samples.misc.Builtins.dec
     */
    @kotlin.internal.InlineOnly
    public inline operator fun dec(): ULong = this.minus(1u)

    /** Creates a range from this value to the specified [other] value. */
    public operator fun rangeTo(other: ULong): ULongRange = ULongRange(this, other)

    /**
     * Creates a range from this value up to but excluding the specified [other] value.
     *
     * If the [other] value is less than or equal to `this` value, then the returned range is empty.
     */
    @SinceKotlin("1.9")
    @WasExperimental(ExperimentalStdlibApi::class)
    @kotlin.internal.InlineOnly
    public inline operator fun rangeUntil(other: ULong): ULongRange = this until other

    /**
     * Shifts this value left by the [bitCount] number of bits.
     *
     * Note that only the six lowest-order bits of the [bitCount] are used as the shift distance.
     * The shift distance actually used is therefore always in the range `0..63`.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline infix fun shl(bitCount: Int): ULong = this.toLong().shl(bitCount).toULong()

    /**
     * Shifts this value right by the [bitCount] number of bits, filling the leftmost bits with zeros.
     *
     * Note that only the six lowest-order bits of the [bitCount] are used as the shift distance.
     * The shift distance actually used is therefore always in the range `0..63`.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline infix fun shr(bitCount: Int): ULong = this.toLong().ushr(bitCount).toULong()

    /** Performs a bitwise AND operation between the two values. */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline infix fun and(other: ULong): ULong = (this.toLong() and other.toLong()).toULong()

    /** Performs a bitwise OR operation between the two values. */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline infix fun or(other: ULong): ULong = (this.toLong() or other.toLong()).toULong()

    /** Performs a bitwise XOR operation between the two values. */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline infix fun xor(other: ULong): ULong = (this.toLong() xor other.toLong()).toULong()

    /** Inverts the bits in this value. */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline fun inv(): ULong = this.toLong().inv().toULong()

    /**
     * Converts this [ULong] value to [Byte].
     *
     * If this value is less than or equals to [Byte.MAX_VALUE], the resulting `Byte` value represents
     * the same numerical value as this `ULong`.
     *
     * The resulting `Byte` value is represented by the least significant 8 bits of this `ULong` value.
     * Note that the resulting `Byte` value may be negative.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline fun toByte(): Byte = this.toInt().toByte()

    /**
     * Converts this [ULong] value to [Short].
     *
     * If this value is less than or equals to [Short.MAX_VALUE], the resulting `Short` value represents
     * the same numerical value as this `ULong`.
     *
     * The resulting `Short` value is represented by the least significant 16 bits of this `ULong` value.
     * Note that the resulting `Short` value may be negative.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline fun toShort(): Short = this.toInt().toShort()

    /**
     * Converts this [ULong] value to [Int].
     *
     * If this value is less than or equals to [Int.MAX_VALUE], the resulting `Int` value represents
     * the same numerical value as this `ULong`.
     *
     * The resulting `Int` value is represented by the least significant 32 bits of this `ULong` value.
     * Note that the resulting `Int` value may be negative.
     */
    @kotlin.internal.IntrinsicConstEvaluation
    public fun toInt(): Int = wasm_i32_wrap_i64(this.toLong())

    /**
     * Converts this [ULong] value to [Long].
     *
     * If this value is less than or equals to [Long.MAX_VALUE], the resulting `Long` value represents
     * the same numerical value as this `ULong`. Otherwise the result is negative.
     *
     * The resulting `Long` value has the same binary representation as this `ULong` value.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline fun toLong(): Long = reinterpretAsLong()

    /**
     * Converts this [ULong] value to [UByte].
     *
     * If this value is less than or equals to [UByte.MAX_VALUE], the resulting `UByte` value represents
     * the same numerical value as this `ULong`.
     *
     * The resulting `UByte` value is represented by the least significant 8 bits of this `ULong` value.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline fun toUByte(): UByte = this.toUInt().toUByte()

    /**
     * Converts this [ULong] value to [UShort].
     *
     * If this value is less than or equals to [UShort.MAX_VALUE], the resulting `UShort` value represents
     * the same numerical value as this `ULong`.
     *
     * The resulting `UShort` value is represented by the least significant 16 bits of this `ULong` value.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline fun toUShort(): UShort = this.toUInt().toUShort()

    /**
     * Converts this [ULong] value to [UInt].
     *
     * If this value is less than or equals to [UInt.MAX_VALUE], the resulting `UInt` value represents
     * the same numerical value as this `ULong`.
     *
     * The resulting `UInt` value is represented by the least significant 32 bits of this `ULong` value.
     */
    @kotlin.internal.IntrinsicConstEvaluation
    public fun toUInt(): UInt = wasm_i32_wrap_i64(this.toLong()).toUInt()

    /** Returns this value. */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline fun toULong(): ULong = this

    /**
     * Converts this [ULong] value to [Float].
     *
     * The resulting value is the closest `Float` to this `ULong` value.
     * In case when this `ULong` value is exactly between two `Float`s,
     * the one with zero at least significant bit of mantissa is selected.
     */
    @kotlin.internal.IntrinsicConstEvaluation
    public fun toFloat(): Float = wasm_f32_convert_i64_u(this.toLong())

    /**
     * Converts this [ULong] value to [Double].
     *
     * The resulting value is the closest `Double` to this `ULong` value.
     * In case when this `ULong` value is exactly between two `Double`s,
     * the one with zero at least significant bit of mantissa is selected.
     */
    @kotlin.internal.IntrinsicConstEvaluation
    public fun toDouble(): Double = wasm_f64_convert_i64_u(this.toLong())

    public override fun toString(): String = utoa64(this, 10)

    public override fun hashCode(): Int = ((this shr 32) xor this).toInt()

    @kotlin.internal.IntrinsicConstEvaluation
    public override fun equals(other: Any?): Boolean =
        other is ULong && wasm_i64_eq(this.toLong(), other.toLong())

    @kotlin.internal.IntrinsicConstEvaluation
    @WasmOp(WasmOp.I64_EQ)
    public fun equals(other: ULong): Boolean = implementedAsIntrinsic

    @PublishedApi
    @WasmNoOpCast
    internal fun reinterpretAsLong(): Long = implementedAsIntrinsic
}
/**
 * Converts this [Byte] value to [ULong].
 *
 * If this value is positive, the resulting `ULong` value represents the same numerical value as this `Byte`.
 *
 * The least significant 8 bits of the resulting `ULong` value are the same as the bits of this `Byte` value,
 * whereas the most significant 56 bits are filled with the sign bit of this value.
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalUnsignedTypes::class)
@kotlin.internal.InlineOnly
public inline fun Byte.toULong(): ULong = toUInt().toULong()
/**
 * Converts this [Short] value to [ULong].
 *
 * If this value is positive, the resulting `ULong` value represents the same numerical value as this `Short`.
 *
 * The least significant 16 bits of the resulting `ULong` value are the same as the bits of this `Short` value,
 * whereas the most significant 48 bits are filled with the sign bit of this value.
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalUnsignedTypes::class)
@kotlin.internal.InlineOnly
public inline fun Short.toULong(): ULong = toUInt().toULong()
/**
 * Converts this [Int] value to [ULong].
 *
 * If this value is positive, the resulting `ULong` value represents the same numerical value as this `Int`.
 *
 * The least significant 32 bits of the resulting `ULong` value are the same as the bits of this `Int` value,
 * whereas the most significant 32 bits are filled with the sign bit of this value.
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalUnsignedTypes::class)
@kotlin.internal.InlineOnly
public inline fun Int.toULong(): ULong = toUInt().toULong()
/**
 * Converts this [Long] value to [ULong].
 *
 * If this value is positive, the resulting `ULong` value represents the same numerical value as this `Long`.
 *
 * The resulting `ULong` value has the same binary representation as this `Long` value.
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalUnsignedTypes::class)
@WasmNoOpCast
public fun Long.toULong(): ULong = implementedAsIntrinsic
/**
 * Converts this [Float] value to [ULong].
 *
 * The fractional part, if any, is rounded down towards zero.
 * Returns zero if this `Float` value is negative or `NaN`, [ULong.MAX_VALUE] if it's bigger than `ULong.MAX_VALUE`.
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalUnsignedTypes::class)
public fun Float.toULong(): ULong = wasm_i64_trunc_sat_f32_u(this).toULong()
/**
 * Converts this [Double] value to [ULong].
 *
 * The fractional part, if any, is rounded down towards zero.
 * Returns zero if this `Double` value is negative or `NaN`, [ULong.MAX_VALUE] if it's bigger than `ULong.MAX_VALUE`.
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalUnsignedTypes::class)
public fun Double.toULong(): ULong = wasm_i64_trunc_sat_f64_u(this).toULong()
