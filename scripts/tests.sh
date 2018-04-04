#!/bin/sh -e
export JVMEMORY_TEST_DIR=../integration-tests
export JVMEMORY_DIR=../build/jvmemory-0.1
export JVMEMORY_WORKING_DIR=/tmp/jvmemory-tests-$USER

PROTOBUF_SRC=../protobufs
PROTOBUF_OUT=$JVMEMORY_TEST_DIR/pb

# create working directory
rm -rf $JVMEMORY_WORKING_DIR
mkdir -p $JVMEMORY_WORKING_DIR $PROTOBUF_OUT

# generate protobufs
rm -f $PROTOBUF_OUT/*.py
protoc \
	-I=$PROTOBUF_SRC/monitor \
	-I=$PROTOBUF_SRC/vis \
	--python_out=$PROTOBUF_OUT \
	$(find $PROTOBUF_SRC -name "*.proto")

sed -i 's/^import.*_pb2/from . \0/' $PROTOBUF_OUT/message_pb2.py

VENV=$JVMEMORY_TEST_DIR/venv
if [[ ! -d $VENV ]]; then
	virtualenv -p python3 $VENV
	source $VENV/bin/activate
	pip install -r $JVMEMORY_TEST_DIR/requirements.txt
else
	source $VENV/bin/activate
fi
python $JVMEMORY_TEST_DIR/tests.py
deactivate
