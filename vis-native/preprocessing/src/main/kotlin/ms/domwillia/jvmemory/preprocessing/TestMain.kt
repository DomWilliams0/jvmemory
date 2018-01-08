package ms.domwillia.jvmemory.preprocessing

import ms.domwillia.jvmemory.parser.parseLog

object TestMain {
    @JvmStatic
    fun main(arg: Array<String>) {
        parseLog("../../monitor-agent/jvmemory.log")
    }
}