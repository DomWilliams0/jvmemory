package ms.domwillia.jvmemory.monitor

import org.objectweb.asm.Type

@Suppress("unused")
object Monitor {
    val instanceName = "INSTANCE" // ty kotlin for `object`
    val type = Type.getType(Monitor::class.java)
    val internalName = type.internalName
    val descriptor = type.descriptor

    private val invalidInstanceId: Long = 0

    // wrappers for Tagger methods, as calls to Tagger directly result in a NoClassDefFoundError
    @JvmStatic
    fun <X> allocateTag(o: Any, expectedClass: Class<out X>) = Tagger.allocateTag(o, expectedClass)

    @JvmStatic
    fun getTag(o: Any) = Tagger.getTag(o)

    fun enterConstructor(clazz: String) {}

    fun enterMethod(clazz: String, method: String) {}

    fun exitMethod() {}

    // called from native agent
    fun onAlloc(id: Long, type: String) {}

    // called from native agent
    fun onDealloc(id: Long) {}

    fun onGetField(objId: Long, clazz: String, field: String, type: String) {}

    fun onLoadLocalVar(index: Int) {}

    private fun onStoreLocalVar(type: String, value: Any, index: Int) {}

    private fun onPutField(objId: Long, clazz: String, field: String, type: String, valueId: Long) {}

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
        val valId = Tagger.getTag(value)
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