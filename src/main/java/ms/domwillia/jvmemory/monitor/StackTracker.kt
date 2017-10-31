package ms.domwillia.jvmemory.monitor

import java.util.*

class StackTracker {
    private val stack = Stack<Frame>()

    val head: String
        get() {
            return when {
                stack.empty() -> "-"
                else -> stack.peek().let { (clazz, method) -> "${prettyClass(clazz)}:$method" }
            }
        }


    fun push(clazz: String, method: String) {
        stack.push(Frame(clazz, method))
    }

    fun pop() {
        stack.pop()
    }

    private fun prettyClass(clazz: String): String = clazz.substring(clazz.lastIndexOf('/') + 1)

    internal data class Frame(val clazz: String, val method: String)
}
