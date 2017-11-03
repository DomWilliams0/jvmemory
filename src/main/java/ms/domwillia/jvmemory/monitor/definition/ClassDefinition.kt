package ms.domwillia.jvmemory.monitor.definition

import java.util.*

class ClassDefinition(
        val name: String,
        access: Int,
        val signature: String?,
        val superName: String?,
        val interfaces: Array<String>?
) {
    val flags: ClassFlags = parseFlags(access, FlagType.CLASS) as ClassFlags
    private val methods: MutableList<MethodDefinition> = mutableListOf()
    private val fields: MutableList<Field> = mutableListOf()

    fun registerMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<String>?): MethodDefinition {
        val method = MethodDefinition(access, name, desc, signature, exceptions)
        methods.add(method)
        return method
    }

    fun registerField(access: Int, name: String, desc: String, signature: String?) {
        fields.add(Field(name, desc, signature, parseFlags(access, FlagType.FIELD) as FieldFlags))
    }

    fun debugPrint() {
        print("Class $name (${flags.type}, super=$superName, interfaces=${Arrays.toString(interfaces)}")
        println(if (signature != null) ", generics=$signature" else "")
        fields.forEach { f ->
            print("\tfield: ${f.flags.visibility}")
            if (f.flags.isStatic)
                print(" static")
            if (f.flags.isFinal)
                print(" final")
            print(" ${f.type} ${f.name}")
            println(if (f.signature != null) ", generics=${f.signature}" else "")
        }

        methods.forEach { m ->
            print("\tmethod ${m.name}: ${m.flags.visibility}")
            if (m.flags.isStatic)
                print(" static")
            if (m.flags.isFinal)
                print(" final")
            if (m.flags.isAbstract)
                print(" abstract")
            print(" ${m.desc} exceptions=${Arrays.toString(m.exceptions)}")
            println(if (m.signature != null) ", generics=${m.signature}" else "")

            m.localVars.forEach { v ->
                print("\t\tlocal var ${v.index} ${v.name} ${v.type}")
                println(if (v.signature != null) ", generics=${v.signature}" else "")
            }
            println("\t----------")
        }
        println("==========")
    }
}

data class Field(val name: String, val type: String, val signature: String?, val flags: FieldFlags)
