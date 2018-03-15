package ms.domwillia.ticker

object Types {
  type InternalObjectId = Long
  type VisualObjectId = String
  type StackFrameUuid = Int
  type TypeName = String
  type MethodName = String
  type NodeColour = String

  def shortenTypeName(typeName: TypeName): TypeName = typeName.lastIndexOf('.') match {
    case -1 => typeName
    case i => typeName.substring(i + 1)
  }
}
