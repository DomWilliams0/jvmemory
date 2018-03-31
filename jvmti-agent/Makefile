ifeq ($(JAVA_HOME),)
	JAVA_HOME = /usr/lib/jvm/java-8-openjdk
endif

JVM_INCS = -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux

CFLAGS = -Wall -Wextra -Wpedantic -Wno-unused-parameter -fPIC -O1 $(JVM_INCS)
LDFLAGS = -lpthread -ldl
CC = gcc
TARGET = libagent.so

CARGO_FLAG = --release
RUST_COMPILE_DIR = release
LOGGER_BASE_DIR = jvmti_logger
RUST_SRCS = $(shell find $(LOGGER_BASE_DIR)/src -type f -name "*.rs")
LOGGER_DIR = $(LOGGER_BASE_DIR)/target/$(RUST_COMPILE_DIR)
LOGGER_LIB = $(LOGGER_DIR)/libjvmti_logger.a

RELEASE ?= 0
ifeq ($(RELEASE), 0)
	RUST_COMPILE_DIR = debug
	CFLAGS += -DDEBUG -O0 -g
	CARGO_FLAG =
endif

SRCS := $(wildcard *.c)
OBJS := $(notdir $(SRCS:%.c=%.o))

.PHONY: all
all: $(TARGET)

.PHONY: clean
clean:
	@rm -rf $(TARGET) $(OBJS)
	@(cd $(LOGGER_BASE_DIR) && cargo clean -p jvmti_logger)

$(TARGET): $(OBJS) $(LOGGER_LIB)
	$(CC) $(CFLAGS) $^ -shared -o $@

# src -> obj
$(OBJS): %.o : %.c
	$(CC) $(CFLAGS) -c $< -o $@

$(LOGGER_LIB): $(RUST_SRCS)
	(cd $(LOGGER_BASE_DIR) && cargo build $(CARGO_FLAG))
