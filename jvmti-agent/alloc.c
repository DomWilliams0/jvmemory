#include <jvmti.h>
#include <string.h>
#include <stdlib.h>
#include "alloc.h"
#include "agent.h"
#include "util.h"

unsigned int classes_loading = 0;
jboolean program_running = 0;
static jlong next_id = 1;

#define SHOULD_LOG_ALLOCATION (program_running == 1 && classes_loading == 0)


void allocate_object_tag(JNIEnv *jnienv,
                         jobject obj,
                         jstring clazz)
{
	allocate_array_tag(jnienv, obj, 0, clazz);
}

static void allocate_array_with_array_src_tag(JNIEnv *jnienv,
                                              jobject obj,
                                              jint array_size,
                                              jstring clazz,
                                              int dims_to_pop,
                                              jlong src_array_id,
                                              jint src_index)
{
	jlong new_tag = next_id++;

	jvmtiError err;
	if ((err = (*env)->SetTag(env, obj, new_tag)) != JVMTI_ERROR_NONE)
	{
		fprintf(stderr, "could not allocate tag: %d\n", err);
		return;
	}

	if (!SHOULD_LOG_ALLOCATION)
		return;


	const char *clazz_str = (*jnienv)->GetStringUTFChars(jnienv, clazz, NULL);

	if (clazz_str == NULL)
	{
		fprintf(stderr, "could not get class name\n");
		return;
	}

	const char *clazz_mod = clazz_str;

	char *clazz_malloc = NULL;
	if (dims_to_pop > 0) {
		size_t new_len = strlen(clazz_str) - (dims_to_pop * strlen("[]"));
		clazz_malloc = calloc(1, new_len + 1);
		if (clazz_malloc != NULL) {
			strncpy(clazz_malloc, clazz_str, new_len);
			clazz_mod = clazz_malloc;
		}
	}


	if (array_size == 0)
		on_alloc_object(logger, get_thread_id(jnienv), new_tag, clazz_mod);
	else if (src_array_id != 0)
		on_alloc_array_in_array(
				logger,
				get_thread_id(jnienv),
				new_tag,
				clazz_mod,
				array_size,
				src_array_id,
				src_index);
	else
		on_alloc_array(logger, get_thread_id(jnienv), new_tag, clazz_mod, array_size);

	(*jnienv)->ReleaseStringUTFChars(jnienv, clazz, clazz_str);

	if (clazz_malloc != NULL)
		free(clazz_malloc);
}

void allocate_array_tag(JNIEnv *jnienv,
                        jobject obj,
                        jint array_size,
                        jstring clazz)
{
	allocate_array_with_array_src_tag(jnienv, obj, array_size, clazz, 0, 0, 0);
}

static void allocate_tags_for_multidim_array_recurse(JNIEnv *jnienv,
                                                    jobject arr,
                                                    jint dims,
                                                    jint current_dim,
                                                    jstring clazz,
                                                    jlong src_array,
                                                    jint src_index)
{
	jsize len = (*jnienv)->GetArrayLength(jnienv, arr);
	allocate_array_with_array_src_tag(jnienv, arr, len, clazz, current_dim, src_array, src_index);

	if (dims <= 1)
		return;


	jlong parent_id = get_tag(arr);
	for (int i = 0; i < len; i++)
	{
		jobject child = (*jnienv)->GetObjectArrayElement(jnienv, arr, i);
		allocate_tags_for_multidim_array_recurse(jnienv, child, dims - 1, current_dim + 1, clazz, parent_id, i);
	}

}

void allocate_tags_for_multidim_array(JNIEnv *jnienv,
                                      jobject arr,
                                      jint dims,
                                      jstring clazz)
{
	allocate_tags_for_multidim_array_recurse(jnienv, arr, dims, 0, clazz, 0, 0);
}

jlong get_tag(jobject obj)
{
	jlong tag = 0L;
	(*env)->GetTag(env, obj, &tag);
	return tag;
}
