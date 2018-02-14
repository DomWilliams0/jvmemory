package ms.domwillia.ticker

import java.io.ByteArrayInputStream

import ms.domwillia.jvmemory.preprocessor.protobuf.vis_event.EventVariant
import ms.domwillia.jvmemory.protobuf.definitions.ClassDefinition

import scala.scalajs.js

object Utils {

  // TODO must be possible to generalise these
  def parseEvents(bytes: js.typedarray.Uint8Array): Array[EventVariant] = {
    val stream = new ByteArrayInputStream(bytes.map(_.toByte).toArray)
    EventVariant.streamFromDelimitedInput(stream).toArray
  }

  def parseDefinitions(bytes: js.typedarray.Uint8Array): Array[ClassDefinition] = {
    val stream = new ByteArrayInputStream(bytes.map(_.toByte).toArray)
    ClassDefinition.streamFromDelimitedInput(stream).toArray
  }

}
