package ms.domwillia.jvmemory

import ms.domwillia.jvmemory.protobuf.Message
import java.io.File

object ProtoBufTester {

    @JvmStatic
    fun main(args: Array<String>) {
        val stream = File("jvmemory.log").inputStream()
        while(true) {
            val msg = Message.Variant.parseDelimitedFrom(stream) ?: break
            println("msg = $msg")
        }
    }
}
