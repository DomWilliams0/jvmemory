package ms.domwillia.jvmemory.visualisation

import ms.domwillia.jvmemory.preprocessor.EventsLoader

class Visualisation(size: Pair<Int, Int>, private val events: EventsLoader) {

    init {
        println("threads available: ${events.threads}")
    }

    fun go() {

    }
}