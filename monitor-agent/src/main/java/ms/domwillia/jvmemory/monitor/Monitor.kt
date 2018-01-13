package ms.domwillia.jvmemory.monitor

import org.objectweb.asm.Type

@Suppress("unused")
object Monitor {
    val internalName = Type.getType(Monitor::class.java).internalName!!

    /**
     * To be called from within java/lang/Object's constructor only
     */
    @JvmStatic external fun allocateTag(o: Any)

    @JvmStatic external fun getTag(o: Any): Long

    @JvmStatic external fun enterMethod(clazz: String, method: String)

    @JvmStatic external fun exitMethod()

    @JvmStatic external fun onAlloc(id: Long, type: String)

    @JvmStatic external fun onDealloc(id: Long)

    /**
     * @param objId The tag of the object whose field is being accessed
     * @param field The name of the field being accessed
     */
    @JvmStatic external fun onGetField(objId: Long, field: String)

    /**
     * @param objId The tag of the object whose field is being set
     * @param field The name of the field being set
     * @param valueId The tag of the value, or 0 if not an object
     */
    @JvmStatic external fun onPutField(objId: Long, field: String, valueId: Long)

    /**
     * @param valueId The tag of the value, or 0 if not an object
     * @param index The local variable index
     */
    @JvmStatic external fun onStoreLocalVar(valueId: Long, index: Int)

    /**
     * @param index The local variable index
     */
    @JvmStatic external fun onLoadLocalVar(index: Int)
}