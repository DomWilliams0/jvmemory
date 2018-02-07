#!/bin/sh
export JVMEMORY_TEST_DIR=../integration-tests
export JVMEMORY_DIR=jvmemory-all

source $JVMEMORY_TEST_DIR/venv/bin/activate
python $JVMEMORY_TEST_DIR/tests.py
deactivate
