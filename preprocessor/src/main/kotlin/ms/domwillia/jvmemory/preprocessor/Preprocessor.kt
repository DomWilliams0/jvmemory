package ms.domwillia.jvmemory.preprocessor

import ms.domwillia.jvmemory.preprocessor.protobuf.Event
import ms.domwillia.jvmemory.protobuf.Message
import java.io.File
import java.nio.file.Files

typealias ThreadID = Long
typealias ObjectID = Long

class Preprocessor {

    private val handler = RawMessageHandler()

    private val events = mutableMapOf<ThreadID, MutableCollection<Event.EventVariant>>()

    private fun handle(msg: Message.Variant) {
        handler.handle(msg).forEach { (tid, event) ->
            events.computeIfAbsent(tid, { mutableListOf() }).add(event)
        }
    }

    private fun process() {
        // TODO have fun here
    }

    private fun flushToDisk(eventsLoader: EventsLoader) {
        events.keys.forEach { tid ->
            eventsLoader.getFileForThread(tid).outputStream().buffered().use { stream ->
                events[tid]!!.forEach { it.writeDelimitedTo(stream) }
            }
        }

        eventsLoader.definitionFile.outputStream().use { stream ->
            handler.loadedClassDefinitions.forEach { it.writeDelimitedTo(stream) }
        }
    }

    companion object {
        /**
         * @param inputLogPath Raw event log from monitor agent
         * @param outputDirPath Directory to write out processed visualisation events
         *                      A file will be created for each thread
         */
        fun runPreprocessor(inputLogPath: File, outputDirPath: File): EventsLoader {
            val outDirPath = outputDirPath.toPath()
            when {
                !inputLogPath.isFile -> throw IllegalArgumentException("Bad log file path given")
                outputDirPath.exists() -> {
                    if (!outputDirPath.isDirectory) throw IllegalArgumentException("Bad output directory: it is not a directory!")
                    outputDirPath.listFiles().forEach { it.delete() }
                }
                else -> Files.createDirectories(outDirPath)
            }
            val eventsLoader = EventsLoader(outDirPath.toFile())
            val proc = Preprocessor()

            readMessages(inputLogPath).forEach(proc::handle)

            proc.process()
            proc.flushToDisk(eventsLoader)

            return eventsLoader
        }

        fun readMessages(inputLogPath: File): Sequence<Message.Variant> {
            val stream = inputLogPath.inputStream()
            return generateSequence { Message.Variant.parseDelimitedFrom(stream) }
        }
    }

}