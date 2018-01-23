#!/bin/sh

INC=../monitor-agent/src/main/proto
IN=../preprocessor/src/main/proto
OUT=src/gen

mkdir -p $OUT
protoc --proto_path=$IN:$INC --js_out=library=jvmemory,binary:$OUT $IN/*.proto
