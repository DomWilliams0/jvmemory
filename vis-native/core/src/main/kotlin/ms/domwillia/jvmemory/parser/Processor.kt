package ms.domwillia.jvmemory.parser

import ms.domwillia.jvmemory.protobuf.*

abstract class Processor(val threadId: Long) {

    fun handle(msg: Message.Variant) = when (msg.type) {
        Message.MessageType.METHOD_ENTER -> enterMethod(msg.methodEnter)
        Message.MessageType.METHOD_EXIT -> exitMethod(msg.methodExit)
        Message.MessageType.CLASS_DEF -> defineClass(msg.classDefinition)
        Message.MessageType.ALLOC -> allocate(msg.allocation)
        Message.MessageType.DEALLOC -> deallocate(msg.deallocation)
        Message.MessageType.GETFIELD -> getField(msg.getField)
        Message.MessageType.PUTFIELD -> putField(msg.putField)
        Message.MessageType.STORE -> store(msg.store)
        Message.MessageType.LOAD -> load(msg.load)
        else -> {
        }
    }

    abstract fun enterMethod(message: Flow.MethodEnter)
    abstract fun exitMethod(message: Flow.MethodExit)
    abstract fun defineClass(message: Definitions.ClassDefinition)
    abstract fun allocate(message: Allocations.Allocation)
    abstract fun deallocate(message: Allocations.Deallocation)
    abstract fun getField(message: Access.GetField)
    abstract fun putField(message: Access.PutField)
    abstract fun store(message: Access.Store)
    abstract fun load(message: Access.Load)

}