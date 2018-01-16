package ms.domwillia.jvmemory.preprocessor

import ms.domwillia.jvmemory.preprocessor.protobuf.Visualisation.*
import ms.domwillia.jvmemory.protobuf.*

class RawMessageHandler {
    private var activeFrames = mutableMapOf<ThreadID, MethodFrame.Builder>()
    private var classDefinitions = mutableMapOf<String, Definitions.ClassDefinition>()
    private var allocationThread = mutableMapOf<ObjectID, ThreadID>()

    fun handle(msg: Message.Variant): Pair<ThreadID, MethodFrame>? {
        when (msg.type) {
            Message.MessageType.ALLOC -> allocate(msg.alloc, msg.threadId)
            Message.MessageType.DEALLOC -> deallocate(msg.dealloc)

            Message.MessageType.GETFIELD -> getField(msg.getField, msg.threadId)
            Message.MessageType.PUTFIELD_OBJECT -> putFieldObject(msg.putFieldObject, msg.threadId)
            Message.MessageType.PUTFIELD_PRIMITIVE -> putFieldPrimitive(msg.putFieldPrimitive, msg.threadId)
            Message.MessageType.STORE_OBJECT -> storeObject(msg.storeObject, msg.threadId)
            Message.MessageType.STORE_PRIMITIVE -> storePrimitive(msg.storePrimitive, msg.threadId)
            Message.MessageType.LOAD -> load(msg.load, msg.threadId)
            Message.MessageType.CLASS_DEF -> defineClass(msg.classDef)

            Message.MessageType.METHOD_ENTER -> enterMethod(msg.methodEnter, msg.threadId)
            Message.MessageType.METHOD_EXIT ->
                return Pair(msg.threadId, getCurrentFrame(msg.threadId).build())

            else -> throw IllegalStateException("bad message type: $msg")
        }

        return null
    }


    private fun getCurrentFrame(threadId: ThreadID): MethodFrame.Builder =
            activeFrames.computeIfAbsent(threadId, { MethodFrame.newBuilder() })

    /**
     * Helper
     */
    private fun emitEvent(
            threadId: ThreadID,
            messageType: MessageType,
            initialiser: (EventVariant.Builder) -> Unit) {

        val event = EventVariant.newBuilder().apply {
            type = messageType
            initialiser(this)
        }

        println("event: $event\n====")
        getCurrentFrame(threadId).addEvents(event)
    }

    private fun defineClass(classDef: Definitions.ClassDefinition) {
        classDefinitions[classDef.name] = classDef
        println("registered ${classDef.name}")
    }

    private fun enterMethod(msg: Flow.MethodEnter, threadId: ThreadID) {
        val type = msg.class_
        val name = msg.method

        val classDef = classDefinitions[type]
                ?: throw IllegalStateException("undefined class $type")
        val methodDef = classDef.methodsList.find { it.name == name }
                ?: throw IllegalStateException("undefined method $type:$name")

        getCurrentFrame(threadId)
                .clear()
                .definition = methodDef
    }

    private fun allocate(alloc: Allocations.Allocation, threadId: ThreadID) {

        emitEvent(threadId, MessageType.ADD_HEAP_OBJECT, {
            it.addHeapObject = AddHeapObject.newBuilder().apply {
                id = alloc.id
                class_ = alloc.type.tidyClassName()
            }.build()
        })

        allocationThread[alloc.id] = threadId
    }

    private fun deallocate(dealloc: Allocations.Deallocation) {
        val threadId = allocationThread[dealloc.id] ?: return

        emitEvent(threadId, MessageType.DEL_HEAP_OBJECT, {
            it.delHeapObject = DelHeapObject.newBuilder().apply {
                id = dealloc.id
            }.build()
        })
    }

    private fun getField(getField: Access.GetField, threadId: ThreadID) {
//        println("GETFIELD ${getField.field}")
    }

    private fun putFieldObject(putFieldObject: Access.PutFieldObject, threadId: ThreadID) {
        emitEvent(threadId, MessageType.SET_LINK, {
            it.setLink = SetLink.newBuilder().apply {
                srcId = putFieldObject.id
                dstId = putFieldObject.valueId // may be 0/null
                name = putFieldObject.field
            }.build()
        })
    }

    private fun putFieldPrimitive(putFieldPrimitive: Access.PutFieldPrimitive, threadId: ThreadID) {
    }

    private fun storeObject(storeObject: Access.StoreObject, threadId: ThreadID) {

    }

    private fun storePrimitive(storePrimitive: Access.StorePrimitive, threadId: ThreadID) {

    }

    private fun load(load: Access.Load, threadId: ThreadID) {
//        println("LOAD ${load.index}")
    }

    /**
     * Converts class names of the formats "Lcom/package/Class;" and "com/package/Class"
     * to "com.package.Class"
     */
    private fun String.tidyClassName(): String {
        val name = StringBuilder(if (this[lastIndex] == ';') {
            subSequence(1, lastIndex)
        } else {
            this
        })
        name.forEachIndexed { i, c -> if (c == '/') name.setCharAt(i, '.') }
        return name.toString()
    }
}