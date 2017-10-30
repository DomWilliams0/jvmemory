package ms.domwillia.jvmemory.monitor

import org.objectweb.asm.Type

class InjectedMonitor {

    companion object {
        val fieldName = "__injectedMonitor__"
        val type = Type.getType(InjectedMonitor::class.java)
        val internalName = type.internalName
        val descriptor = type.descriptor
    }

    private val stackTracker: StackTracker = StackTracker()

    fun enterMethod(clazz: String, method: String) {
        stackTracker.push(clazz, method)
        println(">>> ${stackTracker.head}")
    }

    fun exitMethod() {
        println("<<< ${stackTracker.head}")
        stackTracker.pop()
    }
}