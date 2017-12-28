package ms.domwillia.jvmemory.parser

import ms.domwillia.jvmemory.protobuf.Flow

class DebugProcessor(threadId: Long) : Processor(threadId) {
    override fun enterMethod(msg: Flow.MethodEnter) {
        println(">>> ${msg.class_}:${msg.method}")
    }

    override fun exitMethod(_msg: Flow.MethodExit) {
        println("<<<")
    }
}