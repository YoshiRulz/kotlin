/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.full

import kotlin.reflect.KClass

public fun <T : Any> KClass<T>.createInstance(): T {
    val jsClass = js.asDynamic()

    if (jsClass === js("Object")) return js("{}")

    val noArgsConstructor = jsClass.`$metadata$`.unsafeCast<Metadata?>()?.defaultConstructor
        ?: throw IllegalArgumentException("Class should have a single no-arg constructor: $this")

    val isProvidedClassEsClass = isEsClass(jsClass)

    return if (!isProvidedClassEsClass || noArgsConstructor === jsClass) {
        js("new noArgsConstructor()")
    } else {
        js("noArgsConstructor.call(jsClass)")
    }
}

@Suppress("UNUSED_PARAMETER")
private fun isEsClass(reference: dynamic): Boolean = js("reference.toString()[0] === 'c'")
