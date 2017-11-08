package ms.domwillia.jvmemory.monitor.logging

import ms.domwillia.jvmemory.monitor.definition.ClassDefinition
import ms.domwillia.jvmemory.monitor.definition.Field
import ms.domwillia.jvmemory.monitor.definition.LocalVariable
import ms.domwillia.jvmemory.monitor.definition.MethodDefinition
import ms.domwillia.jvmemory.protobuf.*
import java.io.OutputStream

class Logger(var stream: OutputStream) {

    fun logClassDefinition(def: ClassDefinition) {
        Message.Variant.newBuilder().apply {
            type = Message.MessageType.CLASS_DECL
            setClassDefinition(
                    Definitions.ClassDefinition.newBuilder().apply {
                        name = def.name
                        classType = def.flags.type.toString()
                        visibility = def.flags.visibility.toString()
                        if (def.superName != "java/lang/Object") superClass = def.superName

                        if (def.interfaces != null) addAllInterfaces(def.interfaces.asIterable())

                        addAllMethods(def.methods.map { toProtoBuf(it) })
                        addAllFields(def.fields.map { toProtoBuf(it) })
                    }
            )
        }.log()
    }

    fun logAllocation(desc: String, instanceId: Long) {
        Message.Variant.newBuilder().apply {
            type = Message.MessageType.ALLOC
            setAllocation(
                    Allocations.Allocation.newBuilder().apply {
                        type = desc
                        id = instanceId
                    }
            )
        }.log()
    }

    fun logDeallocation(instanceId: Long) {
        Message.Variant.newBuilder().apply {
            type = Message.MessageType.DEALLOC
            setDeallocation(
                    Allocations.Deallocation.newBuilder().apply {
                        id = instanceId
                    }
            )
        }.log()
    }

    fun logMethodEnter(className: String, methodName: String) {
        Message.Variant.newBuilder().apply {
            type = Message.MessageType.METHOD_ENTER
            setMethodEnter(Flow.MethodEnter.newBuilder().apply {
                class_ = className
                method = methodName
            })
        }.log()
    }

    fun logMethodExit() {
        Message.Variant.newBuilder().apply {
            type = Message.MessageType.METHOD_EXIT
            methodExit = Flow.MethodExit.getDefaultInstance()
        }.log()
    }

    fun logGetField(objId: Long, fieldName: String) {
        Message.Variant.newBuilder().apply {
            type = Message.MessageType.GETFIELD
            setGetField(Access.GetField.newBuilder().apply {
                id = objId
                field = fieldName
            })
        }.log()
    }

    fun logPutField(objId: Long, fieldName: String) {
        Message.Variant.newBuilder().apply {
            type = Message.MessageType.PUTFIELD
            setPutField(Access.PutField.newBuilder().apply {
                id = objId
                field = fieldName
            })
        }.log()
    }

    fun logLoad(varIndex: Int) {
        Message.Variant.newBuilder().apply {
            type = Message.MessageType.LOAD
            setLoad(Access.Load.newBuilder().apply {
                index = varIndex
            })
        }.log()
    }

    fun logStore(desc: String, varIndex: Int) {
        Message.Variant.newBuilder().apply {
            type = Message.MessageType.STORE
            setStore(Access.Store.newBuilder().apply {
                index = varIndex
                type = desc
            })
        }.log()
    }

    // ----------- helpers

    private fun Message.Variant.Builder.log() {
        threadId = Thread.currentThread().id
        val msg = build()

        // i think this has to be synchronised because it may be called in finalise() by GC
        synchronized(stream) {
            msg.writeDelimitedTo(stream)
        }
    }

    private fun toProtoBuf(lv: LocalVariable): Definitions.LocalVariable =
            Definitions.LocalVariable.newBuilder().apply {
                index = lv.index
                name = lv.name
                type = lv.type
            }.build()

    private fun toProtoBuf(m: MethodDefinition): Definitions.MethodDefinition =
            Definitions.MethodDefinition.newBuilder().apply {
                name = m.name
                signature = m.desc
                visibility = m.flags.visibility.toString()
                addAllLocalVars(m.localVars.map { toProtoBuf(it) })
            }.build()

    private fun toProtoBuf(f: Field): Definitions.FieldDefinition =
            Definitions.FieldDefinition.newBuilder().apply {
                name = f.name
                type = f.type
                visibility = f.flags.visibility.toString()
                static = f.flags.isStatic
            }.build()


}