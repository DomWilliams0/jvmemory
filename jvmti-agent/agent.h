#ifndef JVMTI_AGENT
#define JVMTI_AGENT

#include <jvmti.h>
#include "logger.h"
#include "fields.h"
#include "concurrent.h"

// global with no concurrency protection needed
extern jvmtiEnv *env;

// concurrency is in rust
extern logger_p logger;
extern concurrent_p concurrent;

// TODO mutex/monitor
extern explore_cache_p explore_cache;

#endif
