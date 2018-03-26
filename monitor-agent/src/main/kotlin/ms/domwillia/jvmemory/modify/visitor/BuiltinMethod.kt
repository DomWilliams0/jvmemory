package ms.domwillia.jvmemory.modify.visitor

enum class BuiltinMethod {
    TO_STRING, MAIN, NONE;

    companion object {
        fun parse(name: String, desc: String): BuiltinMethod = when {
            name == "toString" && desc == "()Ljava/lang/String;" -> TO_STRING
            name == "main" && desc == "([Ljava/lang/String;)V" -> MAIN
            else -> NONE
        }
    }
}