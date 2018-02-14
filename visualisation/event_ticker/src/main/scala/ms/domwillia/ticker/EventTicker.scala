package ms.domwillia.ticker

import ms.domwillia.jvmemory.preprocessor.protobuf.vis_event.EventVariant
import ms.domwillia.ticker.HandleResult.HandleResult

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExport, JSExportAll, JSExportTopLevel}
import scala.scalajs.js.timers.{SetTimeoutHandle, clearTimeout, setTimeout}

@js.native
trait GoodyBag extends js.Object {
  val setPlayButtonState: js.Function1[Boolean, Unit] = js.native
  val restartSim: js.Function1[Boolean, Unit] = js.native
  val setSimState: js.Function1[Boolean, Unit] = js.native
  val highlightLocalVar: js.Function2[Long, Boolean, Unit] = js.native
  val highlightHeapObj: js.Function3[Long, String, Boolean, Unit] = js.native

  val callStack: CallStack = js.native
  val definitions: Definitions = js.native
  val nodes: js.Array[Node] = js.native
  val links: js.Array[Link] = js.native
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
class EventTicker(rawEvents: js.typedarray.Uint8Array, val goodyBag: GoodyBag) {
  // TODO use scala seq instead of js array, to have better functionality
  private val events: Array[EventVariant] = Utils.parseEvents(rawEvents)
  private var currentEvent = 0
  private var _speed = Constants.DefaultSpeed
  private var playing = false
  private var handle: Option[SetTimeoutHandle] = None

  private val handler = new Handler(goodyBag)

  def clamp(min: Int, max: Int, value: Int): Int = min.max(value).min(max) // teehee

  @JSExport
  def speed: Int = _speed

  @JSExport
  def speed_=(value: Int): Unit = _speed = clamp(Constants.MaxSpeed, Constants.MinSpeed, value)

  @JSExport
  def resume(): Unit = {
    goodyBag.setSimState(true)
    startTickLoop()
  }

  @JSExport
  def pause(): Unit = {
    goodyBag.setSimState(false)
    stopTickLoop()
  }

  @JSExport
  def toggle(): Unit = {
    playing = !playing
    goodyBag.setPlayButtonState(!playing)
    if (playing) pause() else resume()
  }

  @JSExport
  def scrubToRelative(delta: Int): Unit = scrubTo(currentEvent + delta)

  @JSExport
  def scrubTo(eventIndex: Int): Unit = {
    val (step, source, undo) = if (eventIndex < currentEvent)
      (-1, currentEvent - 1, true)
    else (1, currentEvent, false)

    val target = clampIndex(eventIndex)

    if (target == currentEvent)
      return

    // TODO use handle results
    (source to target by step).foreach(handle(_, undo))

    currentEvent = clampIndex(target + (if (undo) 0 else 1))

    // dont repeat the last event
    if (eventIndex >= events.length)
      currentEvent = events.length
  }

  private def clampIndex(index: Int): Int = clamp(0, events.length - 1, index)

  private def isEventInRange: Boolean = events.indices contains currentEvent

  private def handle(index: Int, undo: Boolean = false): HandleResult = {
    val e = events(index)
    val action = if (undo) "undoing" else "handling"
    println(s"$action event $index: ${e.`type`}")
    handler.handle(e.payload)
  }

  private def refreshSimulation(result: HandleResult): Unit = result match {
    case HandleResult.ChangedGraph => goodyBag.restartSim(true)
    case HandleResult.ChangedStackOnly => goodyBag.restartSim(false)
    case _ =>
  }

  private def tick(): Boolean = {
    if (isEventInRange) {
      refreshSimulation(handle(currentEvent))
      currentEvent += 1
      return true
    }

    playing = false
    goodyBag.setPlayButtonState(false)
    false
  }

  private def stopTickLoop(): Unit = {
    handle.foreach(clearTimeout)
    handle = None
  }

  private def startTickLoop(): Unit = {
    def loopTheLoop(): Unit = {
      if (tick()) {
        handle = Some(setTimeout(_speed) {
          loopTheLoop()
        })
      }
    }

    loopTheLoop()
  }
}
