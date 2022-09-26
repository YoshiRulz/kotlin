//// FREE_COMPILER_ARGS: -Xplugin=/Users/Maria.Sokolova/IdeaProjects/kotlin/plugins/atomicfu/atomicfu-compiler/build/libs/kotlinx-atomicfu-compiler-plugin-1.8.255-volatile1-SNAPSHOT.jar
//
//import kotlin.jvm.Volatile
//import kotlin.native.concurrent.*
//import kotlinx.atomicfu.*
//import kotlin.test.*
//
//class A(@Volatile var n: Int) {
//    val a = atomic(56)
//}
//
//fun minusFun(): Int {
//    val aClass = A(77)
//    return aClass::n.getAndSetField(99) - 1
//}
//
////fun myCompareAndSetField(a:A, expected: Int, new: Int) = a.compareAndSetField(A::n, expected, new)
//
////fun myGetAndSetField(a:A, new: Int) = a.getAndSetField(A::n, new)
//
//@Test
//fun foo() {
//    val aClass = A(77)
//    aClass::n.compareAndSetField(1111, 56)
//    aClass.a.compareAndSet(56, 88)
//    assertEquals(88, aClass.a.value)
//    aClass.a.getAndSet(99)
//    assertEquals(99, aClass.a.value)
//    aClass.a.addAndGet(10)
//    assertEquals(111, aClass.a.value)
//}
