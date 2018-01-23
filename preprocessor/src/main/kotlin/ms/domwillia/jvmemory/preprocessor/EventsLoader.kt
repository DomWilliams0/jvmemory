package ms.domwillia.jvmemory.preprocessor

import ms.domwillia.jvmemory.preprocessor.protobuf.Event
import ms.domwillia.jvmemory.protobuf.Definitions
import java.io.File


class EventsLoader(private val outputDir: File) {

    val threads: Collection<ThreadID>
        get() = outputDir.listFiles().mapNotNull(this::getThreadIDFromPath)

    fun getEventsForThread(threadId: ThreadID): List<Event.EventVariant> {
        val path = getFileForThread(threadId)
        return if (path.isFile)
            path.inputStream().buffered().use { stream ->
                generateSequence { Event.EventVariant.parseDelimitedFrom(stream) }.toList()
            }
        else {
            emptyList()
        }
    }

    val definitions: List<Definitions.ClassDefinition>
        get() = if (definitionFile.isFile) {
            definitionFile.inputStream().buffered().use { stream ->
                generateSequence { Definitions.ClassDefinition.parseDelimitedFrom(stream) }.toList()
            }
        } else {
            emptyList()
        }


    internal val definitionFile: File
        get() = File(outputDir, "jvmemory-definitions.log")

    fun getFileForThread(threadID: ThreadID) = File(outputDir, "jvmemory-thread-$threadID.log")

    fun getThreadIDFromPath(file: File): ThreadID? =
            matchPat.matchEntire(file.name)?.groupValues?.get(1)?.toLong()

    companion object {
        private val matchPat = Regex("jvmemory-thread-(\\d+).log")
    }
}