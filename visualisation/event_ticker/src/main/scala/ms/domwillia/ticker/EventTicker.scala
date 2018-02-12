package ms.domwillia.ticker

import ms.domwillia.jvmemory.preprocessor.protobuf.vis_event.EventVariant

import scala.scalajs.js
import scala.scalajs.js.Dynamic
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}


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
  val onPlayOrPause: js.Function1[Boolean, Unit] = js.native
  val onSetSimState: js.Function1[Boolean, Unit] = js.native
  val highlightLocalVar: js.Function2[Long, Boolean, Unit] = js.native
  val highlightHeapObj: js.Function3[Long, String, Boolean, Unit] = js.native
}

object Constants {
  // ms between events
  val MaxSpeed = 1
  val MinSpeed = 500
}

@JSExportTopLevel("EventTicker")
class EventTicker(val events: js.Array[EventVariant], val references: References, val callbacks: Callbacks) {
  Dynamic.global.console.log("events=%o refs=%o callbacks=%o", events, references, callbacks)

  var _speed = 50 // TODO placeholder
  var playing = false

  @JSExport
  val speed: Int = _speed

  @JSExport
  def speed_=(value: Int): Unit = _speed = Constants.MaxSpeed max value min Constants.MinSpeed

  @JSExport
  def resume(): Unit = {
    println("resume")
  }

  @JSExport
  def pause(): Unit = {
    println("pause")
  }

  @JSExport
  def toggle(): Unit = {
    playing = !playing
    callbacks.onPlayOrPause(!playing)
    if (playing) pause() else resume()
  }
}
