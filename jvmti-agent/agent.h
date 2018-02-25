#ifndef JVMTI_AGENT
#define JVMTI_AGENT

#include <jvmti.h>
#include "logger.h"
#include "fields.h"

// global with no concurrency protection needed
extern jvmtiEnv *env;
extern logger_p logger; // mutex is in rust

// TODO mutex/monitor
extern jboolean program_running;

// TODO thread local
extern unsigned int classes_loading;
extern explore_cache_p explore_cache;


#endif
