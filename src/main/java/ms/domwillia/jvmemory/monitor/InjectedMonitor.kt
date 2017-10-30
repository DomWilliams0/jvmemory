package ms.domwillia.jvmemory.monitor

class InjectedMonitor {

    companion object {
        val fieldName = "__injectedMonitor__"
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