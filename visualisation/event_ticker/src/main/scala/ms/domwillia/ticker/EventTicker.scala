package ms.domwillia.ticker

import ms.domwillia.jvmemory.preprocessor.protobuf.vis_event.EventVariant

import scala.scalajs.js
import scala.scalajs.js.Dynamic
import scala.scalajs.js.annotation.{JSExport, JSExportAll, JSExportTopLevel}
import scala.scalajs.js.timers.{SetTimeoutHandle, clearTimeout, setTimeout}


@js.native
trait Node extends js.Object {
  val id: Long = js.native
  val clazz: String = js.native
  val array: js.Dynamic = js.native
  val fill: String = js.native
  val x: Double = js.native
  val y: Double = js.native
}

@js.native
trait Link extends js.Object {
  val source: Long = js.native
  val target: Long = js.native
  val name: String = js.native
}

@js.native
trait References extends js.Object {
  val definitions: js.Array[js.Dynamic] = js.native
  val heapObjects: js.Array[Node] = js.native
  val heapLinks: js.Array[Link] = js.native
  val callstack: js.Array[js.Dynamic] = js.native
  val stackFrames: js.Dictionary[js.Dynamic] = js.native
}

@js.native
trait Callbacks extends js.Object {
  val setPlayButtonState: js.Function1[Boolean, Unit] = js.native
  val setSimulationState: js.Function1[Boolean, Unit] = js.native
  val highlightLocalVar: js.Function2[Long, Boolean, Unit] = js.native
  val highlightHeapObj: js.Function3[Long, String, Boolean, Unit] = js.native
}

@JSExportTopLevel("Constants")
@JSExportAll
object Constants {
  // ms between events
  val MaxSpeed = 1
  val MinSpeed = 500
  val DefaultSpeed = 50
}

@JSExportTopLevel("EventTicker")
class EventTicker(val events: js.Array[EventVariant], val references: References, val callbacks: Callbacks) {
  Dynamic.global.console.log("events=%o refs=%o callbacks=%o", events, references, callbacks)

  private var currentEvent = 0 // TODO placeholder
  private var _speed = Constants.DefaultSpeed
  private var playing = false
  private var handle: Option[SetTimeoutHandle] = None

  @JSExport
  def speed: Int = _speed

  @JSExport
  def speed_=(value: Int): Unit = _speed = Constants.MaxSpeed max value min Constants.MinSpeed

  @JSExport
  def resume(): Unit = startTickLoop()

  @JSExport
  def pause(): Unit = stopTickLoop()

  @JSExport
  def toggle(): Unit = {
    playing = !playing
    callbacks.setPlayButtonState(!playing)
    if (playing) pause() else resume()
  }

  @JSExport
  def scrubToRelative(delta: Int): Unit = scrubTo(currentEvent + delta)

  @JSExport
  def scrubTo(eventIndex: Int): Unit = {
    println(s"scrubbing from $currentEvent to $eventIndex")
    currentEvent = eventIndex
  }

  private def handle(event: EventVariant): Unit = println(s"handling an event ${event.`type`}")

  private def tick(): Unit = println(s"tick ${currentEvent += 1; currentEvent}")

  private def stopTickLoop(): Unit = {
    handle.foreach(clearTimeout)
    handle = None
    println("stop!")
  }

  private def startTickLoop(): Unit = {
    def loopTheLoop(): Unit = {
      tick()
      handle = Some(setTimeout(_speed) {
        loopTheLoop()
      })
    }

    loopTheLoop()
  }
}