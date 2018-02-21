#include <string.h>
#include <stdlib.h>
#include <jni.h>
#include <jvmti.h>

#include "native_array.h"
#include "agent.h"
#include "alloc.h"
#include "util.h"


typedef jobject (*new_array_proxy_func)(JNIEnv *,
                                        jclass,
                                        jclass,
                                        jint);

static new_array_proxy_func orig_new_array;

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
	DO_SAFE_COND(orig_new_array != NULL, "Array.newArray proxy is null");

	jobject array = orig_new_array(jnienv, klass, type, len);

	char *type_name;
	DO_SAFE((*env)->GetClassSignature(env, type, &type_name, NULL), "get class name");

	struct any_string any = {
			.is_jstring = JNI_FALSE,
			.str = type_name
	};
	allocate_array_tag(jnienv, array, len, &any);

	DEALLOCATE(type_name);
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
	if (orig_new_array != NULL)
		return;

	char *method_name;
	DO_SAFE((*env)->GetMethodName(env, method, &method_name, NULL, NULL), "get method name");

	// TODO multi array too
	if (strcmp("newArray", method_name) == 0)
	{
		char *class_name;
		jclass cls;
		DO_SAFE((*env)->GetMethodDeclaringClass(env, method, &cls), "get method class");
		DO_SAFE((*env)->GetClassSignature(env, cls, &class_name, NULL), "get class name");

		if (strcmp(class_name, "Ljava/lang/reflect/Array;") == 0)
		{
			orig_new_array = (new_array_proxy_func) address; // the JVM has to pass this address as a void *, sorry
		}

		(*jnienv)->DeleteLocalRef(jnienv, cls);
		DEALLOCATE(class_name);
	}
	DEALLOCATE(method_name);
}

