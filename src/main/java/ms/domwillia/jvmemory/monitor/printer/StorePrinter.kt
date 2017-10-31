package ms.domwillia.jvmemory.monitor.printer

import ms.domwillia.jvmemory.monitor.StackTracker

class StorePrinter(private val stackTracker: StackTracker) {

    private fun storePrint(type: String, value: Any, index: Int) {
        println("${stackTracker.head} > store $type in '$value' in local var $index")
    }

    fun booleanDo(value: Boolean, index: Int) {
        storePrint("bool", value, index)
    }

    fun charDo(value: Char, index: Int) {
        storePrint("char", value, index)
    }

    fun byteDo(value: Byte, index: Int) {
        storePrint("byte", value, index)
    }

    fun shortDo(value: Short, index: Int) {
        storePrint("short", value, index)
    }

    fun intDo(value: Int, index: Int) {
        storePrint("int", value, index)
    }

    fun floatDo(value: Float, index: Int) {
        storePrint("float", value, index)
    }

    fun longDo(value: Long, index: Int) {
        storePrint("long", value, index)
    }

    fun doubleDo(value: Double, index: Int) {
        storePrint("double", value, index)
    }

    fun objectDo(value: Any, index: Int) {
        storePrint("object", value, index)
    }
}
