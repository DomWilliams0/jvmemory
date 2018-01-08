package ms.domwillia.jvmemory.parser

import ms.domwillia.jvmemory.protobuf.Message
import java.io.File

fun parseLog(path: String) {
    val f = File(path)
    if (!f.isFile)
        throw IllegalArgumentException("Bad log file path given")

    val processors = mutableMapOf<Long, Processor>()

    val messages = run {
        val stream = f.inputStream()
        generateSequence { Message.Variant.parseDelimitedFrom(stream) }
    }
    for (m in messages) {
        val proc = processors.getOrPut(m.threadId, { DebugProcessor(m.threadId) })
        proc.handle(m)
    }
}