//// FREE_COMPILER_ARGS: -Xplugin=/Users/Maria.Sokolova/IdeaProjects/kotlin/plugins/atomicfu/atomicfu-compiler/build/libs/kotlinx-atomicfu-compiler-plugin-1.9.255-SNAPSHOT.jar
//
//import kotlinx.atomicfu.*
//import kotlin.test.*
//import kotlin.reflect.*
//
//private class LoopClass {
//    val a = atomic(67)
//
//    inline fun casLoop(): Int {
//        a.loop { cur ->
//            return cur + 100
//        }
//    }
//
//    inline fun AtomicInt.extensionEmbeddedLoops(to: Int): Int =
//        loop {
//            compareAndSet(value, to)
//            loop { cur2 ->
//                return cur2
//            }
//        }
//}
//
//@Test
//fun testEasyloop() {
//    val aClass = LoopClass()
//    assertEquals(167, aClass.casLoop())
//}
