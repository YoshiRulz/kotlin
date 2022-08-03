/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text

import kotlin.wasm.internal.*

/**
 * Encodes the [string] using UTF-8 and returns the resulting [ByteArray].
 *
 * @param string the string to encode.
 * @param startIndex the start offset (inclusive) of the substring to encode.
 * @param endIndex the end offset (exclusive) of the substring to encode.
 * @param throwOnMalformed whether to throw on malformed char sequence or replace by the [REPLACEMENT_BYTE_SEQUENCE].
 *
 * @throws CharacterCodingException if the char sequence is malformed and [throwOnMalformed] is true.
 */
internal fun encodeUtf8(string: String, startIndex: Int, endIndex: Int, throwOnMalformed: Boolean): ByteArray {
    require(startIndex >= 0 && endIndex <= string.length && startIndex <= endIndex)

    val slicedString = wasm_stringview_wtf16_slice(wasm_string_as_wtf16(string.reference), startIndex, endIndex)
    val arraySize = if (throwOnMalformed) wasm_string_measure_utf8(slicedString) else wasm_string_measure_wtf8(slicedString)
    if (arraySize == -1) throw CharacterCodingException("Malformed sequence")
    return ByteArray(arraySize).also {
        wasm_string_encode_lossy_utf8_array(slicedString, it.storage, 0)
    }
}

/**
 * The character a malformed UTF-8 byte sequence is replaced by.
 */
private const val REPLACEMENT_CHAR = '\uFFFD'

/**
 * Decodes the UTF-8 [bytes] array and returns the resulting [String].
 *
 * @param bytes the byte array to decode.
 * @param startIndex the start offset (inclusive) of the array to be decoded.
 * @param endIndex the end offset (exclusive) of the array to be encoded.
 * @param throwOnMalformed whether to throw on malformed byte sequence or replace by the [REPLACEMENT_CHAR].
 *
 * @throws CharacterCodingException if the array is malformed UTF-8 byte sequence and [throwOnMalformed] is true.
 */
internal fun decodeUtf8(bytes: ByteArray, startIndex: Int, endIndex: Int, throwOnMalformed: Boolean): String {
    require(startIndex >= 0 && endIndex <= bytes.size && startIndex <= endIndex)

    val result = String(wasm_string_new_lossy_utf8_array(bytes.storage, startIndex, endIndex))
    if (throwOnMalformed) {
        result.forEachCodePoint { if (it == 0xFFFD) throw CharacterCodingException("Malformed sequence") }
    }
    return result
}