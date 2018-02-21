#include <string.h>
#include "id_array.h"

int id_array_resize(struct id_array *arr)
{
	void *tmp;
	if ((tmp = realloc(arr->data, arr->size * sizeof(ID_ARRAY_ELEMENT_TYPE))) == NULL)
		return -1;

	arr->data = (ID_ARRAY_ELEMENT_TYPE *) tmp;
	return 0;
}

int id_array_init(struct id_array *arr)
{
	arr->size = ID_ARRAY_STARTING_SIZE;
	arr->count = 0;
	arr->data = NULL;
	return id_array_resize(arr);
}

int id_array_add(struct id_array *arr,
                ID_ARRAY_ELEMENT_TYPE value)
{
	int ret;
	if (arr->count + 1 == arr->size)
	{
		size_t old_size = arr->size;
		arr->size *= 2;
		if ((ret = id_array_resize(arr)) != 0)
		{
			arr->size = old_size;
			return ret;
		}
	}

	arr->data[arr->count] = value;
	arr->count++;
	return 0;
}

void id_array_free(struct id_array *arr)
{
	if (arr->data != NULL)
		free(arr->data);
	memset(arr, 0, sizeof(struct id_array));
}

void id_array_clear(struct id_array *arr)
{
	arr->count = 0;
}
