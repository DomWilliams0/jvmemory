package ms.domwillia.jvmemory.monitor

object Tagger {

    @JvmStatic
    external fun <X> allocateTag(o: Any, expectedClass: Class<out X>): Long

    @JvmStatic
    external fun getTag(o: Any): Long

    @JvmStatic
    external fun flushQueuedDeallocations()
    // TODO actually call this
}
