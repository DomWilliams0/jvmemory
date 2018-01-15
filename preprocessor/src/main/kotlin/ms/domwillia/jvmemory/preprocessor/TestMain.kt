package ms.domwillia.jvmemory.preprocessor

import java.io.File


object TestMain {
    @JvmStatic
    fun main(arg: Array<String>) {
        val inputPath = "../monitor-agent/jvmemory.log"
        val outputDir = "target/vis-events"

        Preprocessor.runPreprocessor(File(inputPath), File(outputDir))
    }
}