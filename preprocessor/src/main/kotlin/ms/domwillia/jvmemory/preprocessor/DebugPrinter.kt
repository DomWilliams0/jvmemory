package ms.domwillia.jvmemory.preprocessor

import java.io.File
import kotlin.system.exitProcess

object DebugPrinter {
    @JvmStatic
    fun main(args: Array<String>) {
        val paths = args.take(3)
        if (paths.size != 2 && paths.size != 3) {
            System.err.println("Expected args: <input log file> <output log dir> [thread]")
            exitProcess(1)
        }

        val (inFile, outDir) = paths
        val thread = paths.getOrNull(2)?.toLong()

        Preprocessor.readMessages(File(inFile)).forEach { println("$it\n-------------") }

        val events = Preprocessor.runPreprocessor(File(inFile), File(outDir))

        val tids = if (thread == null) {
            events.threads
        } else {
            listOf(thread)
        }

        tids.forEach { tid ->
            println("=================== Thread $tid")
            events.getEventsForThread(tid).forEach { println("$it\n-------------") }
        }


    }
}
