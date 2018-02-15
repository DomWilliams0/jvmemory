package ms.domwillia.ticker


import ms.domwillia.jvmemory.protobuf.definitions.{ClassDefinition, MethodDefinition}
import ms.domwillia.ticker.Types.{TypeName, MethodName, NodeColour}

import scala.collection.mutable
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.annotation.JSExportTopLevel
import scala.scalajs.js.typedarray.Uint8Array
import scala.util.Random

object Definitions {
  private val random = new Random()

  def generateRandomPersistentColour(clazz: TypeName, isSystem: Boolean = false): NodeColour = {
    val clazzSansArray = clazz.replace("[]", "")
    random.setSeed(clazzSansArray.hashCode)
    val hue = random.nextFloat() * 360.0
    if (!isSystem)
      s"hsl($hue, 70%, 70%)"
    else
      s"hsl($hue, 60%, 40%)"
  }
}

@JSExportTopLevel("Definitions")
class Definitions(rawDefinitions: Uint8Array) extends js.Object {
  private val definitions: mutable.Map[TypeName, (ClassDefinition, NodeColour)] = {
    val defs = Utils.parseDefinitions(rawDefinitions)
    mutable.Map() ++ defs.map(d => (d.name, (d, Definitions.generateRandomPersistentColour(d.name))))
  }

  private def createClassDef(clazz: TypeName): (ClassDefinition, NodeColour) =
    (new ClassDefinition(name = clazz), Definitions.generateRandomPersistentColour(clazz))

  def getRandomColour(clazz: TypeName): NodeColour = definitions.getOrElseUpdate(clazz, createClassDef(clazz))._2

  def getClassDefinition(clazz: TypeName): js.UndefOr[ClassDefinition] =
    definitions.get(clazz)
      .map(_._1)
      .orUndefined

  def getMethodDefinition(clazz: TypeName, name: MethodName, signature: String): js.UndefOr[MethodDefinition] =
    for {
      clazz <- getClassDefinition(clazz)
      method <- clazz.methods.find(m => m.name == name && m.signature == signature).orUndefined
    } yield method

  def findMainClass(): js.UndefOr[TypeName] =
    definitions.values
      .map(_._1)
      .find(_.methods.exists(m => m.name == "main" && m.signature == "([Ljava/lang/String;)V"))
      .map(_.name)
      .orUndefined
}
