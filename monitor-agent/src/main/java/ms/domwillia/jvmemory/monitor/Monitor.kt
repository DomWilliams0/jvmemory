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
    private val invalidInstanceId: Long = 0

    var effectiveId: Long = 0

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

    private fun assertAllocated(id: Long) {
        assert(id > 0L, { "Uninitialised object id: onAlloc wasn't called" })
    }

    fun enterMethod(clazz: String, method: String) {
        logger.logMethodEnter(clazz, method)
    }

    fun exitMethod() {
        logger.logMethodExit()
    }

    fun onAlloc(type: String): Long {
        val id = nextInstanceId++
        logger.logAllocation(type, id)
        effectiveId = id
        return id
    }

    fun onDealloc(id: Long) {
        assertAllocated(id)
        logger.logDeallocation(id)
    }

    fun onGetField(objId: Long, clazz: String, field: String, type: String) {
        assertAllocated(objId)
        logger.logGetField(objId, field)
    }

    fun onLoadLocalVar(index: Int) {
        logger.logLoad(index)
    }

    private fun onStoreLocalVar(type: String, value: Any, index: Int) {
        logger.logStore(type, index)
    }

    private fun onPutField(objId: Long, clazz: String, field: String, type: String, valueId: Long) {
        assertAllocated(objId)
        logger.logPutField(objId, field, valueId)
    }

    // type specific delegates
    fun onPutFieldBoolean(objId: Long, clazz: String, field: String, type: String, value: Boolean) {
        onPutField(objId, clazz, field, type, invalidInstanceId)
    }

    fun onPutFieldChar(objId: Long, clazz: String, field: String, type: String, value: Char) {
        onPutField(objId, clazz, field, type, invalidInstanceId)
    }

    fun onPutFieldByte(objId: Long, clazz: String, field: String, type: String, value: Byte) {
        onPutField(objId, clazz, field, type, invalidInstanceId)
    }

    fun onPutFieldShort(objId: Long, clazz: String, field: String, type: String, value: Short) {
        onPutField(objId, clazz, field, type, invalidInstanceId)
    }

    fun onPutFieldInt(objId: Long, clazz: String, field: String, type: String, value: Int) {
        onPutField(objId, clazz, field, type, invalidInstanceId)
    }

    fun onPutFieldFloat(objId: Long, clazz: String, field: String, type: String, value: Float) {
        onPutField(objId, clazz, field, type, invalidInstanceId)
    }

    fun onPutFieldLong(objId: Long, clazz: String, field: String, type: String, value: Long) {
        onPutField(objId, clazz, field, type, invalidInstanceId)
    }

    fun onPutFieldDouble(objId: Long, clazz: String, field: String, type: String, value: Double) {
        onPutField(objId, clazz, field, type, invalidInstanceId)
    }

    fun onPutFieldObject(objId: Long, clazz: String, field: String, type: String, value: Any) {
        val valId: Long = try {
            value::class.java.getDeclaredField(instanceIdFieldName).run {
                isAccessible = true
                getLong(value)
            }
        } catch (e: NoSuchFieldException) {
            0L
        }

        onPutField(objId, clazz, field, type, valId)
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