package ms.domwillia.jvmemory.monitor.logging

import ms.domwillia.jvmemory.monitor.definition.ClassDefinition
import ms.domwillia.jvmemory.monitor.definition.Field
import ms.domwillia.jvmemory.monitor.definition.LocalVariable
import ms.domwillia.jvmemory.monitor.definition.MethodDefinition
import ms.domwillia.jvmemory.protobuf.Allocations
import ms.domwillia.jvmemory.protobuf.Definitions
import java.io.OutputStream

class Logger(var stream: OutputStream) {

    fun logClassDefinition(def: ClassDefinition) {
        Definitions.ClassDefinition.newBuilder().apply {
            name = def.name
            classType = def.flags.type.toString()
            visibility = def.flags.visibility.toString()
            if (def.superName != "java/lang/Object") superClass = def.superName

            if (def.interfaces != null) addAllInterfaces(def.interfaces.asIterable())

            addAllMethods(def.methods.map { toProtoBuf(it) })
            addAllFields(def.fields.map { toProtoBuf(it) })

            build().writeTo(stream)
        }
    }

    fun logAllocation(desc: String, instanceId: Long) {
        Allocations.Allocation.newBuilder().apply {
            type = desc
            id = instanceId
            build().writeTo(stream)
        }
    }

    fun logDeallocation(instanceId: Long) {
        Allocations.Deallocation.newBuilder().apply {
            id = instanceId
            build().writeTo(stream)
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