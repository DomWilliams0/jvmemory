package ms.domwillia.jvmemory.visualisation

import ms.domwillia.jvmemory.preprocessor.EventsLoader

class Visualisation(size: Pair<Int, Int>, private val events: EventsLoader) {
    init {
        val heap = HeapGraph()
        val renderer = Renderer(size, heap)

        renderer.openWindow()
    }
}