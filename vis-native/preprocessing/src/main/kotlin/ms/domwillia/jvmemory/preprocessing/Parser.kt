package ms.domwillia.jvmemory.preprocessing

import ms.domwillia.jvmemory.protobuf.Message
import java.io.File

fun parseLog(path: String, processor: (Long) -> Processor) {
    val f = File(path)
    if (!f.isFile)
        throw IllegalArgumentException("Bad log file path given")

    val processors = mutableMapOf<Long, Processor>()

    val messages = run {
        val stream = f.inputStream()
        generateSequence { Message.Variant.parseDelimitedFrom(stream) }
    }
    for (m in messages) {
        val proc = processors.getOrPut(m.threadId, { processor(m.threadId) })
        proc.handle(m)
    }

    processors.forEach { it.value.finish() }
}
