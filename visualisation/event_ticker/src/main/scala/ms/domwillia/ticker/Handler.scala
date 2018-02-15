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
  implicit def id2int(id: InternalObjectId): VisualObjectId = id.toString

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
    val node = new Node(value.id, value._class, array = array.orUndefined, fill = colour)
    goodyBag.nodes.push(node)

    HandleResult.ChangedGraph
  }

  private def handleImpl(value: DelHeapObject): HandleResult = {
    goodyBag.removeHeapNode(value.id)
    HandleResult.ChangedGraph
  }

  private def handleImpl(value: SetIntraHeapLink): HandleResult = updateLink(
    l => l.source.id == implicitly[VisualObjectId](value.srcId) && l.name == value.fieldName,
    value.dstId,
    new Link(findNode(value.srcId), findNode(value.dstId), value.fieldName)
  )

  private def handleImpl(value: SetLocalVarLink): HandleResult = {
    def bail(): HandleResult.Value = {
      println(s"bad local var ${value.varIndex} while setting local var link")
      HandleResult.NoGraphChange
    }

    val deleting = value.dstId == 0
    val (uuid, localVar) = (for {
      frame <- goodyBag.callStack.top().toOption
      localVar <- frame.localVars.find(_.index == value.varIndex)
    } yield (frame.uuid, localVar)).getOrElse(return bail())

    val stackMeta = new StackMeta(uuid, value.varIndex)
    val nodeId = getStackNodeId(uuid, value.varIndex)

    // add stack node if doesn't already exist
    if (!deleting) {
      if (!goodyBag.nodes.exists(_.id == nodeId))
        goodyBag.nodes.push(new Node(nodeId, "", stack = stackMeta))
    }

    updateLink(
      _.stack
        .map(s => s.frameUuid == uuid && s.index == value.varIndex)
        .getOrElse(false),
      value.dstId,
      new Link(findNode(nodeId), findNode(value.dstId), localVar.name, stackMeta)
    )
  }

  private def handleImpl(value: ShowLocalVarAccess): HandleResult = {
    val frame = goodyBag.callStack.top().getOrElse(throw new IllegalStateException("empty callstack"))
    val nodeId = getStackNodeId(frame.uuid, value.varIndex)

    goodyBag.highlightLocalVar(nodeId, value.read)
    HandleResult.NoGraphChange
  }

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
    val popped = goodyBag.callStack.pop().getOrElse(throw new IllegalStateException("no method to pop"))

    goodyBag.removeStackNodes(popped.uuid)

    HandleResult.ChangedStackOnly
  }

  private def findNode(id: VisualObjectId): Node = goodyBag.nodes.find(_.id == id).getOrElse(throw new IllegalArgumentException(s"bad node id $id"))

  private def updateLink(which: Link => Boolean, dstNode: InternalObjectId, newLink: => Link): HandleResult = {
    val existingIndex = goodyBag.links.indexWhere(which)
    val deleting = dstNode == 0
    if (existingIndex >= 0) {
      if (deleting) goodyBag.links.remove(existingIndex) // delete existing
      else goodyBag.links(existingIndex).target = findNode(dstNode) // update existing
    } else if (!deleting) goodyBag.links.push(newLink) // add new

    HandleResult.ChangedGraph
  }

  private def getStackNodeId(uuid: StackFrameUuid, index: Int) = s"stack-$uuid-$index"
}
