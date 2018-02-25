#ifndef JVMTI_AGENT
#define JVMTI_AGENT

#include <jvmti.h>
#include "logger.h"
#include "fields.h"

// global with no concurrency protection needed
extern jvmtiEnv *env;
extern logger_p logger; // mutex is in rust

// TODO mutex/monitor
// TODO next_id too
extern jboolean program_running; // TODO one writer, multiple readers?
extern explore_cache_p explore_cache;

#endif
