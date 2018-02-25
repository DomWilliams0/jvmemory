package ms.domwillia.jvmemory.preprocessor

import ms.domwillia.jvmemory.preprocessor.protobuf.Event
import ms.domwillia.jvmemory.protobuf.Message
import java.io.File
import java.nio.file.Files

typealias ThreadID = Long
typealias ObjectID = Long

enum class EventFlag {
    CONTINUOUS, REMOVED
}

class Preprocessor(
        definitions: ClassDefinitions,
        private val loader: EventsLoader,
        private val threadId: ThreadID
) {

    private val handler = RawMessageHandler(definitions)
    private val emittedEvents = mutableListOf<Event.EventVariant.Builder>()
    private val flags = hashMapOf<Int, EventFlag>()

    private fun preprocess(messages: List<Message.Variant>) {
        // combine multi dimensional arrays
        messages.forEachIndexed { i, msg ->
            if (msg.type != Message.MessageType.ALLOC_ARRAY)
                return@forEachIndexed

            val srcArray = msg.allocArray.srcArrayId
            if (srcArray != 0L) {
                flags[i] = EventFlag.CONTINUOUS

                if (i > 0) {
                    val previous = messages[i - 1]
                    if (previous.type == Message.MessageType.ALLOC_ARRAY &&
                            srcArray == previous.allocArray.id &&
                            previous.allocArray.srcArrayId == 0L)
                        flags[i - 1] = EventFlag.CONTINUOUS
                }
            }
        }

        // remove empty method calls (enter immediately followed by exit)
        messages.forEachIndexed { i, msg ->
            if (msg.type == Message.MessageType.METHOD_EXIT &&
                    i > 0 &&
                    messages[i - 1].type == Message.MessageType.METHOD_ENTER) {
                flags[i] = EventFlag.REMOVED
                flags[i - 1] = EventFlag.REMOVED
            }
        }
    }

    private fun process(messages: MutableList<Message.Variant>) {
        fun handle(index: Int, msg: Message.Variant): EmittedEvents {
            val flag = flags.getOrElse(index, { null })

            if (flag == EventFlag.REMOVED)
                return emptyEmittedEvents

            val emitted = handler.handle(msg)
            emitted.forEach { it ->
                if (flag == EventFlag.CONTINUOUS)
                    it.continuous = true
            }

            return emitted
        }

        val it = messages.iterator()
        var index = 0
        while (it.hasNext()) {
            val msg = it.next()
            it.remove()
            handle(index++, msg).forEach { emittedEvents += it }
        }

        flags.clear()
    }

    private fun postprocess() {
        fun isMergeableAccess(e: Event.EventVariant.Builder) =
                (e.type == Event.EventType.SHOW_HEAP_OBJECT_ACCESS && !e.showHeapObjectAccess.read) ||
                        (e.type == Event.EventType.SHOW_LOCAL_VAR_ACCESS && !e.showLocalVarAccess.read)

        for (i in 1..emittedEvents.lastIndex) {
                // combine consecutive accesses
            val e1 = emittedEvents[i - 1]
            val e2 = emittedEvents[i]
                if (isMergeableAccess(e1) && isMergeableAccess(e2)) {
                    e1.continuous = true
                    e2.continuous = true
                }
            }
    }

    private fun writeOut() {
        loader.getFileForThread(threadId).outputStream().buffered().use { stream ->
            emittedEvents.forEach { it.build().writeDelimitedTo(stream) }
        }
    }

    companion object {
        /**
         * @param inputLogPath Raw event log from monitor agent
         * @param outputDirPath Directory to write out processed visualisation eventsLoader
         *                      A file will be created for each thread
         */
        fun runPreprocessor(
                inputLogPath: File,
                outputDirPath: File
        ): EventsLoader {
            val outDirPath = outputDirPath.toPath()
            if (!inputLogPath.isFile)
                throw IllegalArgumentException("Bad log file path given")
            if (outputDirPath.exists()) {
                if (!outputDirPath.isDirectory) throw IllegalArgumentException("Bad output directory: it is not a directory!")
                outputDirPath.listFiles().forEach { it.delete() }
            } else {
                Files.createDirectories(outDirPath)
            }

            val loader = EventsLoader(outputDirPath)

            // load definitions first, regardless of thread
            val definitions = getAllDefinitions(inputLogPath)

            val threads = getAllThreadIds(inputLogPath)
            println("threads: $threads")
            for (tid in threads) {
                println("processing thread $tid")
                val messages = readMessagesLazily(inputLogPath)
                        .filter { it.threadId == tid || it.threadId == 0L }
                        .toMutableList()
                val proc = Preprocessor(definitions, loader, tid)

                proc.preprocess(messages)
                proc.process(messages)
                proc.postprocess()
                proc.writeOut()
            }

            // dump definitions
            loader.definitionFile.outputStream().use { stream ->
                definitions.values.forEach { it.writeDelimitedTo(stream) }
            }

            return loader
        }

        private fun getAllDefinitions(inputLogPath: File): ClassDefinitions =
                readMessagesLazily(inputLogPath)
                        .filter { it.type == Message.MessageType.CLASS_DEF }
                        .associate { it.classDef.name to it.classDef }

        private fun getAllThreadIds(inputLogPath: File): Set<ThreadID> =
                readMessagesLazily(inputLogPath)
                        .map { it.threadId }
                        .toCollection(LinkedHashSet())
                        .apply { remove(0L) }

        fun readMessagesLazily(inputLogPath: File): Sequence<Message.Variant> {
            val stream = inputLogPath.inputStream()
            return generateSequence {
                Message.Variant.parseDelimitedFrom(stream).let {
                    if (it == null) stream.close(); it
                }
            }
        }
    }

}