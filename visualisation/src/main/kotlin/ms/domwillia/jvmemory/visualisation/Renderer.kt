package ms.domwillia.jvmemory.visualisation

import javafx.application.Application
import java.awt.BorderLayout
import java.awt.event.WindowEvent
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.SwingUtilities

class Renderer(windowSize: Pair<Int, Int>, heapGraph: HeapGraph, callStack: CallStack) {

    private val frame = JFrame("JVMemory")

    init {
        frame.size.setSize(windowSize.first, windowSize.second)
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE

        val panel = JPanel(BorderLayout())
        panel.add(heapGraph.guiPanel, BorderLayout.CENTER)
//        panel.add(callStack.getGUIPanel(), BorderLayout.WEST)
        frame.contentPane = panel
    }

    fun openWindow() {
        frame.pack()
        frame.isVisible = true
    }

    fun closeWindow() {
        println("killing window in 3 seconds")
        Thread.sleep(3000)
        SwingUtilities.invokeLater {
            frame.dispatchEvent(WindowEvent(frame, WindowEvent.WINDOW_CLOSING))
        }
    }
}
