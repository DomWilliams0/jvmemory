package ms.domwillia.jvmemory.parser

import ms.domwillia.jvmemory.protobuf.*

open class Processor(val threadId: Long) {

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

    open fun enterMethod(message: Flow.MethodEnter) {}
    open fun exitMethod(message: Flow.MethodExit) {}
    open fun defineClass(message: Definitions.ClassDefinition) {}
    open fun allocate(message: Allocations.Allocation) {}
    open fun deallocate(message: Allocations.Deallocation) {}
    open fun getField(message: Access.GetField) {}
    open fun putField(message: Access.PutField) {}
    open fun store(message: Access.Store) {}
    open fun load(message: Access.Load) {}

}