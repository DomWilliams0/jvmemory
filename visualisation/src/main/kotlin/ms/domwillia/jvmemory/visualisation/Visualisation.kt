package ms.domwillia.jvmemory.visualisation

import ms.domwillia.jvmemory.preprocessor.EventsLoader
import ms.domwillia.jvmemory.preprocessor.ThreadID
import ms.domwillia.jvmemory.preprocessor.protobuf.Event
import kotlin.system.exitProcess

class Visualisation(size: Pair<Int, Int>, events: EventsLoader) {
    init {
        // TODO only one thread for now
        val threadToRun: ThreadID = try {
            events.threads.first()
        } catch (e: NoSuchElementException) {
            System.err.println("no events to visualise")
            exitProcess(1)
        }

        val heap = HeapGraph()
        val callstack = CallStack()
        val renderer = Renderer(size, heap, callstack)

        renderer.openWindow()
        for (event in events.getEventsForThread(threadToRun)) {
            var sleep: Long = 500
            when (event.type) {
                Event.EventType.PUSH_METHOD_FRAME -> callstack.pushNewFrame(event.pushMethodFrame)
                Event.EventType.POP_METHOD_FRAME -> callstack.popFrame()

                else -> {
                    // TODO use event
                    sleep = 0
                }
            }

            Thread.sleep(sleep)
        }

        // TODO just pause visualisation instead of closing
        renderer.closeWindow()
    }
}