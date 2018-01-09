package ms.domwillia.jvmemory.preprocessing


object TestMain {
    @JvmStatic
    fun main(arg: Array<String>) {
        parseLog("../../monitor-agent/jvmemory.log", ::GraphProcessor)
    }
}