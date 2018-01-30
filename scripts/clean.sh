#!/bin/sh -e
ROOT=$PWD

# agent
(
cd $ROOT/monitor-agent
./gradlew clean
)

# preprocessor and vis server
(
cd $ROOT/visualisation-server
./gradlew clean
)
