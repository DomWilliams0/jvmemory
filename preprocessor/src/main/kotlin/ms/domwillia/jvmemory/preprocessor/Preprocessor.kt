package ms.domwillia.jvmemory.preprocessor

import ms.domwillia.jvmemory.protobuf.Message
import java.io.BufferedOutputStream
import java.io.File
import java.nio.file.Files

typealias ThreadID = Long
typealias ObjectID = Long

class Preprocessor(outputDirPath: File) {

    private val handler = RawMessageHandler()
    private val threadOutputs = mutableMapOf<ThreadID, BufferedOutputStream>()
    private val events = EventsLoader(outputDirPath)

    private val continuousIndices = hashSetOf<Int>()

    private fun preprocess(messages: List<Message.Variant>) {
        // combine multi dimensional arrays
        messages.forEachIndexed { i, msg ->
            if (msg.type != Message.MessageType.ALLOC_ARRAY)
                return@forEachIndexed

            val srcArray = msg.allocArray.srcArrayId
            if (srcArray != 0L) {
                continuousIndices.add(i)

                if (i > 0) {
                    val previous = messages[i - 1]
                    if (previous.type == Message.MessageType.ALLOC_ARRAY &&
                            srcArray == previous.allocArray.id &&
                            previous.allocArray.srcArrayId == 0L)
                        continuousIndices.add(i - 1)
                }
            }
        }
    }

    private fun handle(index: Int, msg: Message.Variant) {
        val continuous = continuousIndices.contains(index)

        handler.handle(msg).forEach { (threadId, event) ->
            event.continuous = continuous
            val stream = threadOutputs.computeIfAbsent(threadId, {
                events.getFileForThread(threadId).outputStream().buffered()
            })
            event.build().writeDelimitedTo(stream)
        }
    }

    private fun finish() {
        continuousIndices.clear()
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