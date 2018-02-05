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

class Preprocessor(outputDirPath: File) {

    private val handler = RawMessageHandler()
    private val eventsLoader = EventsLoader(outputDirPath)
    private val emittedEvents = mutableMapOf<ThreadID, MutableList<Event.EventVariant.Builder>>()

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
            emitted.forEach { (_, eventBuilder) ->
                if (flag == EventFlag.CONTINUOUS)
                    eventBuilder.continuous = true
            }

            return emitted
        }

        val it = messages.iterator()
        var index = 0
        while (it.hasNext()) {
            val msg = it.next()
            it.remove()
            handle(index++, msg).forEach { (tid, event) ->
                emittedEvents.computeIfAbsent(tid, { mutableListOf() }).add(event)
            }
        }

        flags.clear()
    }

    private fun postprocess() {
        fun isMergeableAccess(e: Event.EventVariant.Builder) =
                (e.type == Event.EventType.SHOW_HEAP_OBJECT_ACCESS && !e.showHeapObjectAccess.read) ||
                        (e.type == Event.EventType.SHOW_LOCAL_VAR_ACCESS && !e.showLocalVarAccess.read)

        emittedEvents.forEach { (_, events) ->
            for (i in 1..events.lastIndex) {
                // combine consecutive accesses
                val e1 = events[i - 1]
                val e2 = events[i]
                if (isMergeableAccess(e1) && isMergeableAccess(e2)) {
                    e1.continuous = true
                    e2.continuous = true
                }
            }
        }
    }

    private fun writeOut() {
        emittedEvents.forEach { tid, events ->
            eventsLoader.getFileForThread(tid).outputStream().buffered().use { stream ->
                events.forEach { it.build().writeDelimitedTo(stream) }
            }
        }
        eventsLoader.definitionFile.outputStream().use { stream ->
            handler.loadedClassDefinitions.forEach { it.writeDelimitedTo(stream) }
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

            val proc = Preprocessor(outputDirPath)
            val messages = readMessages(inputLogPath)

            proc.preprocess(messages)
            proc.process(messages)
            proc.postprocess()
            proc.writeOut()

            return proc.eventsLoader
        }

        fun readMessages(inputLogPath: File): MutableList<Message.Variant> {
            inputLogPath.inputStream().use {
                return generateSequence { Message.Variant.parseDelimitedFrom(it) }.toMutableList()
            }
        }
    }

}