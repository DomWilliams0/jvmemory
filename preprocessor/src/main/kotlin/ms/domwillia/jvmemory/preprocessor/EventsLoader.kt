package ms.domwillia.jvmemory.preprocessor

import ms.domwillia.jvmemory.preprocessor.protobuf.Event
import java.io.BufferedInputStream
import java.io.File


class EventsLoader(private val outputDir: File) {

    val threads: Collection<ThreadID>
        get() = outputDir.listFiles().mapNotNull(this::getThreadIDFromPath)

    fun getRawEventsForThread(threadId: ThreadID): BufferedInputStream? {
        val path = getFileForThread(threadId)
        return if (path.isFile)
            path.inputStream().buffered()
        else {
            null
        }
    }

    fun getEventsForThread(threadId: ThreadID): Sequence<Event.EventVariant> =
            getRawEventsForThread(threadId)?.let { stream ->
                generateSequence { Event.EventVariant.parseDelimitedFrom(stream) }
            } ?: emptySequence()

    val rawDefinitions: BufferedInputStream?
        get() = if (definitionFile.isFile) definitionFile.inputStream().buffered() else null


//    val definitions: ArrayList<Definitions.ClassDefinition>

    internal val definitionFile: File
        get() = File(outputDir, "jvmemory-definitions.log")

    fun getFileForThread(threadID: ThreadID) = File(outputDir, "jvmemory-thread-$threadID.log")

    fun getThreadIDFromPath(file: File): ThreadID? =
            matchPat.matchEntire(file.name)?.groupValues?.get(1)?.toLong()

    companion object {
        private val matchPat = Regex("jvmemory-thread-(\\d+).log")
    }
}