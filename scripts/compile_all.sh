#!/bin/sh -e
ROOT=..
OUT=$PWD/jvmemory-all

# TODO versioning
# TODO release builds

JAVA_AGENT=$OUT/agent.jar
BOOTSTRAP=$OUT/bootstrap.jar
LIBAGENT=$OUT/libagent.so
SERVER=$OUT/server.jar
VIS=$OUT/vis-src
RUN=$OUT/run.sh

echo "Compiling and copying all binaries to $OUT"

CLEAN=0
if [[ $1 = "clean" ]]; then
	CLEAN=1
	echo "*** CLEANING FIRST ***"
fi

rm -rf $OUT
mkdir -p $OUT

# agent
(
cd $ROOT/monitor-agent
[ $CLEAN = 1 ] && ./gradlew clean
./gradlew buildJar

cp -f build/libs/monitor-agent-0.1.jar $JAVA_AGENT
echo Java agent copied to $JAVA_AGENT

cp -f $JAVA_AGENT $BOOTSTRAP
zip --quiet -d $BOOTSTRAP ms/domwillia/jvmemory/modify/*
echo Bootstrap created at $BOOTSTRAP

cp -f $ROOT/jvmti-agent/libagent.so $LIBAGENT
echo Native agent copied to $LIBAGENT
) &

# preprocessor and vis server
(
cd $ROOT/visualisation-server
[ $CLEAN = 1 ] && ./gradlew clean
./gradlew buildJar

cp -f build/libs/visualisation-server-0.1.jar $SERVER
echo Visualisation server copied to $SERVER
) &

# vis src
(
cp -f -r $ROOT/visualisation/src $VIS
echo Visualisation sources copied to $VIS
) &

# scala js
(
cd $ROOT/visualisation/event_ticker
sbt fastOptJS
cp -f target/scala-2.12/event_ticker-fastopt.js $VIS/event_ticker.js
) &

cp -f -v $ROOT/scripts/run.sh $RUN

wait
echo All done, run $RUN
