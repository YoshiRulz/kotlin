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
public class UInt private constructor(private val value: UInt) : Comparable<UInt> {
    companion object {
        /**
         * A constant holding the minimum value an instance of UInt can have.
         */
        public const val MIN_VALUE: UInt = 0u

        /**
         * A constant holding the maximum value an instance of UInt can have.
         */
        public const val MAX_VALUE: UInt = 4294967295u

        /**
         * The number of bytes used to represent an instance of UInt in a binary form.
         */
        public const val SIZE_BYTES: Int = 4

        /**
         * The number of bits used to represent an instance of UInt in a binary form.
         */
        public const val SIZE_BITS: Int = 32
    }

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline operator fun compareTo(other: UByte): Int =
        this.compareTo(other.toUInt())

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline operator fun compareTo(other: UShort): Int =
        this.compareTo(other.toUInt())

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    @kotlin.internal.InlineOnly
    @Suppress("OVERRIDE_BY_INLINE")
    @kotlin.internal.IntrinsicConstEvaluation
    public override inline operator fun compareTo(other: UInt): Int =
        wasm_u32_compareTo(this.toInt(), other.toInt())

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline operator fun compareTo(other: ULong): Int =
        this.toULong().compareTo(other)

    /** Adds the other value to this value. */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline operator fun plus(other: UByte): UInt = this.plus(other.toUInt())

    /** Adds the other value to this value. */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline operator fun plus(other: UShort): UInt = this.plus(other.toUInt())

    /** Adds the other value to this value. */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline operator fun plus(other: UInt): UInt = this.toInt().plus(other.toInt()).toUInt()

    /** Adds the other value to this value. */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline operator fun plus(other: ULong): ULong = this.toULong().plus(other)

    /** Subtracts the other value from this value. */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline operator fun minus(other: UByte): UInt = this.minus(other.toUInt())

    /** Subtracts the other value from this value. */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline operator fun minus(other: UShort): UInt = this.minus(other.toUInt())

    /** Subtracts the other value from this value. */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline operator fun minus(other: UInt): UInt = this.toInt().minus(other.toInt()).toUInt()

    /** Subtracts the other value from this value. */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline operator fun minus(other: ULong): ULong = this.toULong().minus(other)

    /** Multiplies this value by the other value. */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline operator fun times(other: UByte): UInt = this.times(other.toUInt())

    /** Multiplies this value by the other value. */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline operator fun times(other: UShort): UInt = this.times(other.toUInt())

    /** Multiplies this value by the other value. */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline operator fun times(other: UInt): UInt = this.toInt().times(other.toInt()).toUInt()

    /** Multiplies this value by the other value. */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline operator fun times(other: ULong): ULong = this.toULong().times(other)

    /** Divides this value by the other value, truncating the result to an integer that is closer to zero. */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline operator fun div(other: UByte): UInt = this.div(other.toUInt())

    /** Divides this value by the other value, truncating the result to an integer that is closer to zero. */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline operator fun div(other: UShort): UInt = this.div(other.toUInt())

    /** Divides this value by the other value, truncating the result to an integer that is closer to zero. */
    @kotlin.internal.IntrinsicConstEvaluation
    @WasmOp(WasmOp.I32_DIV_U)
    public operator fun div(other: UInt): UInt = implementedAsIntrinsic

    /** Divides this value by the other value, truncating the result to an integer that is closer to zero. */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline operator fun div(other: ULong): ULong = this.toULong().div(other)

    /**
     * Calculates the remainder of truncating division of this value (dividend) by the other value (divisor).
     *
     * The result is always less than the divisor.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline operator fun rem(other: UByte): UInt = this.rem(other.toUInt())

    /**
     * Calculates the remainder of truncating division of this value (dividend) by the other value (divisor).
     *
     * The result is always less than the divisor.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline operator fun rem(other: UShort): UInt = this.rem(other.toUInt())

    /**
     * Calculates the remainder of truncating division of this value (dividend) by the other value (divisor).
     *
     * The result is always less than the divisor.
     */
    @kotlin.internal.IntrinsicConstEvaluation
    @WasmOp(WasmOp.I32_REM_U)
    public operator fun rem(other: UInt): UInt = implementedAsIntrinsic

    /**
     * Calculates the remainder of truncating division of this value (dividend) by the other value (divisor).
     *
     * The result is always less than the divisor.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline operator fun rem(other: ULong): ULong = this.toULong().rem(other)

    /**
     * Divides this value by the other value, flooring the result to an integer that is closer to negative infinity.
     *
     * For unsigned types, the results of flooring division and truncating division are the same.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline fun floorDiv(other: UByte): UInt = this.floorDiv(other.toUInt())

    /**
     * Divides this value by the other value, flooring the result to an integer that is closer to negative infinity.
     *
     * For unsigned types, the results of flooring division and truncating division are the same.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline fun floorDiv(other: UShort): UInt = this.floorDiv(other.toUInt())

    /**
     * Divides this value by the other value, flooring the result to an integer that is closer to negative infinity.
     *
     * For unsigned types, the results of flooring division and truncating division are the same.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline fun floorDiv(other: UInt): UInt = div(other)

    /**
     * Divides this value by the other value, flooring the result to an integer that is closer to negative infinity.
     *
     * For unsigned types, the results of flooring division and truncating division are the same.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline fun floorDiv(other: ULong): ULong = this.toULong().floorDiv(other)

    /**
     * Calculates the remainder of flooring division of this value (dividend) by the other value (divisor).
     *
     * The result is always less than the divisor.
     *
     * For unsigned types, the remainders of flooring division and truncating division are the same.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline fun mod(other: UByte): UByte = this.mod(other.toUInt()).toUByte()

    /**
     * Calculates the remainder of flooring division of this value (dividend) by the other value (divisor).
     *
     * The result is always less than the divisor.
     *
     * For unsigned types, the remainders of flooring division and truncating division are the same.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline fun mod(other: UShort): UShort = this.mod(other.toUInt()).toUShort()

    /**
     * Calculates the remainder of flooring division of this value (dividend) by the other value (divisor).
     *
     * The result is always less than the divisor.
     *
     * For unsigned types, the remainders of flooring division and truncating division are the same.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline fun mod(other: UInt): UInt = rem(other)

    /**
     * Calculates the remainder of flooring division of this value (dividend) by the other value (divisor).
     *
     * The result is always less than the divisor.
     *
     * For unsigned types, the remainders of flooring division and truncating division are the same.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline fun mod(other: ULong): ULong = this.toULong().mod(other)

    /**
     * Returns this value incremented by one.
     *
     * @sample samples.misc.Builtins.inc
     */
    @kotlin.internal.InlineOnly
    public inline operator fun inc(): UInt = this.plus(1u)

    /**
     * Returns this value decremented by one.
     *
     * @sample samples.misc.Builtins.dec
     */
    @kotlin.internal.InlineOnly
    public inline operator fun dec(): UInt = this.minus(1u)

    /** Creates a range from this value to the specified [other] value. */
    public operator fun rangeTo(other: UInt): UIntRange = UIntRange(this, other)

    /**
     * Creates a range from this value up to but excluding the specified [other] value.
     *
     * If the [other] value is less than or equal to `this` value, then the returned range is empty.
     */
    @SinceKotlin("1.9")
    @WasExperimental(ExperimentalStdlibApi::class)
    @kotlin.internal.InlineOnly
    public inline operator fun rangeUntil(other: UInt): UIntRange = this until other

    /**
     * Shifts this value left by the [bitCount] number of bits.
     *
     * Note that only the five lowest-order bits of the [bitCount] are used as the shift distance.
     * The shift distance actually used is therefore always in the range `0..31`.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline infix fun shl(bitCount: Int): UInt = this.toInt().shl(bitCount).toUInt()

    /**
     * Shifts this value right by the [bitCount] number of bits, filling the leftmost bits with zeros.
     *
     * Note that only the five lowest-order bits of the [bitCount] are used as the shift distance.
     * The shift distance actually used is therefore always in the range `0..31`.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline infix fun shr(bitCount: Int): UInt = this.toInt().ushr(bitCount).toUInt()

    /** Performs a bitwise AND operation between the two values. */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline infix fun and(other: UInt): UInt = (this.toInt() and other.toInt()).toUInt()

    /** Performs a bitwise OR operation between the two values. */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline infix fun or(other: UInt): UInt = (this.toInt() or other.toInt()).toUInt()

    /** Performs a bitwise XOR operation between the two values. */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline infix fun xor(other: UInt): UInt = (this.toInt() xor other.toInt()).toUInt()

    /** Inverts the bits in this value. */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline fun inv(): UInt = this.toInt().inv().toUInt()

    /**
     * Converts this [UInt] value to [Byte].
     *
     * If this value is less than or equals to [Byte.MAX_VALUE], the resulting `Byte` value represents
     * the same numerical value as this `UInt`.
     *
     * The resulting `Byte` value is represented by the least significant 8 bits of this `UInt` value.
     * Note that the resulting `Byte` value may be negative.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline fun toByte(): Byte = ((this shl 24) shr 24).toInt().reinterpretAsByte()

    /**
     * Converts this [UInt] value to [Short].
     *
     * If this value is less than or equals to [Short.MAX_VALUE], the resulting `Short` value represents
     * the same numerical value as this `UInt`.
     *
     * The resulting `Short` value is represented by the least significant 16 bits of this `UInt` value.
     * Note that the resulting `Short` value may be negative.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline fun toShort(): Short = ((this shl 16) shr 16).toInt().reinterpretAsShort()

    /**
     * Converts this [UInt] value to [Int].
     *
     * If this value is less than or equals to [Int.MAX_VALUE], the resulting `Int` value represents
     * the same numerical value as this `UInt`. Otherwise the result is negative.
     *
     * The resulting `Int` value has the same binary representation as this `UInt` value.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline fun toInt(): Int = reinterpretAsInt()

    /**
     * Converts this [UInt] value to [Long].
     *
     * The resulting `Long` value represents the same numerical value as this `UInt`.
     *
     * The least significant 32 bits of the resulting `Long` value are the same as the bits of this `UInt` value,
     * whereas the most significant 32 bits are filled with zeros.
     */
    @kotlin.internal.IntrinsicConstEvaluation
    public fun toLong(): Long = wasm_i64_extend_i32_u(this.toInt()).toLong()

    /**
     * Converts this [UInt] value to [UByte].
     *
     * If this value is less than or equals to [UByte.MAX_VALUE], the resulting `UByte` value represents
     * the same numerical value as this `UInt`.
     *
     * The resulting `UByte` value is represented by the least significant 8 bits of this `UInt` value.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline fun toUByte(): UByte = ((this shl 24) shr 24).reinterpretAsUByte()

    /**
     * Converts this [UInt] value to [UShort].
     *
     * If this value is less than or equals to [UShort.MAX_VALUE], the resulting `UShort` value represents
     * the same numerical value as this `UInt`.
     *
     * The resulting `UShort` value is represented by the least significant 16 bits of this `UInt` value.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline fun toUShort(): UShort = ((this shl 16) shr 16).reinterpretAsUShort()

    /** Returns this value. */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline fun toUInt(): UInt = this

    /**
     * Converts this [UInt] value to [ULong].
     *
     * The resulting `ULong` value represents the same numerical value as this `UInt`.
     *
     * The least significant 32 bits of the resulting `ULong` value are the same as the bits of this `UInt` value,
     * whereas the most significant 32 bits are filled with zeros.
     */
    @kotlin.internal.IntrinsicConstEvaluation
    public fun toULong(): ULong = wasm_i64_extend_i32_u(this.toInt())

    /**
     * Converts this [UInt] value to [Float].
     *
     * The resulting value is the closest `Float` to this `UInt` value.
     * In case when this `UInt` value is exactly between two `Float`s,
     * the one with zero at least significant bit of mantissa is selected.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public fun toFloat(): Float = wasm_f32_convert_i32_u(this.toInt())

    /**
     * Converts this [UInt] value to [Double].
     *
     * The resulting `Double` value represents the same numerical value as this `UInt`.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public fun toDouble(): Double = wasm_f64_convert_i32_u(this.toInt())

    public override fun toString(): String = utoa32(this, 10)

    public override fun hashCode(): Int = this.toInt()

    @kotlin.internal.IntrinsicConstEvaluation
    public override fun equals(other: Any?): Boolean =
        other is UInt && wasm_i32_eq(this.reinterpretAsInt(), other.reinterpretAsInt())

    @kotlin.internal.IntrinsicConstEvaluation
    @WasmOp(WasmOp.I32_EQ)
    public fun equals(other: UInt): Boolean = implementedAsIntrinsic

    @WasmNoOpCast
    @PublishedApi
    internal fun reinterpretAsUByte(): UByte =
        implementedAsIntrinsic

    @WasmNoOpCast
    @PublishedApi
    internal fun reinterpretAsUShort(): UShort =
        implementedAsIntrinsic

    @WasmNoOpCast
    @PublishedApi
    internal fun reinterpretAsInt(): Int =
        implementedAsIntrinsic
}
/**
 * Converts this [Byte] value to [UInt].
 *
 * If this value is positive, the resulting `UInt` value represents the same numerical value as this `Byte`.
 *
 * The least significant 8 bits of the resulting `UInt` value are the same as the bits of this `Byte` value,
 * whereas the most significant 24 bits are filled with the sign bit of this value.
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalUnsignedTypes::class)
@WasmNoOpCast
public fun Byte.toUInt(): UInt = implementedAsIntrinsic
/**
 * Converts this [Short] value to [UInt].
 *
 * If this value is positive, the resulting `UInt` value represents the same numerical value as this `Short`.
 *
 * The least significant 16 bits of the resulting `UInt` value are the same as the bits of this `Short` value,
 * whereas the most significant 16 bits are filled with the sign bit of this value.
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalUnsignedTypes::class)
@WasmNoOpCast
public fun Short.toUInt(): UInt = implementedAsIntrinsic
/**
 * Converts this [Int] value to [UInt].
 *
 * If this value is positive, the resulting `UInt` value represents the same numerical value as this `Int`.
 *
 * The resulting `UInt` value has the same binary representation as this `Int` value.
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalUnsignedTypes::class)
@WasmNoOpCast
public fun Int.toUInt(): UInt = implementedAsIntrinsic
/**
 * Converts this [Long] value to [UInt].
 *
 * If this value is positive and less than or equals to [UInt.MAX_VALUE], the resulting `UInt` value represents
 * the same numerical value as this `Long`.
 *
 * The resulting `UInt` value is represented by the least significant 32 bits of this `Long` value.
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalUnsignedTypes::class)
@kotlin.internal.InlineOnly
public inline fun Long.toUInt(): UInt = toULong().toUInt()
/**
 * Converts this [Float] value to [UInt].
 *
 * The fractional part, if any, is rounded down towards zero.
 * Returns zero if this `Float` value is negative or `NaN`, [UInt.MAX_VALUE] if it's bigger than `UInt.MAX_VALUE`.
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalUnsignedTypes::class)
public fun Float.toUInt(): UInt = wasm_i32_trunc_sat_f32_u(this).toUInt()
/**
 * Converts this [Double] value to [UInt].
 *
 * The fractional part, if any, is rounded down towards zero.
 * Returns zero if this `Double` value is negative or `NaN`, [UInt.MAX_VALUE] if it's bigger than `UInt.MAX_VALUE`.
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalUnsignedTypes::class)
public fun Double.toUInt(): UInt = wasm_i32_trunc_sat_f64_u(this).toUInt()
