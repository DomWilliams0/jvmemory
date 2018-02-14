package ms.domwillia.ticker

import ms.domwillia.jvmemory.protobuf.definitions
import ms.domwillia.jvmemory.protobuf.definitions.MethodDefinition

import scala.collection.mutable
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.JSExportTopLevel
import scala.util.Try


object StackFrame {
  var nextFrameUuid = 100
}

class LocalVar(val name: String, val `type`: String, val index: Int) extends js.Object

class StackFrame(clazz: String, method: MethodDefinition) extends js.Object {
  val clazzLong: String = clazz
  val clazzShort: String = s"Short($clazzLong)" // TODO
  val name: String = method.name
  val signature: String = method.signature
  val localVars: js.Array[LocalVar] = method.localVars.map(convertLocalVar).toJSArray
  val y: Int = 0

  val uuid: Int = {
    StackFrame.nextFrameUuid += 1
    StackFrame.nextFrameUuid
  }


  private def convertLocalVar(proto: definitions.LocalVariable): LocalVar = new LocalVar(proto.name, proto.`type`, proto.index)
}

@JSExportTopLevel("CallStack")
class CallStack extends js.Object {
  private val _callstack = js.Array[StackFrame]()
  private val frameMap = mutable.Map[Int, StackFrame]()

  def top(): js.UndefOr[StackFrame] = _callstack.lastOption.orUndefined

  def push(frame: StackFrame): Unit = {
    _callstack.push(frame)
    frameMap.put(frame.uuid, frame)
  }

  def pop(): UndefOr[StackFrame] =
    (for {
      top <- Try(_callstack.pop()).toOption
      _ <- frameMap.remove(top.uuid)
    } yield top).orUndefined

  def getFrame(uuid: Int): UndefOr[StackFrame] = frameMap.get(uuid).orUndefined

  def callstack: js.Array[StackFrame] = _callstack
}
