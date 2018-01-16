ROOT=$HOME/jvmemory/monitor-agent/
MVN=$HOME/.m2/repository
JAVA=/usr/lib/jvm/java-8-openjdk
QUE=${1:-ms.domwillia.specimen.SpecimenRunner}
CP=$2

echo Running $QUE
cd $ROOT

$JAVA/bin/java \
-javaagent:out/artifacts/jvmemory_jar/jvmemory.jar \
-agentpath:../jvmti-agent/libagent.so \
-classpath \
target/classes:\
$MVN/org/ow2/asm/asm/6.0/asm-6.0.jar:\
$MVN/org/ow2/asm/asm-commons/6.0/asm-commons-6.0.jar:\
$MVN/org/ow2/asm/asm-tree/6.0/asm-tree-6.0.jar:\
$MVN/org/jetbrains/kotlin/kotlin-stdlib-jre8/1.2.10/kotlin-stdlib-jre8-1.2.10.jar:\
$MVN/org/jetbrains/kotlin/kotlin-stdlib/1.2.10/kotlin-stdlib-1.2.10.jar:\
$MVN/org/jetbrains/annotations/13.0/annotations-13.0.jar:\
$MVN/org/jetbrains/kotlin/kotlin-stdlib-jre7/1.2.10/kotlin-stdlib-jre7-1.2.10.jar:\
$MVN/com/google/protobuf/protobuf-java/3.4.0/protobuf-java-3.4.0.jar \
$QUE
