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
            Message.MessageType.PUTFIELD -> putField(msg.putField, msg.threadId)
            Message.MessageType.STORE -> store(msg.store, msg.threadId)
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

    private fun defineClass(classDef: Definitions.ClassDefinition) {
        classDefinitions[classDef.name] = classDef
        println("registered ${classDef.name}")
    }

    private fun enterMethod(msg: Flow.MethodEnter, threadId: ThreadID) {
        val type = msg.class_
        val name = msg.method

        println(">>> $type:$name")

        val classDef = classDefinitions[type]
                ?: throw IllegalStateException("undefined class $type")
        val methodDef = classDef.methodsList.find { it.name == name }
                ?: throw IllegalStateException("undefined method $type:$name")

        getCurrentFrame(threadId)
                .clear()
                .definition = methodDef
    }

    private fun allocate(alloc: Allocations.Allocation, threadId: ThreadID) {
        val msg = AddHeapObject.newBuilder().apply {
            id = alloc.id
            class_ = alloc.type.tidyClassName()
        }.build()

        println("allocate ${alloc.id} in thread $threadId")

        getCurrentFrame(threadId).addEvents(EventVariant.newBuilder().apply {
            type = MessageType.ADD_HEAP_OBJECT
            addHeapObject = msg
        })

        allocationThread[alloc.id] = threadId
    }

    private fun deallocate(dealloc: Allocations.Deallocation) {
        val threadId = allocationThread[dealloc.id] ?: return

        val msg = DelHeapObject.newBuilder().apply {
            id = dealloc.id
        }.build()

        println("deallocate ${dealloc.id} from thread $threadId")

        getCurrentFrame(threadId).addEvents(EventVariant.newBuilder().apply {
            type = MessageType.DEL_HEAP_OBJECT
            delHeapObject = msg
        })
    }

    private fun getField(getField: Access.GetField, threadId: ThreadID) {
//        println("GETFIELD ${getField.field}")
    }

    private fun putField(putField: Access.PutField, threadId: ThreadID) {
//        println("PUTFIELD ${putField.id}'s ${putField.field} set to ${putField.valueId}")
    }

    private fun store(store: Access.Store, threadId: ThreadID) {
//        println("STORE ${store.index}")
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