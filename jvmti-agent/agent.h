#ifndef JVMTI_AGENT
#define JVMTI_AGENT

#include <jvmti.h>
#include "logger.h"

extern jvmtiEnv *env;
extern logger_p logger;

extern unsigned int classes_loading;
extern jboolean program_running;


#endif
