package ms.domwillia.jvmemory.monitor.definition

import org.objectweb.asm.Type

class MethodDefinition internal constructor(
        access: Int,
        val name: String,
        val desc: String
) {
    val flags: FieldFlags = parseFlags(access) as FieldFlags
    val localVars: MutableList<LocalVariable> = mutableListOf()
        get() {
            field.sortBy { it.index }
            return field
        }

    fun registerLocalVariable(name: String, desc: String, index: Int) {
        localVars.add(LocalVariable(index, name, Type.getType(desc).className))
    }
}

data class LocalVariable(val index: Int, val name: String, val type: String)
