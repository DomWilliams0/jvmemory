#include <jvmti.h>
#include <string.h>
#include <stdlib.h>
#include "alloc.h"
#include "agent.h"
#include "util.h"
#include "thread_local.h"

jboolean should_log_allocatation()
{
	if (concurrent_is_program_running(concurrent) == JNI_FALSE)
		return JNI_FALSE;

	struct thread_local_state *state = thread_local_state_get();
	return (jboolean) (state->classload_depth == 0 ? JNI_TRUE : JNI_FALSE);
}

const char *get_chars(JNIEnv *jnienv,
                      struct any_string *any)
{
	if (any->is_jstring == JNI_TRUE)
		return (*jnienv)->GetStringUTFChars(jnienv, any->jstring, NULL);
	else
		return any->str;
}

void free_chars(JNIEnv *jnienv,
                struct any_string *any,
                const char *chars)
{
	if (any->is_jstring == JNI_TRUE)
		(*jnienv)->ReleaseStringUTFChars(jnienv, any->jstring, chars);
}

void allocate_object_tag(JNIEnv *jnienv,
                         jobject obj,
                         struct any_string *clazz)
{
	allocate_array_tag(jnienv, obj, 0, clazz);
}

static void allocate_array_with_array_src_tag(JNIEnv *jnienv,
                                              jobject obj,
                                              jint array_size,
                                              struct any_string *clazz,
                                              int dims_to_pop,
                                              jlong src_array_id,
                                              jint src_index)
{
	jlong new_tag = concurrent_get_next_tag(concurrent);

	jvmtiError err;
	if ((err = (*env)->SetTag(env, obj, new_tag)) != JVMTI_ERROR_NONE)
	{
		fprintf(stderr, "could not allocate tag: %d\n", err);
		return;
	}

	if (should_log_allocatation() == JNI_FALSE)
		return;


	const char *clazz_str = get_chars(jnienv, clazz);

	if (clazz_str == NULL)
	{
		fprintf(stderr, "could not get class name\n");
		return;
	}

	const char *clazz_mod = clazz_str;

	char *clazz_malloc = NULL;
	if (dims_to_pop > 0)
	{
		size_t new_len = strlen(clazz_str) - (dims_to_pop * strlen("[]"));
		clazz_malloc = calloc(1, new_len + 1);
		if (clazz_malloc != NULL)
		{
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

	free_chars(jnienv, clazz, clazz_str);

	if (clazz_malloc != NULL)
		free(clazz_malloc);
}

void allocate_array_tag(JNIEnv *jnienv,
                        jobject obj,
                        jint array_size,
                        struct any_string *clazz)
{
	allocate_array_with_array_src_tag(jnienv, obj, array_size, clazz, 0, 0, 0);
}

static void allocate_tags_for_multidim_array_recurse(JNIEnv *jnienv,
                                                     jobject arr,
                                                     jint dims,
                                                     jint current_dim,
                                                     struct any_string *clazz,
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
                                      struct any_string *clazz)
{
	allocate_tags_for_multidim_array_recurse(jnienv, arr, dims, 0, clazz, 0, 0);
}

jlong get_tag(jobject obj)
{
	jlong tag = 0L;
	(*env)->GetTag(env, obj, &tag);
	return tag;
}
