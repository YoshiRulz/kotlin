/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text

import kotlin.wasm.internal.utoa64

// To make unsigned/src/kotlin/UStrings.kt happy
@kotlin.internal.InlineOnly
internal inline fun ulongToString(value: Long, radix: Int) = utoa64(value.toULong(), radix)