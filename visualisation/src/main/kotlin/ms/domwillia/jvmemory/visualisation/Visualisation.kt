package ms.domwillia.jvmemory.visualisation

import javafx.application.Application
import javafx.embed.swing.SwingNode
import javafx.scene.Scene
import javafx.scene.layout.StackPane
import javafx.stage.Stage
import ms.domwillia.jvmemory.preprocessor.EventsLoader
import ms.domwillia.jvmemory.preprocessor.Preprocessor
import ms.domwillia.jvmemory.preprocessor.protobuf.Event
import java.io.File
import kotlin.concurrent.thread

class Visualisation : Application() {

    lateinit var events: EventsLoader

    override fun init() {
        val params = parameters.raw
        if (params.size != 2)
            throw IllegalArgumentException("<input log path> <output dir path>")

        events = Preprocessor.runPreprocessor(
                File(params[0]),
                File(params[1])
        )

        if (events.threads.isEmpty())
            throw IllegalStateException("no events to visualise")
    }

    override fun start(primaryStage: Stage) {
        val root = StackPane()

        val heap = HeapGraph(events.definitions)
        val callstack = CallStack()

        val heapNode = SwingNode().apply {
            content = heap.guiPanel
        }

        root.children.add(heapNode)

        primaryStage.title = "JVMemory"
        primaryStage.scene = Scene(root, 600.0, 600.0)
        primaryStage.show()

        thread {
            val threadToRun = 1L // TODO use other threads too
            for (event in events.getEventsForThread(threadToRun)) {
                var sleep: Long = 200
                when (event.type) {
                    Event.EventType.PUSH_METHOD_FRAME -> callstack.pushNewFrame(event.pushMethodFrame)
                    Event.EventType.POP_METHOD_FRAME -> callstack.popFrame()

                    Event.EventType.ADD_HEAP_OBJECT -> {
                        val e = event.addHeapObject
                        heap.addVertex(e.id, e.class_)
                    }
                    Event.EventType.DEL_HEAP_OBJECT -> heap.deleteVertex(event.delHeapObject.id)

                    Event.EventType.SET_INTER_HEAP_LINK -> {
                        val e = event.setInterHeapLink
                        heap.setLink(e.srcId, e.dstId, e.name)
                    }
//                Event.EventType.SHOW_HEAP_OBJECT_ACCESS -> heap.showAccess(event.showHeapObjectAccess)

                    else -> {
                        // TODO use other events
                        sleep = 0
                    }
                }

                root.requestLayout()
                Thread.sleep(sleep)
            }
        }
    }



    init {
/*
val heap = HeapGraph(events.getDefinitions())
val callstack = CallStack()
val renderer = Renderer(size, heap, callstack)

renderer.openWindow()
for (event in events.getEventsForThread(threadToRun)) {
    var sleep: Long = 200
    when (event.type) {
        Event.EventType.PUSH_METHOD_FRAME -> callstack.pushNewFrame(event.pushMethodFrame)
        Event.EventType.POP_METHOD_FRAME -> callstack.popFrame()

        Event.EventType.ADD_HEAP_OBJECT -> {
            val e = event.addHeapObject
            heap.addVertex(e.id, e.class_)
        }
        Event.EventType.DEL_HEAP_OBJECT -> heap.deleteVertex(event.delHeapObject.id)

        Event.EventType.SET_INTER_HEAP_LINK -> {
            val e = event.setInterHeapLink
            heap.setLink(e.srcId, e.dstId, e.name)
        }
//                Event.EventType.SHOW_HEAP_OBJECT_ACCESS -> heap.showAccess(event.showHeapObjectAccess)

        else -> {
            // TODO use other events
            sleep = 0
        }
    }

    Thread.sleep(sleep)
}

// TODO just pause visualisation instead of closing
//        renderer.closeWindow()
*/
    }
}