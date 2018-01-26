package ms.domwillia.jvmemory.monitor.definition

import ms.domwillia.jvmemory.protobuf.Definitions
import java.util.*

class ClassDefinition(
        val name: String,
        access: Int,
        val superName: String?,
        val interfaces: Array<String>?
) {
    val flags: ClassFlags = parseFlags(access, forClass = true) as ClassFlags
    val methods: MutableList<MethodDefinition> = mutableListOf()
    val fields: MutableList<Field> = mutableListOf()

    fun registerMethod(access: Int, name: String, desc: String): MethodDefinition {
        val method = MethodDefinition(access, name, desc)
        methods.add(method)
        return method
    }

    fun registerField(access: Int, name: String, desc: String) {
        fields.add(Field(name, desc, parseFlags(access) as FieldFlags))
    }

    fun debugPrint() {
        println("Class $name (${flags.type}, super=$superName, interfaces=${Arrays.toString(interfaces)}")
        fields.forEach { f ->
            print("\tfield: ${f.flags.visibility}")
            if (f.flags.isStatic)
                print(" static")
            println(" ${f.type} ${f.name}")
        }

        methods.forEach { m ->
            print("\tmethod ${m.name}: ")
            if (m.flags.isStatic)
                print("static ")
            println("${m.flags.visibility}")

            m.localVars.forEach { v ->
                println("\t\tlocal var ${v.index} ${v.name} ${v.type}")
            }
            println("\t----------")
        }
        println("==========")
    }

    fun toProtoBuf(): Definitions.ClassDefinition {
        fun toProtoBuf(lv: LocalVariable): Definitions.LocalVariable =
                Definitions.LocalVariable.newBuilder().apply {
                    index = lv.index
                    name = lv.name
                    type = lv.type
                }.build()

        fun toProtoBuf(m: MethodDefinition): Definitions.MethodDefinition =
                Definitions.MethodDefinition.newBuilder().apply {
                    name = m.name
                    signature = m.desc
                    visibility = m.flags.visibility.toString()
                    static = m.flags.isStatic
                    addAllLocalVars(m.localVars.map { toProtoBuf(it) })
                }.build()

        fun toProtoBuf(f: Field): Definitions.FieldDefinition =
                Definitions.FieldDefinition.newBuilder().apply {
                    name = f.name
                    type = f.type
                    visibility = f.flags.visibility.toString()
                    static = f.flags.isStatic
                }.build()

        val def = this
        return Definitions.ClassDefinition.newBuilder().apply {
            name = def.name
            classType = def.flags.type.toString()
            visibility = def.flags.visibility.toString()
            if (def.superName != "java/lang/Object") superClass = def.superName

            if (def.interfaces != null) addAllInterfaces(def.interfaces.asIterable())

            addAllMethods(def.methods.map(::toProtoBuf))
            addAllFields(def.fields.map(::toProtoBuf))
        }.build()
    }
}

data class Field(val name: String, val type: String, val flags: FieldFlags)
