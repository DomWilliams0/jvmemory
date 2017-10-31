package ms.domwillia.jvmemory.monitor.printer

import ms.domwillia.jvmemory.monitor.StackTracker

class LoadPrinter(private val stackTracker: StackTracker) {

    private fun loadPrint(type: String, index: Int) {
        println("${stackTracker.head} > load $type local var $index")
    }

    fun booleanDo(index: Int) {
        loadPrint("bool", index)
    }

    fun charDo(index: Int) {
        loadPrint("char", index)
    }

    fun byteDo(index: Int) {
        loadPrint("byte", index)
    }

    fun shortDo(index: Int) {
        loadPrint("short", index)
    }

    fun intDo(index: Int) {
        loadPrint("int", index)
    }

    fun floatDo(index: Int) {
        loadPrint("float", index)
    }

    fun longDo(index: Int) {
        loadPrint("long", index)
    }

    fun doubleDo(index: Int) {
        loadPrint("double", index)
    }

    fun objectDo(index: Int) {
        loadPrint("object", index)
    }
}