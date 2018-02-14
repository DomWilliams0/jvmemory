package ms.domwillia.ticker

import ms.domwillia.jvmemory.preprocessor.protobuf.vis_event.EventVariant._
import ms.domwillia.jvmemory.preprocessor.protobuf.vis_event._
import ms.domwillia.ticker.HandleResult.HandleResult

import scala.scalajs.js
import scala.scalajs.js.JSConverters._

object HandleResult extends Enumeration {
  type HandleResult = Value
  val NoGraphChange, ChangedStackOnly, ChangedGraph = Value
}

class Handler(val callStack: CallStack,
              val definitions: Definitions,
              val nodes: js.Array[Node],
              val links: js.Array[Link]) {
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
    val colour = definitions.getRandomColour(value._class)
    val array = ArrayMeta(value.arraySize, value._class)
    val node = new Node(value.id, value._class, array.orUndefined, None.orUndefined, colour)
    nodes.push(node)

    HandleResult.ChangedGraph
  }

  private def handleImpl(value: DelHeapObject): HandleResult = HandleResult.ChangedGraph

  private def handleImpl(value: SetIntraHeapLink): HandleResult = {
    val deleting = value.dstId == 0

    val existingIndex = links.indexWhere(l => l.source.id == value.srcId && l.name == value.fieldName)

    if (existingIndex >= 0) {
      if (deleting) links.remove(existingIndex) // delete existing
      else links(existingIndex).target = findNode(value.dstId) // update existing
    } else if (!deleting) links.push(new Link(findNode(value.srcId), findNode(value.dstId), value.fieldName)) // add new

    HandleResult.ChangedGraph
  }

  private def handleImpl(value: SetLocalVarLink): HandleResult = HandleResult.ChangedGraph

  private def handleImpl(value: ShowLocalVarAccess): HandleResult = HandleResult.ChangedGraph

  private def handleImpl(value: ShowHeapObjectAccess): HandleResult = HandleResult.ChangedGraph

  private def handleImpl(value: PushMethodFrame): HandleResult = {
    definitions.getMethodDefinition(value.owningClass, value.name, value.signature)
      .map(new StackFrame(value.owningClass, _))
      .foreach(callStack.push)

    HandleResult.NoGraphChange
  }

  private def handleImpl(value: PopMethodFrame): HandleResult = {
    callStack.pop()

    // TODO remove stack links

    HandleResult.NoGraphChange
  }

  private def findNode(id: Long): Node = nodes.find(_.id == id).getOrElse(throw new IllegalArgumentException(s"bad node id $id"))
}
