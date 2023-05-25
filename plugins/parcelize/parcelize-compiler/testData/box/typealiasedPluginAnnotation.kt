// WITH_STDLIB

// MODULE: a
// FILE: a.kt
package test

typealias Parcelize = kotlinx.parcelize.Parcelize

// MODULE: b(a)
// FILE: test.kt
@file:JvmName("TestKt")
package test

import android.os.Parcelable

@Parcelize
sealed class Config : Parcelable {
    object Loading : Config()
}

fun box() = "OK"