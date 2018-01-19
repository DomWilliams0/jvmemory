package ms.domwillia.jvmemory.visualisation

import javafx.application.Application

object VisualisationRunner {
    @JvmStatic
    fun main(args: Array<String>) {
        Application.launch(
                Visualisation::class.java,
                "../monitor-agent/jvmemory.log",
                "target/vis-events"
        )
    }
}