#ifndef JVMTI_AGENT_ID_ARRAY
#define JVMTI_AGENT_ID_ARRAY

#include <stdlib.h>

#define ID_ARRAY_STARTING_SIZE 65535
#define ID_ARRAY_ELEMENT_TYPE long

struct id_array
{
	ID_ARRAY_ELEMENT_TYPE *data;
	size_t size;
	size_t count;
};

int id_array_resize(struct id_array *arr);

int id_array_init(struct id_array *arr);

int id_array_add(struct id_array *arr,
                ID_ARRAY_ELEMENT_TYPE value);

void id_array_free(struct id_array *arr);

void id_array_clear(struct id_array *arr);

#endif
