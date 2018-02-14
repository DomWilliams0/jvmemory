package ms.domwillia.ticker

import scala.scalajs.js
import scala.scalajs.js.{Dynamic, UndefOr}
import scala.scalajs.js.annotation.JSExportTopLevel

/*@js.native
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
}*/

object ArrayMeta {
  def apply(size: Int, `type`: String): Option[ArrayMeta] = size match {
    case 1 => None
    case n => Some(new ArrayMeta(n, `type`))
  }
}

class ArrayMeta(val size: Int, `type`: String) extends js.Object {
  val dims: Int = `type`.sliding(2).count(_ == "[]")
}

@JSExportTopLevel("Node")
class Node(val id: Long, val clazz: String, val array: UndefOr[ArrayMeta], val stack: UndefOr[Any], val fill: String)
  extends js.Object

// TODO stackmeta

@JSExportTopLevel("Link")
class Link extends js.Object {

}
