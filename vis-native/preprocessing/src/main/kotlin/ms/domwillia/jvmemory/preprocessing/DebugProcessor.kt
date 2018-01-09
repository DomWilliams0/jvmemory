package ms.domwillia.jvmemory.preprocessing

import ms.domwillia.jvmemory.protobuf.Access
import ms.domwillia.jvmemory.protobuf.Allocations
import ms.domwillia.jvmemory.protobuf.Definitions
import ms.domwillia.jvmemory.protobuf.Flow

class DebugProcessor(threadId: Long) : Processor(threadId) {
    override fun defineClass(message: Definitions.ClassDefinition) {
        val fields = message.fieldsList
                .dropLast(1)
                .fold("", { acc, def -> acc + def.name + ", " })
                .dropLast(2)
        println("DEFINE ${message.name} with fields: $fields")
    }

    override fun allocate(message: Allocations.Allocation) {
        println("ALLOCATE ${message.type} ${message.id}")
    }

    override fun deallocate(message: Allocations.Deallocation) {
        println("DEALLOCATE ${message.id}")
    }

    override fun getField(message: Access.GetField) {
        println("GETFIELD ${message.field}")
    }

    override fun putField(message: Access.PutField) {
        println("PUTFIELD ${message.id}'s ${message.field} set to ${message.valueId}")
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