# script args
INPUT=../monitor-agent/jvmemory.log

# makefile vars
SCRIPT=./preprocessor.py
ENV=env
PROTOBUF_SRC=../monitor-agent/src/main/proto
PROTOBUF_MAIN=message_pb2.py
PROTOBUF_OUT=proto

.PHONY: run
run: ${PROTOBUF_OUT}
	@${SCRIPT} ${INPUT}

${PROTOBUF_OUT}:
	@mkdir -p $@
	protoc -I=${PROTOBUF_SRC} \
		--python_out=$@ \
		${PROTOBUF_SRC}/*
	sed -i 's/^import.*__pb2$$/from . \0/' $@/${PROTOBUF_MAIN}

${ENV}: ${ENV}/bin/activate

${ENV}/bin/activate: requirements.txt
	test -d ${ENV} || virtualenv -p python3.6 ${ENV}
	${ENV}/bin/pip install -Ur requirements.txt
	touch ${ENV}/bin/activate


.PHONY: clean
clean:
	rm -rf ${PROTOBUF_OUT}
