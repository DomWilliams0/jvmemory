#ifndef JVMTI_AGENT_ARRAY
#define JVMTI_AGENT_ARRAY

#include <stdlib.h>
#include <string.h>

#define ARRAY_STARTING_SIZE 16
#define ARRAY_TYPE long

struct array {
	ARRAY_TYPE *data;
	size_t size;
	size_t count;
};

int array_resize(struct array *arr) {
	void *tmp;
	if ((tmp = realloc(arr->data, arr->size * sizeof(ARRAY_TYPE))) == NULL)
		return -1;

	arr->data = (ARRAY_TYPE *)tmp;
	return 0;
}

int array_init(struct array *arr) {
	arr->size = ARRAY_STARTING_SIZE;
	arr->count = 0;
	arr->data = NULL;
	return array_resize(arr);
}

int array_add(struct array *arr, ARRAY_TYPE value) {
	int ret;
	if (arr->count + 1 == arr->size) {
		size_t old_size = arr->size;
		arr->size *= 2;
		if ((ret = array_resize(arr)) != 0) {
			arr->size = old_size;
			return ret;
		}
	}

	arr->data[arr->count] = value;
	arr->count++;
	return 0;
}

void array_free(struct array *arr) {
	if (arr->data != NULL)
		free(arr->data);
	memset(arr, 0, sizeof(struct array));
}

void array_clear(struct array *arr) {
	arr->count = 0;
}


#endif
