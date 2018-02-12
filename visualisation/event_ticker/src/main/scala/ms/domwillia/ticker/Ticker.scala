package ms.domwillia.ticker

import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

@JSExportTopLevel("Ticker")
class Ticker {
  @JSExport
  def doNothing(): Unit = println("it works!")
}
