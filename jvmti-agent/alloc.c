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

void allocate_array_tag(JNIEnv *jnienv,
                        jobject obj,
                        jint array_size)
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

void allocate_tags_for_multidim_array(JNIEnv *jnienv,
                                      jobject arr,
                                      jint dims)
{
	jsize len = (*jnienv)->GetArrayLength(jnienv, arr);
	// TODO array source
	allocate_array_tag(jnienv, arr, len);

	if (dims <= 1)
		return;

	for (int i = 0; i < len; i++)
	{
		jobject child = (*jnienv)->GetObjectArrayElement(jnienv, arr, i);
		allocate_tags_for_multidim_array(jnienv, child, dims - 1);
	}
}


