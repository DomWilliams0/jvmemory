#ifndef JVMTI_AGENT_CONCURRENT
#define JVMTI_AGENT_CONCURRENT

#include <jni.h>

struct concurrent;

typedef struct concurrent *concurrent_p;

extern concurrent_p concurrent_init();

extern void concurrent_free(concurrent_p c);

extern jboolean concurrent_is_program_running(concurrent_p c);

extern void concurrent_set_program_running(concurrent_p c,
                                           jboolean running);

extern jlong concurrent_get_next_tag(concurrent_p c);

#endif
