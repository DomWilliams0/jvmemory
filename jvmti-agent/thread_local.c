#include <stdlib.h>

#include "thread_local.h"
#include "util.h"

void thread_local_state_init(jlong tid)
{
	struct thread_local_state *state = calloc(1, sizeof(struct thread_local_state));
	DO_SAFE_COND(state != NULL, "allocating thread local state");

	state->tid = tid;
	DO_SAFE((*env)->SetThreadLocalStorage(env, NULL, state), "set thread local storage");

}

void thread_local_state_free()
{
	struct thread_local_state *state = thread_local_state_get();
	free(state);
	DO_SAFE((*env)->SetThreadLocalStorage(env, NULL, NULL), "set thread local storage");
}

struct thread_local_state *thread_local_state_get()
{
	void* state = NULL;
	DO_SAFE((*env)->GetThreadLocalStorage(env, NULL, &state), "get thread local storage");
	return state;
}
