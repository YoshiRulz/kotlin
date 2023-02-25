// FREE_COMPILER_ARGS: -Xplugin=/Users/Maria.Sokolova/IdeaProjects/kotlin/plugins/atomicfu/atomicfu-compiler/build/libs/kotlinx-atomicfu-compiler-plugin-1.9.255-SNAPSHOT.jar

import kotlinx.atomicfu.*
import kotlin.test.*
import kotlin.reflect.*

private class AAA {
    val _a = atomic(67)
    var aa = 77
}

inline fun AtomicInt.foo() {
    compareAndSet(67, 999)
    assertEquals(999, value)
    value = 56
    assertEquals(56, getAndSet(77))
    assertEquals(77, value)
}

@Test
fun testAtomicExtension() {
    val aClass = AAA()
    aClass._a.foo()
}
