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
        logger.logMethodEnter(clazz, method)
    }

    fun exitMethod() {
        println("<<< ${stackTracker.head}")
        stackTracker.pop()
        logger.logMethodExit()
    }

    fun onAlloc(type: String): Long {
        val id = nextInstanceId++
        logger.logAllocation(type, id)
        return id
    }

    fun onDealloc(id: Long) {
        logger.logDeallocation(id)
    }

    fun onGetField(objId: Long, clazz: String, field: String, type: String) {
        println("${stackTracker.head} > getfield $type $clazz#$field from object $objId")
        logger.logGetField(objId, field)
    }

    fun onLoadLocalVar(index: Int) {
        println("${stackTracker.head} > load local var $index")
        logger.logLoad(index)
    }

    private fun onStoreLocalVar(type: String, value: Any, index: Int) {
        println("${stackTracker.head} > store $type '$value' in local var $index")
        logger.logStore(type, index)
    }

    private fun onPutField(objId: Long, clazz: String, field: String, type: String, value: Any) {
        println("${stackTracker.head} > putfield $type $clazz#$field on object $objId = '$value'")
        logger.logPutField(objId, field)
    }

    // type specific delegates
    fun onPutFieldBoolean(objId: Long, clazz: String, field: String, type: String, value: Boolean) {
        onPutField(objId, clazz, field, type, value)
    }

    fun onPutFieldChar(objId: Long, clazz: String, field: String, type: String, value: Char) {
        onPutField(objId, clazz, field, type, value)
    }

    fun onPutFieldByte(objId: Long, clazz: String, field: String, type: String, value: Byte) {
        onPutField(objId, clazz, field, type, value)
    }

    fun onPutFieldShort(objId: Long, clazz: String, field: String, type: String, value: Short) {
        onPutField(objId, clazz, field, type, value)
    }

    fun onPutFieldInt(objId: Long, clazz: String, field: String, type: String, value: Int) {
        onPutField(objId, clazz, field, type, value)
    }

    fun onPutFieldFloat(objId: Long, clazz: String, field: String, type: String, value: Float) {
        onPutField(objId, clazz, field, type, value)
    }

    fun onPutFieldLong(objId: Long, clazz: String, field: String, type: String, value: Long) {
        onPutField(objId, clazz, field, type, value)
    }

    fun onPutFieldDouble(objId: Long, clazz: String, field: String, type: String, value: Double) {
        onPutField(objId, clazz, field, type, value)
    }

    fun onPutFieldObject(objId: Long, clazz: String, field: String, type: String, value: Any) {
        onPutField(objId, clazz, field, type, value)
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