#ifndef JVMTI_AGENT
#define JVMTI_AGENT

#include <jvmti.h>
#include "logger.h"
#include "fields.h"

extern jvmtiEnv *env;
extern logger_p logger;
extern fields_map_p fields_map;

extern unsigned int classes_loading;
extern jboolean program_running;


#endif
