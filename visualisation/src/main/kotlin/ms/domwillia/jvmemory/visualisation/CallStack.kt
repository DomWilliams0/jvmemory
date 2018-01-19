package ms.domwillia.jvmemory.visualisation

import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.CornerRadii
import ms.domwillia.jvmemory.preprocessor.protobuf.Event
import java.util.*

class CallStack : GUIPanel {

    private val label = Label()
    private val stack = Stack<Event.PushMethodFrame>()

    init {
        label.text = "<empty callstack>"
        label.background = Background(BackgroundFill(javafx.scene.paint.Color.LAWNGREEN, CornerRadii.EMPTY, Insets.EMPTY))
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

    override val guiPanel: Node
        get() = label
}