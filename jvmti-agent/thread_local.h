#ifndef JVMTI_AGENT_THREAD_LOCAL
#define JVMTI_AGENT_THREAD_LOCAL

#include <jvmti.h>
#include "fields.h"

// all functions act on the current calling thread

struct thread_local_state {
	explore_cache_p explore_cache;
	unsigned int classload_depth;
	jlong tracked_system_obj;
};

void thread_local_state_init();

// TODO call on thread death
void thread_local_state_free();

// will exit on failure (DO_SAFE) so never returns null
// error "handling"!
struct thread_local_state *thread_local_state_get();

#endif