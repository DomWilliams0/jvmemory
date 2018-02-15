package ms.domwillia.ticker

import ms.domwillia.jvmemory.preprocessor.protobuf.vis_event.EventVariant._
import ms.domwillia.jvmemory.preprocessor.protobuf.vis_event._
import ms.domwillia.ticker.HandleResult.HandleResult
import ms.domwillia.ticker.Types._

import scala.language.implicitConversions
import scala.scalajs.js.JSConverters._

object HandleResult extends Enumeration {
  type HandleResult = Value
  val NoGraphChange, ChangedStackOnly, ChangedGraph = Value
}

class Handler(val goodyBag: GoodyBag) {
  implicit def id2int(id: InternalObjectId): VisualObjectId = Math.toIntExact(id)

  def handle(payload: Payload): HandleResult = payload match {
    case Payload.AddHeapObject(value) => handleImpl(value)
    case Payload.DelHeapObject(value) => handleImpl(value)
    case Payload.SetIntraHeapLink(value) => handleImpl(value)
    case Payload.SetLocalVarLink(value) => handleImpl(value)
    case Payload.ShowLocalVarAccess(value) => handleImpl(value)
    case Payload.ShowHeapObjectAccess(value) => handleImpl(value)
    case Payload.PushMethodFrame(value) => handleImpl(value)
    case Payload.PopMethodFrame(value) => handleImpl(value)
    case x => println(s"unknown event: $x"); HandleResult.NoGraphChange
  }

  private def handleImpl(value: AddHeapObject): HandleResult = {
    val colour = goodyBag.definitions.getRandomColour(value._class)
    val array = ArrayMeta(value.arraySize, value._class)
    val node = new Node(value.id, value._class, array.orUndefined, None.orUndefined, colour)
    goodyBag.nodes.push(node)

    HandleResult.ChangedGraph
  }

  private def handleImpl(value: DelHeapObject): HandleResult = HandleResult.NoGraphChange

  private def handleImpl(value: SetIntraHeapLink): HandleResult = {
    val deleting = value.dstId == 0

    val existingIndex = goodyBag.links.indexWhere(l => l.source.id == value.srcId && l.name == value.fieldName)

    if (existingIndex >= 0) {
      if (deleting) goodyBag.links.remove(existingIndex) // delete existing
      else goodyBag.links(existingIndex).target = findNode(value.dstId) // update existing
    } else if (!deleting) goodyBag.links.push(new Link(findNode(value.srcId), findNode(value.dstId), value.fieldName)) // add new

    HandleResult.ChangedGraph
  }

  private def handleImpl(value: SetLocalVarLink): HandleResult = HandleResult.NoGraphChange

  private def handleImpl(value: ShowLocalVarAccess): HandleResult = HandleResult.NoGraphChange

  private def handleImpl(value: ShowHeapObjectAccess): HandleResult = {
    goodyBag.highlightHeapObj(value.objId, value.fieldName, value.read)
    HandleResult.NoGraphChange
  }

  private def handleImpl(value: PushMethodFrame): HandleResult = {
    goodyBag.definitions.getMethodDefinition(value.owningClass, value.name, value.signature)
      .map(new StackFrame(value.owningClass, _))
      .foreach(goodyBag.callStack.push)

    HandleResult.NoGraphChange
  }

  private def handleImpl(value: PopMethodFrame): HandleResult = {
    val popped = goodyBag.callStack.pop()

    // TODO remove stack links

    HandleResult.ChangedStackOnly
  }

  private def findNode(id: InternalObjectId): Node = goodyBag.nodes.find(_.id == id).getOrElse(throw new IllegalArgumentException(s"bad node id $id"))
}
