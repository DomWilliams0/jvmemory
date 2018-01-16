package ms.domwillia.jvmemory.visualisation

import ms.domwillia.jvmemory.preprocessor.Preprocessor
import java.io.File

object VisualisationRunner {
    @JvmStatic
    fun main(args: Array<String>) {
        val events = Preprocessor.runPreprocessor(
                File("../monitor-agent/jvmemory.log"),
                File("target/vis-events")
        )
        val size = Pair(600, 600)
        Visualisation(size, events).go()
    }
}