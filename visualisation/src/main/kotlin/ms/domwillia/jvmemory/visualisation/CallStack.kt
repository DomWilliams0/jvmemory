package ms.domwillia.jvmemory.visualisation

import ms.domwillia.jvmemory.preprocessor.protobuf.Event
import java.awt.Color
import java.util.*
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class CallStack : GUIPanel {

    private val container = JPanel()
    private val label = JLabel()
    private val stack = Stack<Event.PushMethodFrame>()

    init {
        label.text = "<empty callstack>"
        container.background = Color.GREEN
        container.add(label)
    }

    private fun updateLabel() {
        val text = if (stack.empty()) {
            "<empty callstack>"
        } else {
            val frame = stack.peek()
            "${frame.owningClass.name}:${frame.definition.name}"
        }
        label.text = text
    }

    fun pushNewFrame(frame: Event.PushMethodFrame) {
        stack.push(frame)
        updateLabel()
        println("pushed ${label.text}")
    }

    fun popFrame() {
        if (stack.isNotEmpty()) {
            stack.pop()
            updateLabel()
        }
        println("popped")
    }

    override val guiPanel: JComponent
        get() = container
}