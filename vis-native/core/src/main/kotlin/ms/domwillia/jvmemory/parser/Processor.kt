package ms.domwillia.jvmemory.parser

import ms.domwillia.jvmemory.protobuf.Flow
import ms.domwillia.jvmemory.protobuf.Message

abstract class Processor(val threadId: Long) {

    fun handle(msg: Message.Variant) = when (msg.type) {
        Message.MessageType.METHOD_ENTER -> enterMethod(msg.methodEnter)
        Message.MessageType.METHOD_EXIT -> exitMethod(msg.methodExit)
        else -> {}
    }

    abstract fun enterMethod(msg: Flow.MethodEnter)
    abstract fun exitMethod(msg: Flow.MethodExit)

}