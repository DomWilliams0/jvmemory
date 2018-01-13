#ifndef JVMTI_AGENT_ARRAY
#define JVMTI_AGENT_ARRAY

#include <stdlib.h>

#define ARRAY_STARTING_SIZE 16
#define ARRAY_TYPE long

struct array {
	ARRAY_TYPE *data;
	size_t size;
	size_t count;
};

int array_resize(struct array *arr);

int array_init(struct array *arr);

int array_add(struct array *arr, ARRAY_TYPE value);

void array_free(struct array *arr);

void array_clear(struct array *arr);

#endif
