package ms.domwillia.jvmemory.preprocessor


object TestMain {
    @JvmStatic
    fun main(arg: Array<String>) {
        parseLog("../monitor-agent/jvmemory.log", ::GraphProcessor)
    }
}