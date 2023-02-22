// FREE_COMPILER_ARGS: -Xplugin=/Users/Maria.Sokolova/IdeaProjects/kotlin/plugins/atomicfu/atomicfu-compiler/build/libs/kotlinx-atomicfu-compiler-plugin-1.8.255-volatile-SNAPSHOT-new.jar

import kotlinx.atomicfu.*
import kotlin.test.*
import kotlin.reflect.*

private class AAA {
    private val _a = atomic(67)

    private val aa: Int = 464

    inline fun AtomicInt.foo(update: Int) {
        compareAndSet(value, update)
    }

    fun callFoo(update: Int): Int {
        _a.foo(update)
        return _a.value
    }
}


@Test
fun testAtomicExtension() {
    val aClass = AAA()
    //assertEquals(56, aClass.callFoo(56))
}
