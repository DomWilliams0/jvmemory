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

    private fun getOutputStream(threadId: ThreadID): BufferedOutputStream {
        val file = events.getFileForThread(threadId)
        return threadOutputs.computeIfAbsent(threadId, { file.outputStream().buffered() })
    }

    private fun handle(msg: Message.Variant) {
        handler.handle(msg)?.let { (threadId, event) ->
            event.writeDelimitedTo(getOutputStream(threadId))
        }
    }

    private fun finish() {
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

            val messages = run {
                val stream = inputLogPath.inputStream()
                generateSequence { Message.Variant.parseDelimitedFrom(stream) }
            }

            for (m in messages) {
                proc.handle(m)
            }

            proc.finish()

            return proc.events
        }
    }

}