ifeq ($(JAVA_HOME),)
	JAVA_HOME = /usr/lib/jvm/java-8-openjdk
endif

JVM_INCS = -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux

CFLAGS = -Wall -Wextra -Wpedantic -Wno-unused-parameter -fPIC -O1 $(JVM_INCS)
CC = gcc
TARGET = libagent.so

RELEASE ?= 0
ifeq ($(RELEASE), 0)
	CFLAGS += -DDEBUG -O0 -g
endif

SRCS := $(shell find . -type f -name '*.c')
OBJS := $(notdir $(SRCS:%.c=%.o))

.PHONY: default
default: $(TARGET)

.PHONY: clean
clean:
	@rm -rf $(TARGET) $(OBJS)

$(TARGET): $(OBJS)
	$(CC) $(CFLAGS) $(OBJS) -shared -o $@

# src -> obj
$(OBJS): %.o : %.c
	$(CC) $(CFLAGS) -c $< -o $@
