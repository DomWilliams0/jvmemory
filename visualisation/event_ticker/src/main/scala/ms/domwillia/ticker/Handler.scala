package ms.domwillia.ticker

import ms.domwillia.jvmemory.preprocessor.protobuf.vis_event.EventVariant._
import ms.domwillia.jvmemory.preprocessor.protobuf.vis_event._
import ms.domwillia.jvmemory.protobuf.definitions.MethodDefinition
import ms.domwillia.ticker.HandleResult.HandleResult
import ms.domwillia.ticker.Types._

import scala.collection.mutable
import scala.language.implicitConversions
import scala.scalajs.js.JSConverters._

object HandleResult extends Enumeration {
  type HandleResult = Value
  val NoGraphChange, ChangedStackOnly, ChangedGraph = Value
}

case class StackFrameBuilder(calledObj: VisualObjectId, className: TypeName, methodDef: MethodDefinition)

class Handler(val goodyBag: GoodyBag) {
  implicit def id2int(id: InternalObjectId): VisualObjectId = id.toString

  // state keeping for rewinding time
  private val linkHistory = mutable.HashMap[(InternalObjectId, String), mutable.ArrayStack[InternalObjectId]]()
  private val stackHistory = mutable.ArrayStack[StackFrameBuilder]()

  def handle(payload: Payload, forwards: Boolean): HandleResult = payload match {
    case Payload.AddHeapObject(value) => if (forwards) handleImpl(value) else undoImpl(value)
    case Payload.DelHeapObject(value) => if (forwards) handleImpl(value) else undoImpl(value)
    case Payload.SetIntraHeapLink(value) => if (forwards) handleImpl(value) else undoImpl(value)
    case Payload.SetLocalVarLink(value) => if (forwards) handleImpl(value) else undoImpl(value)
    case Payload.ShowLocalVarAccess(value) => if (forwards) handleImpl(value) else undoImpl(value)
    case Payload.ShowHeapObjectAccess(value) => if (forwards) handleImpl(value) else undoImpl(value)
    case Payload.PushMethodFrame(value) => if (forwards) handleImpl(value) else undoImpl(value)
    case Payload.PopMethodFrame(value) => if (forwards) handleImpl(value) else undoImpl(value)
    case Payload.SetStatic(value) => if (forwards) handleImpl(value) else undoImpl(value)
    case Payload.ToStringObject(value) => if (forwards) handleImpl(value) else undoImpl(value)
    case x => println(s"unknown event: $x"); HandleResult.NoGraphChange
  }

  private def handleImpl(value: AddHeapObject): HandleResult = {
    val colour = goodyBag.definitions.getRandomColour(value._class)
    val array = ArrayMeta(value.arraySize, value._class)
    val node = new Node(value.id, value._class, heapCentre, array = array.orUndefined, fill = colour)
    goodyBag.nodes().push(node)

    HandleResult.ChangedGraph
  }

  private def undoImpl(value: AddHeapObject): HandleResult = handleImpl(new DelHeapObject(value.id))

  private def handleImpl(value: DelHeapObject): HandleResult = {
    goodyBag.removeHeapNode(value.id)
    HandleResult.ChangedGraph
  }

  private def undoImpl(value: DelHeapObject): HandleResult = {
    // TODO find corresponding addheapobject event
    HandleResult.NoGraphChange
  }

  private def handleImpl(value: SetIntraHeapLink): HandleResult = {
    val list = linkHistory.getOrElseUpdate((value.srcId, value.fieldName), new mutable.ArrayStack[InternalObjectId]())
    list.push(value.dstId)

    updateLink(
      l => l.source.id == implicitly[VisualObjectId](value.srcId) && l.name == value.fieldName,
      value.dstId,
      new Link(findNode(value.srcId), findNode(value.dstId), value.fieldName)
    )
  }

  private def undoImpl(value: SetIntraHeapLink): HandleResult = {
    val newTarget = linkHistory
      .get((value.srcId, value.fieldName))
      .map(_.pop())
      .getOrElse(throw new IllegalStateException("missing link history"))

    updateLink(
      l => l.source.id == implicitly[VisualObjectId](value.srcId) && l.name == value.fieldName,
      newTarget,
      new Link(findNode(value.srcId), findNode(newTarget), value.fieldName)
    )
  }

  private def handleImpl(value: SetLocalVarLink): HandleResult = {
    def bail(): HandleResult.Value = {
      println(s"bad local var ${value.varIndex} while setting local var link")
      HandleResult.NoGraphChange
    }

    val deleting = value.dstId == 0
    val (uuid, localVar) = (for {
      frame <- goodyBag.callStack.topPerhaps().toOption
      localVar <- frame.localVars.find(_.index == value.varIndex)
    } yield (frame.uuid, localVar)).getOrElse(return bail())

    val stackMeta = new StackMeta(uuid, value.varIndex)
    val nodeId = getStackNodeId(uuid, value.varIndex)

    // add stack node if doesn't already exist
    if (!deleting) {
      if (!goodyBag.nodes().exists(_.id == nodeId))
        goodyBag.nodes().push(new Node(nodeId, "", heapCentre, stack = stackMeta))
    }

    updateLink(
      _.stack
        .map(s => s.frameUuid == uuid && s.index == value.varIndex)
        .getOrElse(false),
      value.dstId,
      new Link(findNode(nodeId), findNode(value.dstId), localVar.name, stackMeta)
    )
  }

  private def undoImpl(value: SetLocalVarLink): HandleResult = {
    HandleResult.NoGraphChange
  }

  private def handleImpl(value: ShowLocalVarAccess): HandleResult = {
    val frame = goodyBag.callStack.top()
    val nodeId = getStackNodeId(frame.uuid, value.varIndex)

    goodyBag.highlightLocalVar(nodeId, value.read)
    HandleResult.NoGraphChange
  }

  private def undoImpl(value: ShowLocalVarAccess): HandleResult = {
    HandleResult.NoGraphChange
  }

  private def handleImpl(value: ShowHeapObjectAccess): HandleResult = {
    goodyBag.highlightHeapObj(value.objId, value.fieldName, value.read)
    HandleResult.NoGraphChange
  }

  private def undoImpl(value: ShowHeapObjectAccess): HandleResult = HandleResult.NoGraphChange

  private def handleImpl(value: PushMethodFrame): HandleResult = {
    setTopOfCallStackCalled(false)

    goodyBag.definitions.getMethodDefinition(value.owningClass, value.name, value.signature)
      .map(new StackFrame(value.objId, value.owningClass, _))
      .foreach(goodyBag.callStack.push)

    setTopOfCallStackCalled(true)

    HandleResult.ChangedStackOnly
  }

  private def undoImpl(value: PushMethodFrame): HandleResult = {
    popStackFrame()
    HandleResult.ChangedStackOnly
  }

  private def handleImpl(value: PopMethodFrame): HandleResult = {
    setTopOfCallStackCalled(false)

    val frame = popStackFrame()
    stackHistory.push(StackFrameBuilder(frame.calledObject, frame.clazzLong, frame.method))

    setTopOfCallStackCalled(true)

    HandleResult.ChangedGraph
  }

  private def undoImpl(value: PopMethodFrame): HandleResult = {
    val history = stackHistory.pop()
    pushStackFrame(history)
    HandleResult.ChangedGraph
  }

  private def handleImpl(value: SetStatic): HandleResult = {

    if (value.oldObjId != 0L) {
      findNode(value.oldObjId).static = false

      if (value.newObjId != 0L) {
        // redirect existing links
        val newNode = findNode(value.newObjId)
        goodyBag.links().filter(_.target.id == value.oldObjId).foreach(_.target = newNode)
        newNode.static = true
      } else {
        // delete links
        goodyBag.removeLinks(value.oldObjId)
      }
    } else if (value.newObjId != 0L) {
      // create new links
      val links = goodyBag.links()
      val target = findNode(value.newObjId)
      goodyBag.nodes()
        .filter(_.clazz == value._class)
        .foreach(n => links.push(new Link(n, target, value.fieldName)))
      target.static = true
    }

    HandleResult.ChangedGraph
  }

  private def undoImpl(value: SetStatic): HandleResult = {

    // TODO
    HandleResult.NoGraphChange
  }

  private def handleImpl(value: ToStringObject): HandleResult = {
    findNode(value.objId).str = value.str
    HandleResult.NoGraphChange
  }

  private def undoImpl(value: ToStringObject): HandleResult = {
    // TODO
    HandleResult.NoGraphChange
  }


  private def findNode(id: VisualObjectId): Node = goodyBag.nodes().find(_.id == id).getOrElse(throw new IllegalArgumentException(s"bad node id $id"))

  private def updateLink(which: Link => Boolean, dstNode: InternalObjectId, newLink: => Link): HandleResult = {
    val existingIndex = goodyBag.links().indexWhere(which)
    val deleting = dstNode == 0
    if (existingIndex >= 0) {
      if (deleting) goodyBag.links().remove(existingIndex) // delete existing
      else goodyBag.links()(existingIndex).target = findNode(dstNode) // update existing
    } else if (!deleting) goodyBag.links().push(newLink) // add new

    HandleResult.ChangedGraph
  }

  private def getStackNodeId(uuid: StackFrameUuid, index: Int) = s"stack-$uuid-$index"

  private def heapCentre: (Float, Float) = goodyBag.getHeapCentre() match {
    case arr if arr.length == 2 => (arr(0), arr(1))
    case _ => throw new IllegalStateException("heap centre must be [x, y]")
  }

  private def pushStackFrame(builder: StackFrameBuilder): Unit =
    goodyBag.callStack.push(new StackFrame(builder.calledObj, builder.className, builder.methodDef))

  private def popStackFrame(): StackFrame = {
    val frame = goodyBag.callStack.pop()
    goodyBag.removeStackNodes(frame.uuid)
    frame
  }

  private def setTopOfCallStackCalled(calling: Boolean) =
    goodyBag.callStack.topPerhaps()
      .filter(_.calledObject != "0")
      .map(frame => goodyBag.setCalledObjHighlighted(frame.calledObject, calling))
}
