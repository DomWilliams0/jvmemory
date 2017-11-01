package ms.domwillia.jvmemory.monitor

import ms.domwillia.jvmemory.monitor.printer.StorePrinter
import org.objectweb.asm.Type

@Suppress("unused")
class InjectedMonitor {

    companion object {
        val fieldName = "__injectedMonitor__"
        val type = Type.getType(InjectedMonitor::class.java)
        val internalName = type.internalName
        val descriptor = type.descriptor

        fun getTypeSpecificLocalVarFuncName(storing: Boolean, type: Type): String? {
            val typeName = when (type.sort) {
                Type.BOOLEAN -> "Boolean"
                Type.CHAR -> "Char"
                Type.BYTE -> "Byte"
                Type.SHORT -> "Short"
                Type.INT -> "Int"
                Type.FLOAT -> "Float"
                Type.LONG -> "Long"
                Type.DOUBLE -> "Double"
                Type.OBJECT -> "Object"
            // TODO arrays just complicate things at this stage
            // Type.ARRAY -> "array"
                else -> return null
            }

            val action = if (storing) "Store" else "Load"
            return "on$action$typeName"
        }

    }

    private val stackTracker: StackTracker = StackTracker()
    private val storeMonitor: StorePrinter = StorePrinter(stackTracker)

    fun enterMethod(clazz: String, method: String) {
        stackTracker.push(clazz, method)
        println(">>> ${stackTracker.head}")
    }

    fun exitMethod() {
        println("<<< ${stackTracker.head}")
        stackTracker.pop()
    }

    // fields
    fun onGetField(objHash: Int, clazz: String, field: String, type: String) {
        println("${stackTracker.head} > getfield $type $clazz#$field from object $objHash")
    }

    // loading
    fun onLoadLocalVar(index: Int) {
        println("${stackTracker.head} > load local var $index")
    }

    // storing
    fun onStoreBoolean(value: Boolean, index: Int) {
        storeMonitor.booleanDo(value, index)
    }

    fun onStoreChar(value: Char, index: Int) {
        storeMonitor.charDo(value, index)
    }

    fun onStoreByte(value: Byte, index: Int) {
        storeMonitor.byteDo(value, index)
    }

    fun onStoreShort(value: Short, index: Int) {
        storeMonitor.shortDo(value, index)
    }

    fun onStoreInt(value: Int, index: Int) {
        storeMonitor.intDo(value, index)
    }

    fun onStoreFloat(value: Float, index: Int) {
        storeMonitor.floatDo(value, index)
    }

    fun onStoreLong(value: Long, index: Int) {
        storeMonitor.longDo(value, index)
    }

    fun onStoreDouble(value: Double, index: Int) {
        storeMonitor.doubleDo(value, index)
    }

    fun onStoreObject(value: Any, index: Int) {
        storeMonitor.objectDo(value, index)
    }
}