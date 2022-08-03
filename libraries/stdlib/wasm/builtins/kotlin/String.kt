/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.wasm.internal.*
import kotlin.wasm.internal.reftypes.*

public class String internal @WasmPrimitiveConstructor constructor(internal val reference: stringref) : Comparable<String>, CharSequence {
    public companion object {}

    public override val length: Int get() =
        wasm_stringview_wtf16_length(wasm_string_as_wtf16(reference))

    public override fun get(index: Int): Char =
        wasm_stringview_wtf16_get_codeunit(wasm_string_as_wtf16(reference), index).toChar()

    public override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        val view = wasm_string_as_wtf16(reference)
        val length = wasm_stringview_wtf16_length(view)
        val actualStartIndex = startIndex.coerceAtLeast(0)
        val actualEndIndex = endIndex.coerceAtMost(length)
        return String(wasm_stringview_wtf16_slice(view, actualStartIndex, actualEndIndex))
    }

    /**
     * Returns the character of this string at the specified [index].
     *
     * If the [index] is out of bounds of this string, throws an [IndexOutOfBoundsException] except in Kotlin/JS
     * where the behavior is unspecified.
     */
    public operator fun plus(other: Any?): String {
        val otherReference = when (other) {
            is String -> other.reference
            else -> other.toString().reference
        }
        return String(wasm_string_concat(reference, otherReference))
    }

    public override fun compareTo(other: String): Int {
        val thisIterator = wasm_string_as_iter(this.reference)
        val otherIterator = wasm_string_as_iter(other.reference)

        var thisCode = wasm_stringview_iter_next(thisIterator)
        var otherCode = wasm_stringview_iter_next(otherIterator)
        while (thisCode != -1 && otherCode != -1) {
            val diff = thisCode - otherCode
            if (diff != 0) return diff
            thisCode = wasm_stringview_iter_next(thisIterator)
            otherCode = wasm_stringview_iter_next(otherIterator)
        }
        return if (thisCode == -1 && otherCode == -1) 0 else this.length - other.length
    }

    public override fun equals(other: Any?): Boolean {
        if (other === null) return false
        if (this === other) return true
        return other is String && wasm_string_eq(reference, other.reference)
    }

    public override fun toString(): String = this

    public override fun hashCode(): Int {
        if (_hashCode != 0) return _hashCode

        var hash = 0
        forEachCodePoint { hash = 31 * hash + it }
        _hashCode = hash
        return _hashCode
    }
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun WasmCharArray.createString(): String =
    String(wasm_string_new_wtf16_array(this, 0, len()))

internal inline fun String.forEachCodePoint(body: (Int) -> Unit) {
    val iter = wasm_string_as_iter(reference)
    var codePoint = wasm_stringview_iter_next(iter)
    while (codePoint != -1) {
        body(codePoint)
        codePoint = wasm_stringview_iter_next(iter)
    }
}

internal fun stringLiteral(poolId: Int, startAddress: Int, length: Int): String {
    val cached = stringPool[poolId]
    if (cached !== null) {
        return cached
    }

    val chars = array_new_data0<WasmCharArray>(startAddress, length)
    val newString = chars.createString()
    stringPool[poolId] = newString
    return newString
}