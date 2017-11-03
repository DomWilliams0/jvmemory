package ms.domwillia.jvmemory

import ms.domwillia.jvmemory.protobuf.Definitions
import java.io.File

object ProtoBufTester {

    @JvmStatic
    fun main(args: Array<String>) {
        val def = Definitions.ClassDefinition.parseFrom(File("jvmemory.log").readBytes())
        println(def)
    }
}
