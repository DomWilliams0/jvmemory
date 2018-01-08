package ms.domwillia.jvmemory.parser

import ms.domwillia.jvmemory.protobuf.Access
import ms.domwillia.jvmemory.protobuf.Allocations
import ms.domwillia.jvmemory.protobuf.Definitions
import ms.domwillia.jvmemory.protobuf.Flow

class DebugProcessor(threadId: Long) : Processor(threadId) {
    override fun defineClass(message: Definitions.ClassDefinition) {
        println("DEFINE ${message.name}")
    }

    override fun allocate(message: Allocations.Allocation) {
        println("ALLOCATE ${message.id}")
    }

    override fun deallocate(message: Allocations.Deallocation) {
        println("DEALLOCATE ${message.id}")
    }

    override fun getField(message: Access.GetField) {
        println("GETFIELD ${message.field}")
    }

    override fun putField(message: Access.PutField) {
        println("PUTFIELD ${message.field}")
    }

    override fun store(message: Access.Store) {
        println("STORE ${message.index}")
    }

    override fun load(message: Access.Load) {
        println("LOAD ${message.index}")
    }

    override fun enterMethod(message: Flow.MethodEnter) {
        println(">>> ${message.class_}:${message.method}")
    }

    override fun exitMethod(message: Flow.MethodExit) {
        println("<<<")
    }
}