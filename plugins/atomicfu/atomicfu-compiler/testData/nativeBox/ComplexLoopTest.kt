//// FREE_COMPILER_ARGS: -Xplugin=/Users/Maria.Sokolova/IdeaProjects/kotlin/plugins/atomicfu/atomicfu-compiler/build/libs/kotlin-atomicfu-compiler-plugin-1.9.255-SNAPSHOT-atomicfu-1.jar
//
//import kotlinx.atomicfu.*
//import kotlin.test.*
//
////private val topLevelA = atomic(0)
//
//class ComplexLoopTest {
//    val a = atomic(10)
//    val b = atomic(11)
//    val c = atomic(12)
//    val r = atomic<String>("aaa")
//    //val intArr = AtomicIntArray(10)
//
//    private inline fun AtomicInt.extensionEmbeddedLoops(to: Int): Int =
//        loop { cur1 ->
//            compareAndSet(cur1, to)
//            loop { cur2 ->
//                return cur2
//            }
//        }
//
//    private inline fun embeddedLoops(to: Int): Int =
//        a.loop { aValue ->
//            b.loop { bValue ->
//                if (b.compareAndSet(bValue, to)) return aValue + bValue
//            }
//        }
//
//    private inline fun embeddedUpdate(to: Int): Int =
//        a.loop { aValue ->
//            a.compareAndSet(aValue, to)
//            return a.updateAndGet { cur -> cur + 100 }
//        }
//
//    private inline fun AtomicRef<String>.extesntionEmbeddedRefUpdate(to: String): String =
//        loop { value ->
//            compareAndSet(value, to)
//            return updateAndGet { cur -> "${cur}AAA" }
//        }
//
//    fun test() {
//        assertEquals(21, embeddedLoops(12))
//        assertEquals(77, c.extensionEmbeddedLoops(77))
//        a.getAndSet(67)
//        //intArr[0].getAndSet(56)
//        //assertEquals(66, intArr[0].extensionEmbeddedLoops(66))
//        assertEquals(166, embeddedUpdate(66))
//        assertEquals("bbbAAA", r.extesntionEmbeddedRefUpdate("bbb"))
//    }
//}
//
////fun testTopLevel() {
////    topLevelA.getAndSet(123)
////}
//
//@Test
//fun testComplexLoopTest() {
//    val testClass = ComplexLoopTest()
//    testClass.test()
//    //testTopLevel()
//}
