ifeq ($(JAVA_HOME),)
	JAVA_HOME = /usr/lib/jvm/java-8-openjdk
endif

JVM_INCS = -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux

CFLAGS = -Wall -Wextra -Wpedantic -Wno-unused-parameter -fPIC -O1 $(JVM_INCS)
CC = gcc
TARGET = libagent.so

RUST_COMPILE_DIR = release
LOGGER_DIR = jvmti_logger/target/$(RUST_COMPILE_DIR)
LOGGER_LIB = $(LOGGER_DIR)/libjvmti_logger.so

RELEASE ?= 0
ifeq ($(RELEASE), 0)
	RUST_COMPILE_DIR = debug
	CFLAGS += -DDEBUG -O0 -g
endif

SRCS := $(wildcard *.c)
OBJS := $(notdir $(SRCS:%.c=%.o))

.PHONY: default
default: $(TARGET)

.PHONY: clean
clean:
	@rm -rf $(TARGET) $(OBJS)
	@(cd $(LOGGER_DIR) && cargo clean -p jvmti_logger)

$(TARGET): $(OBJS) $(LOGGER_LIB)
	$(CC) $(CFLAGS) $(OBJS) -shared -L$(LOGGER_DIR) -ljvmti_logger -o $@

# src -> obj
$(OBJS): %.o : %.c
	$(CC) $(CFLAGS) -c $< -o $@

.PHONY: $(LOGGER_LIB)
$(LOGGER_LIB):
	@(cd $(LOGGER_DIR) && cargo build)
