package ms.domwillia.jvmemory.monitor.definition

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
        localVars.add(LocalVariable(index, name, desc))
    }
}

data class LocalVariable(val index: Int, val name: String, val type: String)
