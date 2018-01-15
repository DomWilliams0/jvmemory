package ms.domwillia.jvmemory.preprocessor

import ms.domwillia.jvmemory.protobuf.Message
import java.io.BufferedOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

class Preprocessor(
        private val outputDirPath: File
) {

    private val handler = RawMessageHandler()
    private val threadOutputs = mutableMapOf<Long, BufferedOutputStream>()

    private fun getOutputStream(threadId: Long): BufferedOutputStream {
        val path = Paths.get(outputDirPath.path, "jvmemory-thread-$threadId.log")
        return threadOutputs.getOrElse(threadId, { BufferedOutputStream(path.toFile().outputStream()) })
    }

    private fun handle(msg: Message.Variant) {
        handler.handle(msg)

        // TODO if handler returns a new processed vis event, write to thread stream
//        val stream = getOutputStream(msg.threadId)
    }


    private fun finish() {
        threadOutputs.values.forEach(BufferedOutputStream::close)
    }


    companion object {

        /**
         * @param inputLogPath Raw event log from monitor agent
         * @param outputDirPath Directory to write out processed visualisation events
         *                      A file will be created for each thread
         */
        fun runPreprocessor(inputLogPath: File, outputDirPath: File) {
            if (!inputLogPath.isFile)
                throw IllegalArgumentException("Bad log file path given")
            if (outputDirPath.exists()) {
                if (!outputDirPath.isDirectory) throw IllegalArgumentException("Bad output directory: it is not a directory!")
                outputDirPath.listFiles().forEach { it.delete() }
            } else {
                Files.createDirectories(outputDirPath.toPath().parent)
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
        }
    }
}