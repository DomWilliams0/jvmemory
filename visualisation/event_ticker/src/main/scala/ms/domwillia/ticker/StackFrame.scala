package ms.domwillia.ticker

import ms.domwillia.jvmemory.protobuf.definitions
import ms.domwillia.jvmemory.protobuf.definitions.{ClassDefinition, MethodDefinition}

import scala.collection.mutable
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.annotation.JSExportTopLevel
import scala.scalajs.js.{Iterator, UndefOr}
import scala.util.Try


object StackFrame {
  var nextFrameUuid = 100
}

class LocalVar(name: String, `type`: String, index: Int) extends js.Object

class StackFrame(clazz: ClassDefinition, method: MethodDefinition) extends js.Object {
  val clazzLong: String = clazz.name
  val clazzShort: String = s"Short($clazzLong)" // TODO
  val name: String = method.name
  val signature: String = method.signature
  val localVars: Seq[LocalVar] = method.localVars.map(convertLocalVar)
  val y: Int = 0

  val uuid: Int = {
    StackFrame.nextFrameUuid += 1
    StackFrame.nextFrameUuid
  }


  private def convertLocalVar(proto: definitions.LocalVariable): LocalVar = new LocalVar(proto.name, proto.`type`, proto.index)
}

@JSExportTopLevel("CallStack")
class CallStack extends js.Object {
  private val callstack = mutable.Stack[StackFrame]()
  private val frameMap = mutable.Map[Int, StackFrame]()

  def top(): js.UndefOr[StackFrame] = Try(callstack.top).toOption.orUndefined

  def push(frame: StackFrame): Unit = {
    callstack.push(frame)
    frameMap.put(frame.uuid, frame)
    println(s"push ${frame.toString}")
  }

  def pop(): UndefOr[StackFrame] =
    (for {
      top <- Try(callstack.pop()).toOption
      _ <- frameMap.remove(top.uuid)
    } yield top).orUndefined

  def getFrame(uuid: Int): UndefOr[StackFrame] = frameMap.get(uuid).orUndefined


  def getCallStack(): js.Iterable[StackFrame] = callstack.toJSIterable
}
