// FREE_COMPILER_ARGS: -Xplugin=/Users/Maria.Sokolova/IdeaProjects/kotlin/plugins/atomicfu/atomicfu-compiler/build/libs/kotlinx-atomicfu-compiler-plugin-1.9.255-SNAPSHOT.jar

import kotlinx.atomicfu.*
import kotlin.test.*

class ParameterizedInlineFunExtensionTest {

    private inline fun <S> AtomicRef<S>.foo(arg: S) {
//        val res = bar(res1, res2)
//        return res
        //return "12"
         //f(value)
        //return arg
        val a = arg
    }

//    private inline fun <S> AtomicRef<S>.bar(res1: S, res2: S): S {
//        return res2
//    }
//
    private val tail = atomic("aaa")

    fun testClose() {
        tail.foo("djnvjkd")
        //assertEquals("ccc", res)
    }
}

@Test
fun testParameterizedInlineFunExtensionTest() {
    val testClass = ParameterizedInlineFunExtensionTest()
    testClass.testClose()
}
