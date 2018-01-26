#!/bin/sh
DIR=jvmemory-all
MAIN=$1
PACKAGES=$2

if [[ $# != 2 ]]; then
	echo "Usage: $0 <main class> <comma separated list of packages to instrument>"
	exit 1
fi

i=0
while true; do
	PROJ=jvmproj-$MAIN-$i
	mkdir $PROJ 2>/dev/null && break
	i=$((i+1))
done
echo Saving output in directory $PROJ

set -x
java \
	-javaagent:$DIR/agent.jar=$DIR/bootstrap.jar,$PACKAGES \
	-agentpath:$DIR/libagent.so=$PROJ/jvmemory.log \
	$MAIN

(
echo Opening visualisation in browser in 1 second
sleep 1
xdg-open $DIR/vis-src/vis.html
) &

echo Starting visualisation server
java -jar $DIR/server.jar $PROJ/jvmemory.log $PROJ/vis-events $DIR/vis-src
