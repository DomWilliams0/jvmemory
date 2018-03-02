package ms.domwillia.ticker

import ms.domwillia.jvmemory.protobuf.definitions
import ms.domwillia.jvmemory.protobuf.definitions.MethodDefinition
import ms.domwillia.ticker.Types.{MethodName, StackFrameUuid, TypeName, VisualObjectId}

import scala.collection.mutable
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.JSExportTopLevel
import scala.util.Try


object StackFrame {
  var nextFrameUuid: StackFrameUuid = 100

  def allocateFrameUuid(): StackFrameUuid = {
    val uuid = nextFrameUuid
    nextFrameUuid += 1
    uuid
  }

}

class LocalVar(val name: String, val `type`: TypeName, val index: Int) extends js.Object

class StackFrame(val calledObject: VisualObjectId, clazz: TypeName, val method: MethodDefinition) extends js.Object {
  val clazzLong: TypeName = clazz
  val clazzShort: TypeName = s"Short($clazzLong)" // TODO
  val name: MethodName = method.name
  val signature: String = method.signature
  val localVars: js.Array[LocalVar] = method.localVars.map(convertLocalVar).toJSArray
  val y: Int = 0

  val uuid: StackFrameUuid = StackFrame.allocateFrameUuid()


  private def convertLocalVar(proto: definitions.LocalVariable): LocalVar = new LocalVar(proto.name, proto.`type`, proto.index)
}

@JSExportTopLevel("CallStack")
class CallStack extends js.Object {
  private val _callstack = js.Array[StackFrame]()
  private val frameMap = mutable.Map[StackFrameUuid, StackFrame]()

  def topPerhaps(): js.UndefOr[StackFrame] = _callstack.lastOption.orUndefined

  def top(): StackFrame = topPerhaps().getOrElse(throw new IllegalStateException("callstack is empty"))

  def push(frame: StackFrame): Unit = {
    _callstack.push(frame)
    frameMap.put(frame.uuid, frame)
  }

  def popPerhaps(): UndefOr[StackFrame] =
    (for {
      top <- Try(_callstack.pop()).toOption
      _ <- frameMap.remove(top.uuid)
    } yield top).orUndefined

  def pop(): StackFrame = popPerhaps().getOrElse(throw new IllegalStateException("callstack is empty"))

  def getFrame(uuid: StackFrameUuid): UndefOr[StackFrame] = frameMap.get(uuid).orUndefined

  def callstack: js.Array[StackFrame] = _callstack
}
