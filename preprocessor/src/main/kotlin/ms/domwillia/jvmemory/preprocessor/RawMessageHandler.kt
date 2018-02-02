package ms.domwillia.jvmemory.preprocessor

import ms.domwillia.jvmemory.preprocessor.protobuf.Event.*
import ms.domwillia.jvmemory.protobuf.*
import java.util.*

typealias EmittedEvent = Pair<ThreadID, EventVariant.Builder>
typealias EmittedEvents = List<EmittedEvent>

class RawMessageHandler {
    private var classDefinitions = mutableMapOf<String, Definitions.ClassDefinition>()
    private var allocationThread = mutableMapOf<ObjectID, ThreadID>()
    private var callstacks = mutableMapOf<ThreadID, Stack<PushMethodFrame>>()

    fun handle(msg: Message.Variant): EmittedEvents = when (msg.type) {
        Message.MessageType.ALLOC_OBJECT -> allocateObject(msg.allocObject, msg.threadId)
        Message.MessageType.ALLOC_ARRAY -> allocateArray(msg.allocArray, msg.threadId)
        Message.MessageType.DEALLOC -> deallocate(msg.dealloc)

        Message.MessageType.GETFIELD -> getField(msg.getField, msg.threadId)
        Message.MessageType.PUTFIELD_OBJECT -> putFieldObject(msg.putFieldObject, msg.threadId)
        Message.MessageType.PUTFIELD_PRIMITIVE -> putFieldPrimitive(msg.putFieldPrimitive, msg.threadId)

        Message.MessageType.LOAD -> load(msg.load, msg.threadId)
        Message.MessageType.LOAD_ARRAY -> loadFromArray(msg.loadFromArray, msg.threadId)

        Message.MessageType.STORE_OBJECT -> storeObject(msg.storeObject, msg.threadId)
        Message.MessageType.STORE_PRIMITIVE -> storePrimitive(msg.storePrimitive, msg.threadId)

        Message.MessageType.STORE_OBJECT_IN_ARRAY -> storeObjectInArray(msg.storeObjectInArray, msg.threadId)
        Message.MessageType.STORE_PRIMITIVE_IN_ARRAY -> storePrimitiveInArray(msg.storePrimitiveInArray, msg.threadId)

        Message.MessageType.CLASS_DEF -> {
            defineClass(msg.classDef)
            emptyList()
        }

        Message.MessageType.METHOD_ENTER -> enterMethod(msg.methodEnter, msg.threadId)
        Message.MessageType.METHOD_EXIT -> exitMethod(msg.threadId)

        else -> throw IllegalArgumentException("bad message type: $msg")
    }

    /**
     * Helpers
     */
    private fun createEvents(
            threadId: ThreadID,
            messageType: EventType,
            initialiser: (EventVariant.Builder) -> Unit): EmittedEvents = listOf(createEvent(threadId, messageType, initialiser))

    private fun createEvent(
            threadId: ThreadID,
            messageType: EventType,
            initialiser: (EventVariant.Builder) -> Unit): EmittedEvent {

        val event = EventVariant.newBuilder().apply {
            type = messageType
            initialiser(this)
        }
        return Pair(threadId, event)
    }

    private fun defineClass(classDef: Definitions.ClassDefinition) {
        classDefinitions[classDef.name] = classDef
    }

    private fun enterMethod(msg: Flow.MethodEnter, threadId: ThreadID): EmittedEvents {
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

        return createEvents(threadId, EventType.PUSH_METHOD_FRAME, {
            it.pushMethodFrame = frame
        })
    }

    private fun exitMethod(threadId: ThreadID): EmittedEvents {
        callstacks[threadId]!!.pop()

        return createEvents(threadId, EventType.POP_METHOD_FRAME, {})
    }

    private fun allocateObject(alloc: Allocations.AllocationObject, threadId: ThreadID): EmittedEvents {
        allocationThread[alloc.id] = threadId

        return createEvents(threadId, EventType.ADD_HEAP_OBJECT, {
            it.addHeapObject = AddHeapObject.newBuilder().apply {
                id = alloc.id
                class_ = alloc.type
            }.build()
        })
    }

    private fun allocateArray(alloc: Allocations.AllocationArray, threadId: ThreadID): EmittedEvents {
        allocationThread[alloc.id] = threadId

        val events = mutableListOf(
                createEvent(threadId, EventType.ADD_HEAP_OBJECT, {
                    it.addHeapObject = AddHeapObject.newBuilder().apply {
                        id = alloc.id
                        class_ = alloc.type
                        arraySize = alloc.size
                    }.build()
                }))

        if (alloc.hasField(Allocations.AllocationArray.getDescriptor().findFieldByNumber(Allocations.AllocationArray.SRC_ARRAY_ID_FIELD_NUMBER))) {
            events.add(
                    createEvent(threadId, EventType.SET_INTER_HEAP_LINK, {
                        it.setInterHeapLink = SetInterHeapLink.newBuilder().apply {
                            srcId = alloc.srcArrayId
                            dstId = alloc.id
                            fieldName = alloc.srcIndex.toString()
                        }.build()
                    })
            )
        }

        return events
    }

    private fun deallocate(dealloc: Allocations.Deallocation): EmittedEvents {
        val threadId = allocationThread[dealloc.id] ?:
                throw IllegalArgumentException("found deallocation of unknown object ${dealloc.id}")

        return createEvents(threadId, EventType.DEL_HEAP_OBJECT, {
            it.delHeapObject = DelHeapObject.newBuilder().apply {
                id = dealloc.id
            }.build()
        })
    }

    private fun getField(getField: Access.GetField, threadId: ThreadID): EmittedEvents {
        return createEvents(threadId, EventType.SHOW_HEAP_OBJECT_ACCESS, {
            it.showHeapObjectAccess = ShowHeapObjectAccess.newBuilder().apply {
                objId = getField.id
                fieldName = getField.field
            }.build()
        })
    }

    // removes implicitly allocated objects that haven't been explicitly added to the heap graph
    // these will typically be boxed types (Long, Integer, Byte etc.) for which a range of low
    // values have been preallocated and cached
    private fun checkObjectIsNotImplicit(objectID: ObjectID): ObjectID =
            if (objectID != 0L && !allocationThread.containsKey(objectID))
                0L
            else
                objectID

    private fun putFieldObject(putFieldObject: Access.PutFieldObject, threadId: ThreadID): EmittedEvents {
        return createEvents(threadId, EventType.SET_INTER_HEAP_LINK, {
            it.setInterHeapLink = SetInterHeapLink.newBuilder().apply {
                srcId = putFieldObject.id
                dstId = checkObjectIsNotImplicit(putFieldObject.valueId) // may be 0/null
                fieldName = putFieldObject.field
            }.build()
        })
    }

    private fun putFieldPrimitive(putFieldPrimitive: Access.PutFieldPrimitive, threadId: ThreadID): EmittedEvents {
        return createEvents(threadId, EventType.SHOW_HEAP_OBJECT_ACCESS, {
            it.showHeapObjectAccess = ShowHeapObjectAccess.newBuilder().apply {
                objId = putFieldPrimitive.id
            }.build()
        })
    }

    private fun storeObject(storeObject: Access.StoreObject, threadId: ThreadID): EmittedEvents {
        return createEvents(threadId, EventType.SET_LOCAL_VAR_LINK, {
            it.setLocalVarLink = SetLocalVarLink.newBuilder().apply {
                varIndex = storeObject.index
                dstId = checkObjectIsNotImplicit(storeObject.valueId)
            }.build()
        })
    }

    private fun storePrimitive(storePrimitive: Access.StorePrimitive, threadId: ThreadID): EmittedEvents {
        return createEvents(threadId, EventType.SHOW_LOCAL_VAR_ACCESS, {
            it.showLocalVarAccess = ShowLocalVarAccess.newBuilder().apply {
                varIndex = storePrimitive.index
            }.build()
        })
    }

    private fun storeObjectInArray(store: Access.StoreObjectInArray, threadId: ThreadID): EmittedEvents {
        return createEvents(threadId, EventType.SET_INTER_HEAP_LINK, {
            it.setInterHeapLink = SetInterHeapLink.newBuilder().apply {
                srcId = store.id
                dstId = checkObjectIsNotImplicit(store.valueId)
                fieldName = store.index.toString()
            }.build()
        })
    }

    private fun storePrimitiveInArray(store: Access.StorePrimitiveInArray, threadId: ThreadID): EmittedEvents {
        return createEvents(threadId, EventType.SHOW_HEAP_OBJECT_ACCESS, {
            it.showHeapObjectAccess = ShowHeapObjectAccess.newBuilder().apply {
                objId = store.id
            }.build()
        })
    }

    private fun load(load: Access.Load, threadId: ThreadID): EmittedEvents {
        return createEvents(threadId, EventType.SHOW_LOCAL_VAR_ACCESS, {
            it.showLocalVarAccess = ShowLocalVarAccess.newBuilder().apply {
                varIndex = load.index
            }.build()
        })
    }

    private fun loadFromArray(load: Access.LoadFromArray, threadId: ThreadID): EmittedEvents {
        // TODO new array access event?
        return createEvents(threadId, EventType.SHOW_HEAP_OBJECT_ACCESS, {
            it.showHeapObjectAccess = ShowHeapObjectAccess.newBuilder().apply {
                objId = load.id
                if (load.hasField(Access.LoadFromArray.getDescriptor().findFieldByNumber(Access.LoadFromArray.INDEX_FIELD_NUMBER)))
                    fieldName = load.index.toString()
            }.build()
        })
    }

    internal val loadedClassDefinitions: Collection<Definitions.ClassDefinition>
        get() = this.classDefinitions.values
}