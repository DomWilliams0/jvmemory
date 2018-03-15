#include <jvmti.h>
#include <string.h>
#include <stdlib.h>
#include "alloc.h"
#include "agent.h"
#include "util.h"
#include "thread_local.h"

jboolean should_log_allocation()
{
	if (concurrent_is_program_running(concurrent) == JNI_FALSE)
		return JNI_FALSE;

	struct thread_local_state *state = thread_local_state_get();
	if (state == NULL)
		return JNI_FALSE;

	return (jboolean) (state->ignore_depth == 0 ? JNI_TRUE : JNI_FALSE);
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

	if (should_log_allocation() == JNI_FALSE)
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

#ifdef ALLOC_STACK_TRACE
#define STACK_TRACE_LENGTH 60
	jvmtiFrameInfo frames[STACK_TRACE_LENGTH];
	jint count;
	jvmtiEnv *jvmti = env;

	err = (*jvmti)->GetStackTrace(
			jvmti, 0, 0, STACK_TRACE_LENGTH,
			frames, &count);
	if (err == JVMTI_ERROR_NONE && count >= 1)
	{
		printf("allocated %lu of type %s: \n", new_tag, clazz_mod);
		for (int i = 0; i < count; ++i)
		{
			char *methodName;
			err = (*jvmti)->GetMethodName(
					jvmti, frames[i].method,
					&methodName, NULL, NULL);
			if (err != JVMTI_ERROR_NONE)
				continue;

			jclass declaring_clazz = NULL;
			err = (*jvmti)->GetMethodDeclaringClass(jvmti, frames[i].method, &declaring_clazz);
			if (err == JVMTI_ERROR_NONE)
			{
				char *declaring_name = NULL;
				err = (*jvmti)->GetClassSignature(jvmti, declaring_clazz, &declaring_name, NULL);
				if (err == JVMTI_ERROR_NONE)
				{
					printf("%d: %s:%s", i, declaring_name, methodName);
					if (frames[i].location == -1)
						printf("\n");
					else
						printf(":%lu\n", frames[i].location);

					deallocate(declaring_name);
				}

				(*jnienv)->DeleteLocalRef(jnienv, declaring_clazz);
			}
		}

		puts("-----");
	}
#endif


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
	if (obj != NULL)
		DO_SAFE((*env)->GetTag(env, obj, &tag), "get tag");
	return tag;
}
