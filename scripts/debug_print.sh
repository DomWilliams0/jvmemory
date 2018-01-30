#!/bin/sh
ROOT=jvmemory-all
java -cp $ROOT/server.jar ms.domwillia.jvmemory.preprocessor.DebugPrinter "$@"

