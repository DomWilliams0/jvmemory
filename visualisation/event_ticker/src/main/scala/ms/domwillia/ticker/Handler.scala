package ms.domwillia.ticker

import ms.domwillia.jvmemory.preprocessor.protobuf.vis_event.EventVariant._
import ms.domwillia.jvmemory.preprocessor.protobuf.vis_event._
import ms.domwillia.ticker.HandleResult.HandleResult

object HandleResult extends Enumeration {
  type HandleResult = Value
  val NoChange, ChangedStackOnly, ChangedGraph = Value
}

object Handler {
  def handle(payload: Payload): HandleResult = payload match {
    case Payload.AddHeapObject(value) => handleImpl(value)
    case Payload.DelHeapObject(value) => handleImpl(value)
    case Payload.SetIntraHeapLink(value) => handleImpl(value)
    case Payload.SetLocalVarLink(value) => handleImpl(value)
    case Payload.ShowLocalVarAccess(value) => handleImpl(value)
    case Payload.ShowHeapObjectAccess(value) => handleImpl(value)
    case Payload.PushMethodFrame(value) => handleImpl(value)
    case Payload.PopMethodFrame(value) => handleImpl(value)
    case x => println(s"unknown event: $x"); HandleResult.NoChange
  }

  private def handleImpl(value: AddHeapObject): HandleResult = HandleResult.ChangedGraph

  private def handleImpl(value: DelHeapObject): HandleResult = HandleResult.ChangedGraph

  private def handleImpl(value: SetIntraHeapLink): HandleResult = HandleResult.ChangedGraph

  private def handleImpl(value: SetLocalVarLink): HandleResult = HandleResult.ChangedGraph

  private def handleImpl(value: ShowLocalVarAccess): HandleResult = HandleResult.ChangedGraph

  private def handleImpl(value: ShowHeapObjectAccess): HandleResult = HandleResult.ChangedGraph

  private def handleImpl(value: PushMethodFrame): HandleResult = HandleResult.ChangedGraph

  private def handleImpl(value: PopMethodFrame): HandleResult = HandleResult.ChangedGraph
}
