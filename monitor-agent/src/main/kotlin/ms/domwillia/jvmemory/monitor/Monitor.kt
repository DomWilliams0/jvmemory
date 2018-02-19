package ms.domwillia.jvmemory.monitor

@Suppress("unused")
object Monitor {
    @JvmStatic external fun setProgramInProgress(running: Boolean)

    @JvmStatic external fun onClassLoad(starting: Boolean)

    /**
     * To be called from within java/lang/Object's constructor only
     */
    @JvmStatic external fun allocateTag(clazz: String, o: Any)

    @JvmStatic external fun allocateTagForArray(size: Int, a: Any, clazz: String)

    @JvmStatic external fun allocateTagForMultiDimArray(a: Any, dims: Int, clazz: String)

    @JvmStatic external fun allocateTagForConstant(o: Any, clazz: String)

    @JvmStatic external fun getTag(o: Any): Long

    @JvmStatic external fun enterMethod(clazz: String, method: String)

    @JvmStatic external fun exitMethod()

    @JvmStatic external fun primeForSystemMethod()

    @JvmStatic external fun enterSystemMethod(method: String)

    @JvmStatic external fun onGetField(obj: Any, field: String)

    /**
     * @param field The name of the field being set
     */
    @JvmStatic external fun onPutFieldObject(obj: Any, value: Any?, field: String)

    /**
     * @param field The name of the field being set
     */
    @JvmStatic external fun onPutFieldPrimitive(obj: Any, field: String)

    @JvmStatic external fun onStoreLocalVarObject(value: Any, index: Int)

    /**
     * @param index The local variable index
     */
    @JvmStatic external fun onStoreLocalVarPrimitive(index: Int)

    @JvmStatic external fun onStoreObjectInArray(value: Any, array: Any, index: Int)

    @JvmStatic external fun onStorePrimitiveInArray(array: Any, index: Int)

    @JvmStatic external fun onLoadFromArray(array: Any, index: Int)

    /**
     * @param index The local variable index
     */
    @JvmStatic external fun onLoadLocalVar(index: Int)

    @JvmStatic external fun onDefineClass(def: ByteArray)
}
