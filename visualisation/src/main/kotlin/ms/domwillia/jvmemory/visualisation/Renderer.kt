package ms.domwillia.jvmemory.visualisation

import javax.swing.JFrame

class Renderer(windowSize: Pair<Int, Int>, heapGraph: HeapGraph) {

    private val frame = JFrame("JVMemory")

    init {
        frame.size.setSize(windowSize.first, windowSize.second)
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.add(heapGraph.guiPanel)
    }

    fun openWindow() {
        frame.pack()
        frame.isVisible = true
    }
}
