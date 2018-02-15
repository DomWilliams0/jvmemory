package ms.domwillia.ticker

import ms.domwillia.ticker.Types.{NodeColour, StackFrameUuid, TypeName, VisualObjectId}

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.JSExportTopLevel

object ArrayMeta {
  def apply(size: Int, `type`: TypeName): Option[ArrayMeta] = size match {
    case 1 => None
    case n => Some(new ArrayMeta(n, `type`))
  }
}

class ArrayMeta(val size: Int, `type`: TypeName) extends js.Object {
  val dims: Int = `type`.sliding(2).count(_ == "[]")
}

class StackMeta(val frameUuid: StackFrameUuid, val index: Int) extends js.Object

@JSExportTopLevel("Node")
class Node(val id: VisualObjectId,
           val clazz: TypeName,
           pos: (Float, Float),
           val array: UndefOr[ArrayMeta] = js.undefined,
           val stack: UndefOr[StackMeta] = js.undefined,
           val fill: NodeColour = "none")
  extends js.Object {
  val x: Float = pos._1
  val y: Float = pos._2
}

// TODO stackmeta

@JSExportTopLevel("Link")
class Link(val source: Node, var target: Node, val name: String, val stack: UndefOr[StackMeta] = js.undefined) extends js.Object
