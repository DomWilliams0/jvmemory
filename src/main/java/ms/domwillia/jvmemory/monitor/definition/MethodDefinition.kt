package ms.domwillia.jvmemory.monitor.structure

class MethodStructure internal constructor(
        access: Int,
        val name: String,
        val desc: String,
        val signature: String?,
        val exceptions: Array<String>?
) {
    val flags: MethodFlags = parseFlags(access, FlagType.METHOD) as MethodFlags
    val localVars: MutableList<LocalVariable> = mutableListOf()

    fun registerLocalVariable(name: String, desc: String, signature: String?, index: Int) {
        localVars.add(LocalVariable(index, name, desc, signature))
    }
}

data class LocalVariable(val index: Int, val name: String, val type: String, val signature: String?)
