package ms.domwillia.jvmemory.monitor

import ms.domwillia.jvmemory.monitor.logging.Logger
import org.objectweb.asm.Type
import java.io.FileOutputStream

@Suppress("unused")
object Monitor {
    val instanceName = "INSTANCE" // ty kotlin for `object`
    val type = Type.getType(Monitor::class.java)
    val internalName = type.internalName
    val descriptor = type.descriptor

    var logger = Logger(FileOutputStream("jvmemory.log"))
    private var nextInstanceId: Long = 1
    val instanceIdFieldName = "__uniqueID__"

    fun getHandler(type: Type, op: TypeSpecificOperation): String? {
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

        return "on$op$typeName"
    }

    enum class TypeSpecificOperation {
        STORE {
            override fun toString(): String = "Store"
        },
        PUTFIELD {
            override fun toString(): String = "PutField"
        }
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

    fun onAlloc(type: String): Long {
        val id = nextInstanceId++
        logger.logAllocation(type, id)
        return id
    }

    fun onDealloc(id: Long) {
        logger.logDeallocation(id)
    }

    fun onGetField(objHash: Int, clazz: String, field: String, type: String) {
        println("${stackTracker.head} > getfield $type $clazz#$field from object $objHash")
    }

    fun onLoadLocalVar(index: Int) {
        println("${stackTracker.head} > load local var $index")
    }

    private fun onStoreLocalVar(type: String, value: Any, index: Int) {
        println("${stackTracker.head} > store $type '$value' in local var $index")
    }

    private fun onPutField(objHash: Int, clazz: String, field: String, type: String, value: Any) {
        println("${stackTracker.head} > putfield $type $clazz#$field on object $objHash = '$value'")
    }

    // type specific delegates
    fun onPutFieldBoolean(objHash: Int, clazz: String, field: String, type: String, value: Boolean) {
        onPutField(objHash, clazz, field, type, value)
    }

    fun onPutFieldChar(objHash: Int, clazz: String, field: String, type: String, value: Char) {
        onPutField(objHash, clazz, field, type, value)
    }

    fun onPutFieldByte(objHash: Int, clazz: String, field: String, type: String, value: Byte) {
        onPutField(objHash, clazz, field, type, value)
    }

    fun onPutFieldShort(objHash: Int, clazz: String, field: String, type: String, value: Short) {
        onPutField(objHash, clazz, field, type, value)
    }

    fun onPutFieldInt(objHash: Int, clazz: String, field: String, type: String, value: Int) {
        onPutField(objHash, clazz, field, type, value)
    }

    fun onPutFieldFloat(objHash: Int, clazz: String, field: String, type: String, value: Float) {
        onPutField(objHash, clazz, field, type, value)
    }

    fun onPutFieldLong(objHash: Int, clazz: String, field: String, type: String, value: Long) {
        onPutField(objHash, clazz, field, type, value)
    }

    fun onPutFieldDouble(objHash: Int, clazz: String, field: String, type: String, value: Double) {
        onPutField(objHash, clazz, field, type, value)
    }

    fun onPutFieldObject(objHash: Int, clazz: String, field: String, type: String, value: Any) {
        onPutField(objHash, clazz, field, type, value)
    }

    // storing
    fun onStoreBoolean(value: Boolean, index: Int) {
        onStoreLocalVar("Boolean", value, index)
    }

    fun onStoreChar(value: Char, index: Int) {
        onStoreLocalVar("Char", value, index)
    }

    fun onStoreByte(value: Byte, index: Int) {
        onStoreLocalVar("Byte", value, index)
    }

    fun onStoreShort(value: Short, index: Int) {
        onStoreLocalVar("Short", value, index)
    }

    fun onStoreInt(value: Int, index: Int) {
        onStoreLocalVar("Int", value, index)
    }

    fun onStoreFloat(value: Float, index: Int) {
        onStoreLocalVar("Float", value, index)
    }

    fun onStoreLong(value: Long, index: Int) {
        onStoreLocalVar("Long", value, index)
    }

    fun onStoreDouble(value: Double, index: Int) {
        onStoreLocalVar("Double", value, index)
    }

    fun onStoreObject(value: Any, index: Int) {
        onStoreLocalVar("Object", value, index)
    }
}