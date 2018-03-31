VERSION=0.1
BUILD_ROOT=build
TARGET_DIR=$(BUILD_ROOT)/jvmemory-$(VERSION)
ROOT?=$(PWD)

GRADLE_FLAGS=--daemon --parallel

# protobufs
# PROTO_ROOT=$(ROOT)/protobufs
# PROTO_SRCS_JVM=$(shell find $(PROTO_ROOT/monitor) -type f)
# PROTO_SRCS_VIS=$(shell find $(PROTO_ROOT/vis) -type f)

# monitor agent
MONITOR_TRGT=$(TARGET_DIR)/agent.jar
MONITOR_ROOT=$(ROOT)/monitor-agent
MONITOR_SRCS=$(shell find $(MONITOR_ROOT)/src -type f)

# bootstrap
BOOTSTRAP_TRGT=$(TARGET_DIR)/bootstrap.jar

# native agent
NATIVE_TRGT=$(TARGET_DIR)/libagent.so
NATIVE_ROOT=$(ROOT)/jvmti-agent

# preprocessor
PREPROC_TRGT=$(TARGET_DIR)/preprocessor.jar
PREPROC_ROOT=$(ROOT)/visualisation-server
PREPROC_DPND=$(ROOT)/preprocessor
PREPROC_SRCS=$(shell find $(PREPROC_ROOT)/src -type f) $(shell find $(PREPROC_DPND)/src -type f)

# event ticker
SCALA_VERSION=2.12
VIS_ROOT=$(ROOT)/visualisation
VIS_OUT=$(TARGET_DIR)/visualisation
EVENTS_ROOT=$(VIS_ROOT)/event_ticker
EVENTS_TRGT=$(VIS_OUT)/event_ticker.js
EVENTS_SRCS=$(shell find $(EVENTS_ROOT)/src -type f)

# visualisation
VIS_SRCS=$(shell find $(VIS_ROOT)/src -type f)

# run script
RUNSH_SRCS=$(ROOT)/scripts/run.sh
RUNSH_TRGT=$(TARGET_DIR)/run.sh

.PHONY: build
build: $(TARGET_DIR) $(MONITOR_TRGT) $(NATIVE_TRGT) $(BOOTSTRAP_TRGT) $(PREPROC_TRGT) $(EVENTS_TRGT) vis_srcs $(RUNSH_TRGT)
	@echo $(TARGET_DIR)

$(TARGET_DIR):
	@mkdir -p $(TARGET_DIR) $(VIS_OUT)

# TODO check for failures needed?

$(MONITOR_TRGT): $(MONITOR_SRC)
	$(MONITOR_ROOT)/gradlew $(GRADLE_FLAGS) -p $(MONITOR_ROOT) buildJar >$@.stdout 2>$@.stderr
	@cp -f $(MONITOR_ROOT)/build/libs/monitor-agent-$(VERSION).jar $@

$(BOOTSTRAP_TRGT): $(MONITOR_TRGT)
	@cp -f $< $@
	@zip -q -d $@ ms/domwillia/jvmemory/modify/*

$(PREPROC_TRGT): $(PREPROC_SRCS)
	$(PREPROC_ROOT)/gradlew $(GRADLE_FLAGS) -p $(PREPROC_ROOT) buildJar >$@.stdout 2>$@.stderr
	@cp -f $(PREPROC_ROOT)/build/libs/visualisation-server-$(VERSION).jar $@

$(NATIVE_TRGT):
	$(MAKE) RELEASE=1 -C $(NATIVE_ROOT) >$@.stdout 2>$@.stderr
	cp -f $(NATIVE_ROOT)/libagent.so $@

# TODO replace slow sbt
# TODO release config
$(EVENTS_TRGT): $(EVENTS_SRCS)
	(cd $(EVENTS_ROOT) && exec sbt fastOptJS) >$@.stdout 2>$@.stderr
	@cp -f $(EVENTS_ROOT)/target/scala-$(SCALA_VERSION)/event_ticker-fastopt.js $@

.PHONY: vis_srcs
vis_srcs:
	@cp -f $(VIS_SRCS) $(VIS_OUT)

$(RUNSH_TRGT): $(RUNSH_SRCS)
	@sed 's:$$INSTALL_DIR:$(abspath $(TARGET_DIR)):' $< > $@
	@chmod +x $@

.PHONY: clean
clean:
	rm -rf $(BUILD_ROOT)
	@$(MAKE) -C $(NATIVE_ROOT) clean
	@$(MONITOR_ROOT)/gradlew $(GRADLE_FLAGS) -p $(MONITOR_ROOT) clean
	@$(PREPROC_ROOT)/gradlew $(GRADLE_FLAGS) -p $(PREPROC_ROOT) clean
	@(cd $(EVENTS_ROOT) && exec sbt clean)
