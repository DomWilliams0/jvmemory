package ms.domwillia.jvmemory.preprocessor

import ms.domwillia.jvmemory.preprocessor.protobuf.Event
import ms.domwillia.jvmemory.protobuf.Definitions
import java.io.File


class EventsLoader(private val outputDir: File) {

    val threads: Collection<ThreadID>
        get() = outputDir.listFiles().mapNotNull(this::getThreadIDFromPath)

    fun getEventsForThread(threadId: ThreadID): Sequence<Event.EventVariant> {
        val path = getFileForThread(threadId)
        return if (!path.isFile)
            emptySequence()
        else {
            val stream = path.inputStream().buffered()
            generateSequence { Event.EventVariant.parseDelimitedFrom(stream) }
        }
    }

    val definitions: ArrayList<Definitions.ClassDefinition>
        get() {
            val out = ArrayList<Definitions.ClassDefinition>()
            with(definitionFile.inputStream().buffered()) {
                return generateSequence { Definitions.ClassDefinition.parseDelimitedFrom(this) }
                        .toCollection(out)
            }
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