#include <string.h>
#include <stdlib.h>
#include <jni.h>
#include <jvmti.h>

#include "native_array.h"
#include "agent.h"
#include "alloc.h"
#include "util.h"


typedef jobject (*array_proxy_func)(JNIEnv *,
                                    jclass,
                                    jclass,
                                    void *);

enum array_func
{
	NEW_ARRAY = 0,
	MULTI_NEW_ARRAY,
	PROXY_COUNT
};

static array_proxy_func proxy_funcs[2] = {0};

static array_proxy_func get_proxy_func(enum array_func which)
{
	array_proxy_func func = proxy_funcs[which];
	DO_SAFE_COND(func != NULL, "Array proxy function is null");
	return func;
}

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    newArrayWrapper
 * Signature: (Ljava/lang/Class;I)Ljava/lang/Object;
 */
JNIEXPORT jobject JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_newArrayWrapper(
		JNIEnv *jnienv,
		jclass klass,
		jclass type,
		jint len)
{

	array_proxy_func func = get_proxy_func(NEW_ARRAY);
	jobject array = func(jnienv, klass, type, (void *) len); // oof, it hurts

	char *type_name;
	DO_SAFE((*env)->GetClassSignature(env, type, &type_name, NULL), "get class name");

	struct any_string any = {
			.is_jstring = JNI_FALSE,
			.str = type_name
	};
	allocate_array_tag(jnienv, array, len, &any);

	deallocate(type_name);
	return array;
}

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    multiNewArrayWrapper
 * Signature: (Ljava/lang/Class;[Ljava/lang/Integer;)Ljava/lang/Object;
 */
JNIEXPORT jobject JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_multiNewArrayWrapper(
		JNIEnv *jnienv,
		jclass klass,
		jclass type,
		jobjectArray dims)
{
	array_proxy_func func = get_proxy_func(MULTI_NEW_ARRAY);
	jobject array = func(jnienv, klass, type, (void *) dims);
	jint dim_len = (*jnienv)->GetArrayLength(jnienv, array);
	EXCEPTION_CHECK(jnienv);

	char *type_name;
	DO_SAFE((*env)->GetClassSignature(env, type, &type_name, NULL), "get class name");

	struct any_string any = {
			.is_jstring = JNI_FALSE,
			.str = type_name
	};

	allocate_tags_for_multidim_array(jnienv, array, dim_len, &any);

	deallocate(type_name);
	return array;
}

void JNICALL callback_native_bind(
		jvmtiEnv *env,
		JNIEnv *jnienv,
		jthread thread,
		jmethodID method,
		void *address,
		void **new_address_ptr)
{
	{
		int unset = 0;
		for (int i = 0; i < PROXY_COUNT; ++i)
			if (proxy_funcs[i] == NULL)
				unset++;

		if (unset == 0)
			return;
	}

	char *method_name;
	DO_SAFE((*env)->GetMethodName(env, method, &method_name, NULL, NULL), "get method name");

	enum array_func which;

	if (strcmp("newArray", method_name) == 0)
		which = NEW_ARRAY;
	else if (strcmp("multiNewArray", method_name) == 0)
		which = MULTI_NEW_ARRAY;
	else
		which = PROXY_COUNT; // invalid

	if (which != PROXY_COUNT)
	{
		char *class_name;
		jclass cls;
		DO_SAFE((*env)->GetMethodDeclaringClass(env, method, &cls), "get method class");
		DO_SAFE((*env)->GetClassSignature(env, cls, &class_name, NULL), "get class name");

		if (strcmp(class_name, "Ljava/lang/reflect/Array;") == 0)
		{
			proxy_funcs[which] = (array_proxy_func) address; // the JVM has to pass this address as a void *, sorry
		}

		(*jnienv)->DeleteLocalRef(jnienv, cls);
		deallocate(class_name);
	}
	deallocate(method_name);
}

