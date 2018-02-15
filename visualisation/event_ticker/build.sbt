

name := "event_ticker"

version := "0.1"

scalaVersion := "2.12.4"

enablePlugins(ScalaJSPlugin)
//enablePlugins(ProtobufPlugin)

PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value
)

PB.protoSources in Compile := Seq(
  file("../../protobufs/monitor"),
  file("../../protobufs/vis"),
)

scalacOptions += "-P:scalajs:sjsDefinedByDefault"

libraryDependencies ++= Seq(
  "com.trueaccord.scalapb" %%% "scalapb-runtime" % com.trueaccord.scalapb.compiler.Version.scalapbVersion,
  "com.trueaccord.scalapb" %%% "scalapb-runtime" % com.trueaccord.scalapb.compiler.Version.scalapbVersion % "protobuf"
)
