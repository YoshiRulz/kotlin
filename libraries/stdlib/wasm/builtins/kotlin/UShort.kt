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
public class UShort private constructor(private val value: UShort) : Comparable<UShort> {
    companion object {
        /**
         * A constant holding the minimum value an instance of UShort can have.
         */
        public const val MIN_VALUE: UShort = 0u

        /**
         * A constant holding the maximum value an instance of UShort can have.
         */
        public const val MAX_VALUE: UShort = 65535u

        /**
         * The number of bytes used to represent an instance of UShort in a binary form.
         */
        public const val SIZE_BYTES: Int = 2

        /**
         * The number of bits used to represent an instance of UShort in a binary form.
         */
        public const val SIZE_BITS: Int = 16
    }

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline operator fun compareTo(other: UByte): Int =
        this.compareTo(other.toUShort())

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    @kotlin.internal.InlineOnly
    @Suppress("OVERRIDE_BY_INLINE")
    @kotlin.internal.IntrinsicConstEvaluation
    public override inline operator fun compareTo(other: UShort): Int =
        this.toInt().compareTo(other.toInt())

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline operator fun compareTo(other: UInt): Int =
        this.toUInt().compareTo(other)

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
    public inline operator fun plus(other: UByte): UInt = this.toUInt().plus(other.toUInt())

    /** Adds the other value to this value. */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline operator fun plus(other: UShort): UInt = this.toUInt().plus(other.toUInt())

    /** Adds the other value to this value. */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline operator fun plus(other: UInt): UInt = this.toUInt().plus(other)

    /** Adds the other value to this value. */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline operator fun plus(other: ULong): ULong = this.toULong().plus(other)

    /** Subtracts the other value from this value. */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline operator fun minus(other: UByte): UInt = this.toUInt().minus(other.toUInt())

    /** Subtracts the other value from this value. */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline operator fun minus(other: UShort): UInt = this.toUInt().minus(other.toUInt())

    /** Subtracts the other value from this value. */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline operator fun minus(other: UInt): UInt = this.toUInt().minus(other)

    /** Subtracts the other value from this value. */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline operator fun minus(other: ULong): ULong = this.toULong().minus(other)

    /** Multiplies this value by the other value. */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline operator fun times(other: UByte): UInt = this.toUInt().times(other.toUInt())

    /** Multiplies this value by the other value. */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline operator fun times(other: UShort): UInt = this.toUInt().times(other.toUInt())

    /** Multiplies this value by the other value. */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline operator fun times(other: UInt): UInt = this.toUInt().times(other)

    /** Multiplies this value by the other value. */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline operator fun times(other: ULong): ULong = this.toULong().times(other)

    /** Divides this value by the other value, truncating the result to an integer that is closer to zero. */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline operator fun div(other: UByte): UInt = this.toUInt().div(other.toUInt())

    /** Divides this value by the other value, truncating the result to an integer that is closer to zero. */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline operator fun div(other: UShort): UInt = this.toUInt().div(other.toUInt())

    /** Divides this value by the other value, truncating the result to an integer that is closer to zero. */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline operator fun div(other: UInt): UInt = this.toUInt().div(other)

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
    public inline operator fun rem(other: UByte): UInt = this.toUInt().rem(other.toUInt())

    /**
     * Calculates the remainder of truncating division of this value (dividend) by the other value (divisor).
     *
     * The result is always less than the divisor.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline operator fun rem(other: UShort): UInt = this.toUInt().rem(other.toUInt())

    /**
     * Calculates the remainder of truncating division of this value (dividend) by the other value (divisor).
     *
     * The result is always less than the divisor.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline operator fun rem(other: UInt): UInt = this.toUInt().rem(other)

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
    public inline fun floorDiv(other: UByte): UInt = this.toUInt().floorDiv(other.toUInt())

    /**
     * Divides this value by the other value, flooring the result to an integer that is closer to negative infinity.
     *
     * For unsigned types, the results of flooring division and truncating division are the same.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline fun floorDiv(other: UShort): UInt = this.toUInt().floorDiv(other.toUInt())

    /**
     * Divides this value by the other value, flooring the result to an integer that is closer to negative infinity.
     *
     * For unsigned types, the results of flooring division and truncating division are the same.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline fun floorDiv(other: UInt): UInt = this.toUInt().floorDiv(other)

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
    public inline fun mod(other: UByte): UByte = this.toUInt().mod(other.toUInt()).toUByte()

    /**
     * Calculates the remainder of flooring division of this value (dividend) by the other value (divisor).
     *
     * The result is always less than the divisor.
     *
     * For unsigned types, the remainders of flooring division and truncating division are the same.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline fun mod(other: UShort): UShort = this.toUInt().mod(other.toUInt()).toUShort()

    /**
     * Calculates the remainder of flooring division of this value (dividend) by the other value (divisor).
     *
     * The result is always less than the divisor.
     *
     * For unsigned types, the remainders of flooring division and truncating division are the same.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline fun mod(other: UInt): UInt = this.toUInt().mod(other)

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
    public inline operator fun inc(): UShort = this.plus(1u).toUShort()

    /**
     * Returns this value decremented by one.
     *
     * @sample samples.misc.Builtins.dec
     */
    @kotlin.internal.InlineOnly
    public inline operator fun dec(): UShort = this.minus(1u).toUShort()

    /** Creates a range from this value to the specified [other] value. */
    public operator fun rangeTo(other: UShort): UIntRange = UIntRange(this.toUInt(), other.toUInt())

    /**
     * Creates a range from this value up to but excluding the specified [other] value.
     *
     * If the [other] value is less than or equal to `this` value, then the returned range is empty.
     */
    @SinceKotlin("1.9")
    @WasExperimental(ExperimentalStdlibApi::class)
    @kotlin.internal.InlineOnly
    public inline operator fun rangeUntil(other: UShort): UIntRange = this.toUInt() until other.toUInt()

    /** Performs a bitwise AND operation between the two values. */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline infix fun and(other: UShort): UShort = (this.toInt() and other.toInt()).toUShort()

    /** Performs a bitwise OR operation between the two values. */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline infix fun or(other: UShort): UShort = (this.toInt() or other.toInt()).toUShort()

    /** Performs a bitwise XOR operation between the two values. */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline infix fun xor(other: UShort): UShort = (this.toInt() xor other.toInt()).toUShort()

    /** Inverts the bits in this value. */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline fun inv(): UShort = this.toInt().inv().toUShort()

    /**
     * Converts this [UShort] value to [Byte].
     *
     * If this value is less than or equals to [Byte.MAX_VALUE], the resulting `Byte` value represents
     * the same numerical value as this `UShort`.
     *
     * The resulting `Byte` value is represented by the least significant 8 bits of this `UShort` value.
     * Note that the resulting `Byte` value may be negative.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline fun toByte(): Byte = this.toInt().toByte()

    /**
     * Converts this [UShort] value to [Short].
     *
     * If this value is less than or equals to [Short.MAX_VALUE], the resulting `Short` value represents
     * the same numerical value as this `UShort`. Otherwise the result is negative.
     *
     * The resulting `Short` value has the same binary representation as this `UShort` value.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline fun toShort(): Short = this.toInt().toShort()

    /**
     * Converts this [UShort] value to [Int].
     *
     * The resulting `Int` value represents the same numerical value as this `UShort`.
     *
     * The least significant 16 bits of the resulting `Int` value are the same as the bits of this `UShort` value,
     * whereas the most significant 16 bits are filled with zeros.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline fun toInt(): Int = this.toUInt().reinterpretAsInt()

    /**
     * Converts this [UShort] value to [Long].
     *
     * The resulting `Long` value represents the same numerical value as this `UShort`.
     *
     * The least significant 16 bits of the resulting `Long` value are the same as the bits of this `UShort` value,
     * whereas the most significant 48 bits are filled with zeros.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline fun toLong(): Long = this.toInt().toLong()

    /**
     * Converts this [UShort] value to [UByte].
     *
     * If this value is less than or equals to [UByte.MAX_VALUE], the resulting `UByte` value represents
     * the same numerical value as this `UShort`.
     *
     * The resulting `UByte` value is represented by the least significant 8 bits of this `UShort` value.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline fun toUByte(): UByte = this.toUInt().toUByte()

    /** Returns this value. */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline fun toUShort(): UShort = this

    /**
     * Converts this [UShort] value to [UInt].
     *
     * The resulting `UInt` value represents the same numerical value as this `UShort`.
     *
     * The least significant 16 bits of the resulting `UInt` value are the same as the bits of this `UShort` value,
     * whereas the most significant 16 bits are filled with zeros.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline fun toUInt(): UInt = reinterpretAsUInt()

    /**
     * Converts this [UShort] value to [ULong].
     *
     * The resulting `ULong` value represents the same numerical value as this `UShort`.
     *
     * The least significant 16 bits of the resulting `ULong` value are the same as the bits of this `UShort` value,
     * whereas the most significant 48 bits are filled with zeros.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public inline fun toULong(): ULong = this.toUInt().toULong()

    /**
     * Converts this [UShort] value to [Float].
     *
     * The resulting `Float` value represents the same numerical value as this `UShort`.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public fun toFloat(): Float = wasm_f32_convert_i32_u(this.toInt())

    /**
     * Converts this [UShort] value to [Double].
     *
     * The resulting `Double` value represents the same numerical value as this `UShort`.
     */
    @kotlin.internal.InlineOnly
    @kotlin.internal.IntrinsicConstEvaluation
    public fun toDouble(): Double = wasm_f64_convert_i32_u(this.toInt())

    public override fun toString(): String = toUInt().toString()

    public override fun hashCode(): Int = this.toInt()

    @kotlin.internal.IntrinsicConstEvaluation
    public override fun equals(other: Any?): Boolean =
        other is UShort && wasm_i32_eq(this.toInt(), other.toInt())

    @kotlin.internal.IntrinsicConstEvaluation
    @WasmOp(WasmOp.I32_EQ)
    public fun equals(other: UShort): Boolean = implementedAsIntrinsic

    @WasmNoOpCast
    @PublishedApi
    internal fun reinterpretAsUInt(): UInt =
        implementedAsIntrinsic
}
/**
 * Converts this [Byte] value to [UShort].
 *
 * If this value is positive, the resulting `UShort` value represents the same numerical value as this `Byte`.
 *
 * The least significant 8 bits of the resulting `UShort` value are the same as the bits of this `Byte` value,
 * whereas the most significant 8 bits are filled with the sign bit of this value.
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalUnsignedTypes::class)
@kotlin.internal.InlineOnly
public inline fun Byte.toUShort(): UShort = toUInt().toUShort()
/**
 * Converts this [Short] value to [UShort].
 *
 * If this value is positive, the resulting `UShort` value represents the same numerical value as this `Short`.
 *
 * The resulting `UShort` value has the same binary representation as this `Short` value.
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalUnsignedTypes::class)
@kotlin.internal.InlineOnly
public inline fun Short.toUShort(): UShort = toUInt().toUShort()
/**
 * Converts this [Int] value to [UShort].
 *
 * If this value is positive and less than or equals to [UShort.MAX_VALUE], the resulting `UShort` value represents
 * the same numerical value as this `Int`.
 *
 * The resulting `UShort` value is represented by the least significant 16 bits of this `Int` value.
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalUnsignedTypes::class)
@kotlin.internal.InlineOnly
public inline fun Int.toUShort(): UShort = toUInt().toUShort()
/**
 * Converts this [Long] value to [UShort].
 *
 * If this value is positive and less than or equals to [UShort.MAX_VALUE], the resulting `UShort` value represents
 * the same numerical value as this `Long`.
 *
 * The resulting `UShort` value is represented by the least significant 16 bits of this `Long` value.
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalUnsignedTypes::class)
@kotlin.internal.InlineOnly
public inline fun Long.toUShort(): UShort = toULong().toUShort()
