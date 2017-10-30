package ms.domwillia.jvmemory.monitor

import java.util.*

class LocalVarTracker {
    // method name -> var
    private val vars = HashMap<String, MutableList<LocalVar>>()

    fun registerLocalVar(method: String, name: String, index: Int) {
        val locals = vars.getOrPut(method, ::mutableListOf)
        locals.add(LocalVar(index, name))
    }

    fun debugPrint() {
        vars.forEach { (key, value) ->
            println("$key:")
            value.forEach { println("\t${it.index} - ${it.name}") }
        }
    }

    private data class LocalVar(val index: Int, val name: String)
}
