package ms.domwillia.jvmemory.monitor

object Tagger {

    /**
     * Generates a new tag for a new object
     */
    external fun allocateTag(o: Any): Long

    /**
     * Assigns the last allocated tag to this object, without allocating a new one
     * Handy for super classes
     */
    @JvmStatic
    external fun assignCurrentTag(o: Any)

    /**
     * Gets the tag of the current
     */
    external fun getTag(o: Any): Long
}
