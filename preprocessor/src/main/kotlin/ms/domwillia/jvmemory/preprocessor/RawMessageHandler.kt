package ms.domwillia.jvmemory.preprocessor

import ms.domwillia.jvmemory.preprocessor.protobuf.Event
import ms.domwillia.jvmemory.protobuf.*

class RawMessageHandler {
    private var methodCall = Event.MethodCall.newBuilder()
    private var classDefinitions = mutableMapOf<String, Definitions.ClassDefinition>()

    fun handle(msg: Message.Variant): Event.MethodCall? {
        when (msg.type) {
            Message.MessageType.ALLOC -> allocate(msg.alloc)
            Message.MessageType.DEALLOC -> deallocate(msg.dealloc)
            Message.MessageType.GETFIELD -> getField(msg.getField)
            Message.MessageType.PUTFIELD -> putField(msg.putField)
            Message.MessageType.STORE -> store(msg.store)
            Message.MessageType.LOAD -> load(msg.load)
            Message.MessageType.CLASS_DEF -> defineClass(msg.classDef)
            Message.MessageType.METHOD_ENTER -> enterMethod(msg.methodEnter)

            Message.MessageType.METHOD_EXIT -> {
                return methodCall.build()
            }

            else -> throw IllegalStateException("bad message type: $msg")
        }

        return null
    }

    private fun defineClass(classDef: Definitions.ClassDefinition) {
        classDefinitions[classDef.name] = classDef
        println("registered ${classDef.name}")
    }

    private fun enterMethod(msg: Flow.MethodEnter) {
        val type = msg.class_
        val name = msg.method

        println(">>> $type:$name")

        val classDef = classDefinitions[type]
                ?: throw IllegalStateException("undefined class $type")
        val methodDef = classDef.methodsList.find { it.name == name }
                ?: throw IllegalStateException("undefined method $type:$name")

        methodCall.clear()
        methodCall.definition = methodDef
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