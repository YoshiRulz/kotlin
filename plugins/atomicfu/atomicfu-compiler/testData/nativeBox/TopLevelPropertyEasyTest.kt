// FREE_COMPILER_ARGS: -Xplugin=/Users/Maria.Sokolova/IdeaProjects/kotlin/plugins/atomicfu/atomicfu-compiler/build/libs/kotlin-atomicfu-compiler-plugin-1.9.255-SNAPSHOT-atomicfu-1.jar

import kotlinx.atomicfu.*
import kotlin.test.*

private val a = atomic(0)

@Test
fun testTopLevelInt() {
    assertEquals(0, a.value)
}
