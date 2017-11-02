package ms.domwillia.jvmemory.monitor

import java.util.*

class LocalVarTracker {
    // method name -> var
    private val vars = HashMap<String, MutableList<LocalVar>>()

    fun registerLocalVar(method: String, name: String, type: String, index: Int) {
        val locals = vars.getOrPut(method, ::mutableListOf)
        locals.add(LocalVar(index, name, type))
    }

    fun debugPrint() {
        vars.forEach { (key, value) ->
            println("$key:")
            value.forEach { println("\t${it.index} - ${it.type} ${it.name}") }
        }
    }

    private data class LocalVar(val index: Int, val name: String, val type: String)
}
