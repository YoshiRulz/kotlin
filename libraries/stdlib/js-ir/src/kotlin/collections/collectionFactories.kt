/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

public fun stringSetOf(vararg elements: String): HashSet<String> {
    return HashSet<String>().apply { addAll(elements) }
}

public fun linkedStringSetOf(vararg elements: String): LinkedHashSet<String> {
    return LinkedHashSet<String>().apply { addAll(elements) }
}

public fun <V> stringMapOf(vararg pairs: Pair<String, V>): HashMap<String, V> {
    return HashMap<String, V>().apply { putAll(pairs) }
}

public fun <V> linkedStringMapOf(vararg pairs: Pair<String, V>): LinkedHashMap<String, V> {
    return LinkedHashMap<String, V>().apply { putAll(pairs) }
}
