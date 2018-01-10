ifeq ($(JAVA_HOME),)
	JAVA_HOME = /usr/lib/jvm/java-8-openjdk
endif

JVM_INCS = -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux

CFLAGS = -Wall -Wextra -Wpedantic -fPIC -O1 $(JVM_INCS)
CC = gcc
TARGET = libagent.so
JAVA = JavaTest

RELEASE ?= 0
ifeq ($(RELEASE), 0)
	CFLAGS += -DDEBUG -O0 -g
endif

SRCS := $(shell find . -type f -name '*.c')
OBJS := $(notdir $(SRCS:%.c=%.o))

.PHONY: default
default: java

.PHONY: clean
clean:
	@rm -rf $(TARGET) $(OBJS) $(JAVA).class

$(TARGET): $(OBJS)
	$(CC) $(CFLAGS) $(OBJS) -shared -o $@

$(JAVA).class: $(JAVA).java
	javac $<

java: $(TARGET) $(JAVA).class
	java -agentpath:./libagent.so $(JAVA) -Xms2m -Xmx2m

# src -> obj
$(OBJS): %.o : %.c
	$(CC) $(CFLAGS) -c $< -o $@
