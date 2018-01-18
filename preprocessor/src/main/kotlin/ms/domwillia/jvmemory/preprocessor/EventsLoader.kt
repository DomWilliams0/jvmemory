package ms.domwillia.jvmemory.preprocessor

import ms.domwillia.jvmemory.preprocessor.protobuf.Event
import java.io.File


class EventsLoader(private val outputDir: File) {

    val threads: Collection<ThreadID>
        get() = outputDir.listFiles()
                .map(this::getThreadIDFromPath)
                .mapNotNull { it!! }

    fun getMethodFramesForThread(threadId: ThreadID): Sequence<Event.MethodFrame> {
        val path = getFileForThread(threadId)
        return if (!path.isFile)
            emptySequence()
        else {
            val stream = path.inputStream()
            generateSequence { Event.MethodFrame.parseDelimitedFrom(stream) }
        }
    }

    fun getFileForThread(threadID: ThreadID) = File(outputDir, "jvmemory-thread-$threadID.log")

    fun getThreadIDFromPath(file: File): ThreadID? =
            matchPat.matchEntire(file.name)?.groupValues?.get(1)?.toLong()

    companion object {
        private val matchPat = Regex("jvmemory-thread-(\\d+).log")
    }
}