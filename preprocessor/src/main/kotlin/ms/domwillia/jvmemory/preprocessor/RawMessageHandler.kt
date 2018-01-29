package ms.domwillia.jvmemory.preprocessor

import ms.domwillia.jvmemory.preprocessor.protobuf.Event.*
import ms.domwillia.jvmemory.protobuf.*
import java.util.*

class RawMessageHandler {
    private var classDefinitions = mutableMapOf<String, Definitions.ClassDefinition>()
    private var allocationThread = mutableMapOf<ObjectID, ThreadID>()
    private var callstacks = mutableMapOf<ThreadID, Stack<PushMethodFrame>>()

    fun handle(msg: Message.Variant): Pair<ThreadID, EventVariant>? = when (msg.type) {
        Message.MessageType.ALLOC_OBJECT -> allocateObject(msg.allocObject, msg.threadId)
        Message.MessageType.DEALLOC -> deallocate(msg.dealloc)

        Message.MessageType.GETFIELD -> getField(msg.getField, msg.threadId)
        Message.MessageType.PUTFIELD_OBJECT -> putFieldObject(msg.putFieldObject, msg.threadId)
        Message.MessageType.PUTFIELD_PRIMITIVE -> putFieldPrimitive(msg.putFieldPrimitive, msg.threadId)
        Message.MessageType.STORE_OBJECT -> storeObject(msg.storeObject, msg.threadId)
        Message.MessageType.STORE_PRIMITIVE -> storePrimitive(msg.storePrimitive, msg.threadId)
        Message.MessageType.LOAD -> load(msg.load, msg.threadId)
        Message.MessageType.CLASS_DEF -> {
            defineClass(msg.classDef)
            null
        }

        Message.MessageType.METHOD_ENTER -> enterMethod(msg.methodEnter, msg.threadId)
        Message.MessageType.METHOD_EXIT -> exitMethod(msg.threadId)

        else -> throw IllegalArgumentException("bad message type: $msg")
    }

    /**
     * Helper
     */
    private fun createEvent(
            threadId: ThreadID,
            messageType: EventType,
            initialiser: (EventVariant.Builder) -> Unit): Pair<ThreadID, EventVariant> {

        val event = EventVariant.newBuilder().apply {
            type = messageType
            initialiser(this)
        }.build()
        return Pair(threadId, event)
    }

    private fun defineClass(classDef: Definitions.ClassDefinition) {
        classDefinitions[classDef.name] = classDef
        println("registered ${classDef.name}")
    }

    private fun enterMethod(msg: Flow.MethodEnter, threadId: ThreadID): Pair<ThreadID, EventVariant> {
        val type = msg.class_
        val methodName = msg.method

        val classDef = classDefinitions[type]
                ?: throw IllegalStateException("undefined class $type")
        val methodDef = classDef.methodsList.find { it.name == methodName }
                ?: throw IllegalStateException("undefined method $type:$methodName")

        val frame = PushMethodFrame.newBuilder().apply {
            owningClass = classDef.name
            name = methodDef.name
            signature = methodDef.signature
        }.build()

        callstacks.computeIfAbsent(threadId, { Stack() }).push(frame)

        return createEvent(threadId, EventType.PUSH_METHOD_FRAME, {
            it.pushMethodFrame = frame
        })
    }

    private fun exitMethod(threadId: ThreadID): Pair<ThreadID, EventVariant> {
        callstacks[threadId]!!.pop()

        return createEvent(threadId, EventType.POP_METHOD_FRAME, {})
    }

    private fun allocateObject(alloc: Allocations.AllocationObject, threadId: ThreadID): Pair<ThreadID, EventVariant> {
        allocationThread[alloc.id] = threadId

        return createEvent(threadId, EventType.ADD_HEAP_OBJECT, {
            it.addHeapObject = AddHeapObject.newBuilder().apply {
                id = alloc.id
                class_ = alloc.type.tidyClassName()
            }.build()
        })
    }

    private fun deallocate(dealloc: Allocations.Deallocation): Pair<ThreadID, EventVariant> {
        val threadId = allocationThread[dealloc.id] ?:
                throw IllegalArgumentException("found deallocation of unknown object ${dealloc.id}")

        return createEvent(threadId, EventType.DEL_HEAP_OBJECT, {
            it.delHeapObject = DelHeapObject.newBuilder().apply {
                id = dealloc.id
            }.build()
        })
    }

    private fun getField(getField: Access.GetField, threadId: ThreadID): Pair<ThreadID, EventVariant> {
        return createEvent(threadId, EventType.SHOW_HEAP_OBJECT_ACCESS, {
            it.showHeapObjectAccess = ShowHeapObjectAccess.newBuilder().apply {
                objId = getField.id
                fieldName = getField.field
            }.build()
        })
    }

    private fun putFieldObject(putFieldObject: Access.PutFieldObject, threadId: ThreadID): Pair<ThreadID, EventVariant> {
        return createEvent(threadId, EventType.SET_INTER_HEAP_LINK, {
            it.setInterHeapLink = SetInterHeapLink.newBuilder().apply {
                srcId = putFieldObject.id
                dstId = putFieldObject.valueId // may be 0/null
                fieldName = putFieldObject.field
            }.build()
        })
    }

    private fun putFieldPrimitive(putFieldPrimitive: Access.PutFieldPrimitive, threadId: ThreadID): Pair<ThreadID, EventVariant> {
        return createEvent(threadId, EventType.SHOW_HEAP_OBJECT_ACCESS, {
            it.showHeapObjectAccess = ShowHeapObjectAccess.newBuilder().apply {
                objId = putFieldPrimitive.id
            }.build()
        })
    }

    private fun storeObject(storeObject: Access.StoreObject, threadId: ThreadID): Pair<ThreadID, EventVariant> {
        return createEvent(threadId, EventType.SET_LOCAL_VAR_LINK, {
            it.setLocalVarLink = SetLocalVarLink.newBuilder().apply {
                varIndex = storeObject.index
                dstId = storeObject.valueId
            }.build()
        })
    }

    private fun storePrimitive(storePrimitive: Access.StorePrimitive, threadId: ThreadID): Pair<ThreadID, EventVariant> {
        return createEvent(threadId, EventType.SHOW_LOCAL_VAR_ACCESS, {
            it.showLocalVarAccess = ShowLocalVarAccess.newBuilder().apply {
                varIndex = storePrimitive.index
            }.build()
        })
    }

    private fun load(load: Access.Load, threadId: ThreadID): Pair<ThreadID, EventVariant> {
        return createEvent(threadId, EventType.SHOW_LOCAL_VAR_ACCESS, {
            it.showLocalVarAccess = ShowLocalVarAccess.newBuilder().apply {
                varIndex = load.index
            }.build()
        })
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

    internal val loadedClassDefinitions: Collection<Definitions.ClassDefinition>
        get() = this.classDefinitions.values
}