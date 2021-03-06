package ms.domwillia.jvmemory.preprocessor

import ms.domwillia.jvmemory.preprocessor.protobuf.Event.*
import ms.domwillia.jvmemory.protobuf.*
import java.util.*

typealias EmittedEvent = EventVariant.Builder
typealias EmittedEvents = MutableList<EmittedEvent>
typealias ClassStates = Map<String, ClassState>

data class ClassState(val def: Definitions.ClassDefinition,
                      val statics: MutableMap<String, ObjectID>)

val emptyEmittedEvents = mutableListOf<EmittedEvent>()

class RawMessageHandler(private val classStates: ClassStates) {
    private var allocations = mutableSetOf<ObjectID>()
    private var callstack = Stack<PushMethodFrame>()

    fun handle(msg: Message.Variant): EmittedEvents = when (msg.type) {
        Message.MessageType.ALLOC_OBJECT -> allocateObject(msg.allocObject)
        Message.MessageType.ALLOC_ARRAY -> allocateArray(msg.allocArray)
        Message.MessageType.DEALLOC -> deallocate(msg.dealloc)

        Message.MessageType.GETFIELD -> getField(msg.getField)
        Message.MessageType.PUTFIELD_OBJECT -> putFieldObject(msg.putFieldObject)
        Message.MessageType.PUTFIELD_PRIMITIVE -> putFieldPrimitive(msg.putFieldPrimitive)

        Message.MessageType.LOAD -> load(msg.load)
        Message.MessageType.LOAD_ARRAY -> loadFromArray(msg.loadFromArray)

        Message.MessageType.STORE_OBJECT -> storeObject(msg.storeObject)
        Message.MessageType.STORE_PRIMITIVE -> storePrimitive(msg.storePrimitive)

        Message.MessageType.GETSTATIC -> getStatic(msg.getStatic)
        Message.MessageType.PUTSTATIC_OBJECT -> putStaticObject(msg.putStaticObject)

        Message.MessageType.STORE_OBJECT_IN_ARRAY -> storeObjectInArray(msg.storeObjectInArray)
        Message.MessageType.STORE_PRIMITIVE_IN_ARRAY -> storePrimitiveInArray(msg.storePrimitiveInArray)

        Message.MessageType.CLASS_DEF -> emptyEmittedEvents

        Message.MessageType.METHOD_ENTER -> enterMethod(msg.methodEnter)
        Message.MessageType.METHOD_EXIT -> exitMethod()

        Message.MessageType.TO_STRING_OBJECT -> toStringObject(msg.toStringObject)

        else -> throw IllegalArgumentException("bad message type: $msg")
    }

    /**
     * Helpers
     */
    private fun createEvents(
            messageType: EventType,
            initialiser: (EventVariant.Builder) -> Unit,
            continuous: Boolean = false): EmittedEvents =
            mutableListOf(createEvent(messageType, initialiser, continuous))

    private fun createEvent(
            messageType: EventType,
            initialiser: (EventVariant.Builder) -> Unit,
            continuous: Boolean = false): EmittedEvent = EventVariant.newBuilder().apply {
        type = messageType
        this.continuous = continuous
        initialiser(this)
    }

    private fun arrayIndexField(index: Int) = "[$index]"

    private fun enterMethod(msg: Flow.MethodEnter): EmittedEvents {
        val type = msg.class_
        val methodName = msg.method

        val classDef = classStates[type]
                ?: throw IllegalStateException("undefined class $type")
        val methodDef = classDef.def.methodsList.find { it.name == methodName }
                ?: throw IllegalStateException("undefined method $type:$methodName")

        val frame = PushMethodFrame.newBuilder().apply {
            owningClass = type
            name = methodDef.name
            signature = methodDef.signature
            objId = msg.objId
        }.build()

        callstack.push(frame)
        return createEvents(EventType.PUSH_METHOD_FRAME, {
            it.pushMethodFrame = frame
        })
    }

    private fun exitMethod(): EmittedEvents {
        callstack.pop()

        return createEvents(EventType.POP_METHOD_FRAME, {
            it.popMethodFrame = PopMethodFrame.getDefaultInstance()
        })
    }

    private fun allocateObject(alloc: Allocations.AllocationObject): EmittedEvents {
        allocations.add(alloc.id)

        val events = createEvents(EventType.ADD_HEAP_OBJECT, {
            it.addHeapObject = AddHeapObject.newBuilder().apply {
                id = alloc.id
                class_ = alloc.type
            }.build()
        })

        // TODO links must be static
        classStates[alloc.type]?.let { cls ->
            cls.def.fieldsList
                    .filter { it.static }
                    .forEach { field ->
                        cls.statics[field.name]?.let { value ->
                            if (value != 0L) {
                                events += createEvent(EventType.SET_INTRA_HEAP_LINK, { e ->
                                    e.setIntraHeapLink = SetIntraHeapLink.newBuilder().apply {
                                        srcId = alloc.id
                                        dstId = value
                                        fieldName = field.name
                                    }.build()
                                })
                            }
                        }
                    }
        }


        return events
    }

    private fun allocateArray(alloc: Allocations.AllocationArray): EmittedEvents {
        allocations.add(alloc.id)

        val events = createEvents(EventType.ADD_HEAP_OBJECT, {
            it.addHeapObject = AddHeapObject.newBuilder().apply {
                id = alloc.id
                class_ = alloc.type
                arraySize = alloc.size
            }.build()
        })

        if (alloc.hasField(Allocations.AllocationArray.getDescriptor().findFieldByNumber(Allocations.AllocationArray.SRC_ARRAY_ID_FIELD_NUMBER))) {
            events.add(
                    createEvent(EventType.SET_INTRA_HEAP_LINK, {
                        it.setIntraHeapLink = SetIntraHeapLink.newBuilder().apply {
                            srcId = alloc.srcArrayId
                            dstId = alloc.id
                            fieldName = arrayIndexField(alloc.srcIndex)
                        }.build()
                    })
            )
        }

        return events
    }

    private fun deallocate(dealloc: Allocations.Deallocation): EmittedEvents =
            if (!allocations.remove(dealloc.id)) {
                emptyEmittedEvents
            } else
                createEvents(EventType.DEL_HEAP_OBJECT, {
                    it.delHeapObject = DelHeapObject.newBuilder().apply {
                        id = dealloc.id
                    }.build()
                })

    private fun getField(getField: Access.GetField): EmittedEvents {
        return createEvents(EventType.SHOW_HEAP_OBJECT_ACCESS, {
            it.showHeapObjectAccess = ShowHeapObjectAccess.newBuilder().apply {
                objId = getField.id
                fieldName = getField.field
                read = true
            }.build()
        })
    }

    // removes implicitly allocated objects that haven't been explicitly added to the heap graph
    // these will typically be boxed types (Long, Integer, Byte etc.) for which a range of low
    // values have been preallocated and cached
    private fun checkObjectIsNotImplicit(objectID: ObjectID): ObjectID =
            if (objectID != 0L && !allocations.contains(objectID))
                0L
            else
                objectID

    private fun putFieldObject(putFieldObject: Access.PutFieldObject): EmittedEvents {
        return createEvents(EventType.SET_INTRA_HEAP_LINK, {
            it.setIntraHeapLink = SetIntraHeapLink.newBuilder().apply {
                srcId = putFieldObject.id
                dstId = checkObjectIsNotImplicit(putFieldObject.valueId) // may be 0/null
                fieldName = putFieldObject.field
            }.build()
        })
    }

    private fun putFieldPrimitive(putFieldPrimitive: Access.PutFieldPrimitive): EmittedEvents {
        return createEvents(EventType.SHOW_HEAP_OBJECT_ACCESS, {
            it.showHeapObjectAccess = ShowHeapObjectAccess.newBuilder().apply {
                objId = putFieldPrimitive.id
                read = false
            }.build()
        })
    }

    private fun storeObject(storeObject: Access.StoreObject): EmittedEvents {
        return createEvents(EventType.SET_LOCAL_VAR_LINK, {
            it.setLocalVarLink = SetLocalVarLink.newBuilder().apply {
                varIndex = storeObject.index
                dstId = checkObjectIsNotImplicit(storeObject.valueId)
            }.build()
        })
    }

    private fun storePrimitive(storePrimitive: Access.StorePrimitive): EmittedEvents {
        return createEvents(EventType.SHOW_LOCAL_VAR_ACCESS, {
            it.showLocalVarAccess = ShowLocalVarAccess.newBuilder().apply {
                varIndex = storePrimitive.index
                read = false
            }.build()
        })
    }

    private fun getStatic(getStatic: Access.GetStatic): EmittedEvents {
        val state = classStates[getStatic.class_]
                ?: return emptyEmittedEvents

        val value = state.statics[getStatic.field] ?: return emptyEmittedEvents

        return createEvents(EventType.SHOW_HEAP_OBJECT_ACCESS, {
            it.showHeapObjectAccess = ShowHeapObjectAccess.newBuilder().apply {
                objId = value
                read = true
            }.build()
        })
    }

    // TODO what if other threads set the value of a static?

    private fun putStaticObject(putStaticObject: Access.PutStaticObject): EmittedEvents {
        val state = classStates[putStaticObject.class_]
                ?: throw IllegalArgumentException("unknown class ${putStaticObject.class_}")

        val oldValue = state.statics.put(putStaticObject.field, putStaticObject.valueId)

        return createEvents(EventType.SET_STATIC, {
            it.setStatic = SetStatic.newBuilder().apply {
                oldObjId = oldValue ?: 0L
                newObjId = putStaticObject.valueId
                class_ = putStaticObject.class_
                fieldName = putStaticObject.field
            }.build()
        })
    }

    private fun storeObjectInArray(store: Access.StoreObjectInArray): EmittedEvents {
        return createEvents(EventType.SET_INTRA_HEAP_LINK, {
            it.setIntraHeapLink = SetIntraHeapLink.newBuilder().apply {
                srcId = store.id
                dstId = checkObjectIsNotImplicit(store.valueId)
                fieldName = arrayIndexField(store.index)
            }.build()
        })
    }

    private fun storePrimitiveInArray(store: Access.StorePrimitiveInArray): EmittedEvents {
        return createEvents(EventType.SHOW_HEAP_OBJECT_ACCESS, {
            it.showHeapObjectAccess = ShowHeapObjectAccess.newBuilder().apply {
                objId = store.id
                read = false
            }.build()
        })
    }

    private fun load(load: Access.Load): EmittedEvents {
        return createEvents(EventType.SHOW_LOCAL_VAR_ACCESS, {
            it.showLocalVarAccess = ShowLocalVarAccess.newBuilder().apply {
                varIndex = load.index
                read = true
            }.build()
        }, continuous = true)
    }

    private fun loadFromArray(load: Access.LoadFromArray): EmittedEvents {
        return createEvents(EventType.SHOW_HEAP_OBJECT_ACCESS, {
            it.showHeapObjectAccess = ShowHeapObjectAccess.newBuilder().apply {
                objId = load.id
                read = true
                if (load.hasField(Access.LoadFromArray.getDescriptor().findFieldByNumber(Access.LoadFromArray.INDEX_FIELD_NUMBER)))
                    fieldName = arrayIndexField(load.index)
            }.build()
        }, continuous = true)
    }

    private fun toStringObject(toString: Definitions.ToStringObject): EmittedEvents {
        return createEvents(EventType.TO_STRING_OBJECT, {
            it.toStringObject = ToStringObject.newBuilder().apply {
                objId = toString.objId
                str = toString.str
            }.build()
        }, continuous = true)
    }
}