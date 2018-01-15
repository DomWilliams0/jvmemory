package ms.domwillia.jvmemory.preprocessor

import ms.domwillia.jvmemory.protobuf.*

class RawMessageHandler {
    fun handle(msg: Message.Variant) = when (msg.type) {
        Message.MessageType.METHOD_ENTER -> enterMethod(msg.methodEnter)
        Message.MessageType.METHOD_EXIT -> exitMethod(msg.methodExit)
        Message.MessageType.CLASS_DEF -> defineClass(msg.classDef)
        Message.MessageType.ALLOC -> allocate(msg.alloc)
        Message.MessageType.DEALLOC -> deallocate(msg.dealloc)
        Message.MessageType.GETFIELD -> getField(msg.getField)
        Message.MessageType.PUTFIELD -> putField(msg.putField)
        Message.MessageType.STORE -> store(msg.store)
        Message.MessageType.LOAD -> load(msg.load)
        else -> {
        }
    }

    private fun enterMethod(enter: Flow.MethodEnter) {
        println(">>> ${enter.class_}:${enter.method}")
    }

    private fun exitMethod(@Suppress("UNUSED_PARAMETER") exit: Flow.MethodExit) {
        println("<<<")
    }

    private fun defineClass(classDef: Definitions.ClassDefinition) {
        val fields = classDef.fieldsList
                .dropLast(1)
                .fold("", { acc, def -> acc + def.name + ", " })
                .dropLast(2)
        println("DEFINE ${classDef.name} with fields: $fields")
    }

    private fun allocate(alloc: Allocations.Allocation) {
        println("ALLOCATE ${alloc.type} ${alloc.id}")
    }

    private fun deallocate(dealloc: Allocations.Deallocation) {
        println("DEALLOCATE ${dealloc.id}")
    }

    private fun getField(getField: Access.GetField) {
        println("GETFIELD ${getField.field}")
    }

    private fun putField(putField: Access.PutField) {
        println("PUTFIELD ${putField.id}'s ${putField.field} set to ${putField.valueId}")
    }

    private fun store(store: Access.Store) {
        println("STORE ${store.index}")
    }

    private fun load(load: Access.Load) {
        println("LOAD ${load.index}")
    }
}