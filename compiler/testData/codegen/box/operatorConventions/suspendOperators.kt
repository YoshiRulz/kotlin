// WITH_STDLIB
// WITH_COROUTINES
// IGNORE_BACKEND_K1: ANY
// IGNORE_BACKEND: WASM

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*
import kotlin.reflect.KProperty

fun <T> runBlocking(c: suspend () -> T): T {
    var res: T? = null
    c.startCoroutine(Continuation(EmptyCoroutineContext) {
        res = it.getOrThrow()
    })
    return res!!
}

class A {
    suspend operator fun get(x: Int) = "K"
    suspend operator fun set(x: Int, v: String) {}

    operator suspend fun contains(y: String): Boolean = true

    suspend operator fun provideDelegate(a: Nothing?, p: KProperty<*>) = lazy { "O" }

    suspend fun getO(): String {
        val delegated by this
        return delegated
    }
}

fun box() = runBlocking {
    val a = A()
    if ("" !in a) return@runBlocking "FAIL"
    a[1] = ""

    a.getO() + a[2]
}