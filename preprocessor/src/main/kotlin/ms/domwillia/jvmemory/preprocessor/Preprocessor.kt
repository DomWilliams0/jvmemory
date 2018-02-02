package ms.domwillia.jvmemory.preprocessor

import ms.domwillia.jvmemory.protobuf.Message
import java.io.BufferedOutputStream
import java.io.File
import java.nio.file.Files

typealias ThreadID = Long
typealias ObjectID = Long

enum class EventFlag {
    CONTINUOUS, REMOVED
}

class Preprocessor(outputDirPath: File) {

    private val handler = RawMessageHandler()
    private val threadOutputs = mutableMapOf<ThreadID, BufferedOutputStream>()
    private val events = EventsLoader(outputDirPath)

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

    private fun handle(index: Int, msg: Message.Variant) {
        val flag = flags.getOrElse(index, { null })

        handler.handle(msg).forEach { (threadId, event) ->
            when (flag) {
                EventFlag.CONTINUOUS -> event.continuous = true
                EventFlag.REMOVED -> return@forEach
            }

            val stream = threadOutputs.computeIfAbsent(threadId, {
                events.getFileForThread(threadId).outputStream().buffered()
            })
            event.build().writeDelimitedTo(stream)
        }
    }

    private fun finish() {
        flags.clear()
        threadOutputs.values.forEach(BufferedOutputStream::close)

        with(events.definitionFile.outputStream()) {
            for (classDef in handler.loadedClassDefinitions) {
                classDef.writeDelimitedTo(this)
            }
        }
    }

    companion object {
        /**
         * @param inputLogPath Raw event log from monitor agent
         * @param outputDirPath Directory to write out processed visualisation events
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

            messages.forEachIndexed(proc::handle)

            proc.finish()

            return proc.events
        }

        fun readMessages(inputLogPath: File): List<Message.Variant> {
            inputLogPath.inputStream().use {
                return generateSequence { Message.Variant.parseDelimitedFrom(it) }.toList()
            }
        }
    }

}