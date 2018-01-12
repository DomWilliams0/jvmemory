package ms.domwillia.jvmemory.monitor

import ms.domwillia.jvmemory.monitor.definition.ClassDefinition
import ms.domwillia.jvmemory.monitor.definition.Field
import ms.domwillia.jvmemory.monitor.definition.LocalVariable
import ms.domwillia.jvmemory.monitor.definition.MethodDefinition
import ms.domwillia.jvmemory.protobuf.*
import java.io.OutputStream

class Logger(var stream: OutputStream) {

    private var active = true

    private fun checkActive(): Boolean {
        if (!active) {
            return false
        }

        active = false
        return true
    }

    fun logClassDefinition(def: ClassDefinition) {
        if (!checkActive()) return
        try {
            Message.Variant.newBuilder().apply {
                type = Message.MessageType.CLASS_DEF
                setClassDefinition(
                        Definitions.ClassDefinition.newBuilder().apply {
                            name = def.name
                            classType = def.flags.type.toString()
                            visibility = def.flags.visibility.toString()
                            if (def.superName != null && def.superName != "java/lang/Object") superClass = def.superName

                            if (def.interfaces != null) addAllInterfaces(def.interfaces.asIterable())

                        addAllMethods(def.methods.map { toProtoBuf(it) })
                        addAllFields(def.fields.map { toProtoBuf(it) })
                        }
                )
            }.log()
        } finally {
            active = true
        }
    }

    fun logAllocation(desc: String, instanceId: Long) {
        if (!checkActive()) return
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
        if (!checkActive()) return
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
        if (!checkActive()) return
        Message.Variant.newBuilder().apply {
            type = Message.MessageType.METHOD_ENTER
            setMethodEnter(Flow.MethodEnter.newBuilder().apply {
                class_ = className
                method = methodName
            })
        }.log()
    }

    fun logMethodExit() {
        if (!checkActive()) return
        Message.Variant.newBuilder().apply {
            type = Message.MessageType.METHOD_EXIT
            methodExit = Flow.MethodExit.getDefaultInstance()
        }.log()
    }

    fun logGetField(objId: Long, fieldName: String) {
        if (!checkActive()) return
        Message.Variant.newBuilder().apply {
            type = Message.MessageType.GETFIELD
            setGetField(Access.GetField.newBuilder().apply {
                id = objId
                field = fieldName
            })
        }.log()
    }

    fun logPutField(objId: Long, fieldName: String, valId: Long) {
        if (!checkActive()) return
        Message.Variant.newBuilder().apply {
            type = Message.MessageType.PUTFIELD
            setPutField(Access.PutField.newBuilder().apply {
                id = objId
                field = fieldName
                valueId = valId
            })
        }.log()
    }

    fun logLoad(varIndex: Int) {
        if (!checkActive()) return
        Message.Variant.newBuilder().apply {
            type = Message.MessageType.LOAD
            setLoad(Access.Load.newBuilder().apply {
                index = varIndex
            })
        }.log()
    }

    fun logStore(desc: String, varIndex: Int) {
        if (!checkActive()) return
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
        msg.writeDelimitedTo(stream)
        active = true
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