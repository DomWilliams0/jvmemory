package ms.domwillia.jvmemory.monitor.definition

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
}

data class Field(val name: String, val type: String, val flags: FieldFlags)
