package ms.domwillia.jvmemory.server

import ms.domwillia.jvmemory.preprocessor.EventsLoader
import ms.domwillia.jvmemory.preprocessor.Preprocessor
import java.io.File

object Server {
    @JvmStatic
    fun main(args: Array<String>) {
        val useCached = args.getOrNull(0)?.equals("cached") ?: false
        val events = if (!useCached) {
            Preprocessor.runPreprocessor()
        } else {
            EventsLoader(File(Preprocessor.defaultOutputDirPath))
        }

        // TODO tada, server
    }
}
