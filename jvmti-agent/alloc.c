#include <jvmti.h>
#include "alloc.h"
#include "agent.h"
#include "util.h"

unsigned int classes_loading = 0;
jboolean program_running = 0;
static jlong next_id = 1;

#define SHOULD_LOG_ALLOCATION (program_running == 1 && classes_loading == 0)


void allocate_object_tag(JNIEnv *jnienv,
                         jobject obj)
{
	allocate_array_tag(jnienv, obj, 0);
}

static void allocate_array_with_array_src_tag(JNIEnv *jnienv,
                                              jobject obj,
                                              jint array_size,
                                              jlong src_array_id,
                                              jint src_index)
{
	jlong new_tag = next_id++;

	jvmtiError err;
	if ((err = (*env)->SetTag(env, obj, new_tag)) == JVMTI_ERROR_NONE)
	{
		if (SHOULD_LOG_ALLOCATION)
		{
			char *name = NULL;
			jclass cls = (*jnienv)->GetObjectClass(jnienv, obj);
			if ((err = (*env)->GetClassSignature(env, cls, &name, NULL)) == JVMTI_ERROR_NONE)
			{
				if (array_size == 0)
					on_alloc_object(logger, get_thread_id(jnienv), new_tag, name);
				else if (src_array_id != 0)
					on_alloc_array_in_array(
							logger,
							get_thread_id(jnienv),
							new_tag,
							name,
							array_size,
							src_array_id,
							src_index);
				else
					on_alloc_array(logger, get_thread_id(jnienv), new_tag, name, array_size);

				(*env)->Deallocate(env, (unsigned char *) name);
				name = NULL;
			} else
			{
				fprintf(stderr, "could not get class name: %d\n", err);
			}
		}
	} else
	{
		fprintf(stderr, "could not allocate tag: %d\n", err);
	}

}

void allocate_array_tag(
		JNIEnv *jnienv,
		jobject obj,
		jint array_size)
{
	allocate_array_with_array_src_tag(jnienv, obj, array_size, 0, 0);
}

static void allocate_tags_for_multidim_array_recurse(
		JNIEnv *jnienv,
		jobject arr,
		jint dims,
		jlong src_array,
		jint src_index)
{
	jsize len = (*jnienv)->GetArrayLength(jnienv, arr);
	allocate_array_with_array_src_tag(jnienv, arr, len, src_array, src_index);

	if (dims <= 1)
		return;


	jlong parent_id = get_tag(arr);
	for (int i = 0; i < len; i++)
	{
		jobject child = (*jnienv)->GetObjectArrayElement(jnienv, arr, i);
		allocate_tags_for_multidim_array_recurse(jnienv, child, dims - 1, parent_id, i);
	}

}

void allocate_tags_for_multidim_array(
		JNIEnv *jnienv,
		jobject arr,
		jint dims)
{
	allocate_tags_for_multidim_array_recurse(jnienv, arr, dims, 0, 0);
}

jlong get_tag(jobject obj)
{
	jlong tag = 0L;
	(*env)->GetTag(env, obj, &tag);
	return tag;
}
