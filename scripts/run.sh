#!/bin/sh

# using this script standalone will use $PWD, but using the Makefile will substitute
# this for the true install dir
INSTALL_DIR=$PWD
DIR=$INSTALL_DIR

MAIN=$1
PACKAGES=$2
# TODO project source?

if [[ $# != 2 && $# != 3 && $# != 4 ]]; then
	echo "Usage: $0 <main class> <comma separated list of packages to instrument> [open]"
	exit 1
fi

OPEN=0
if [[ $3 = "open" ]]; then
	OPEN=1
fi

i=1
while true; do
	PROJ=jvmproj-$MAIN-$i
	mkdir $PROJ 2>/dev/null && break
	i=$((i+1))
done
echo Saving output in directory $PROJ

set -e
java \
	-javaagent:$DIR/agent.jar=$DIR/bootstrap.jar,$PACKAGES \
	-agentpath:$DIR/libagent.so=$PROJ/jvmemory.log \
	$MAIN

if [[ $OPEN = 1 ]]; then
	(
	echo Opening visualisation in browser in 1 second
	sleep 1
	xdg-open $DIR/visualisation/vis.html
	) &
fi

echo Starting visualisation server
java -jar $DIR/preprocessor.jar $PROJ/jvmemory.log $PROJ/vis-events $DIR/visualisation
