#include <jni.h>
#include <jvmti.h>
#include <stdlib.h>
#include "agent.h"
#include "exports.h"
#include "alloc.h"
#include "util.h"
#include "thread_local.h"

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    setProgramRunning
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_setProgramRunning(
		JNIEnv *jnienv,
		jclass klass,
		jboolean value)
{
	concurrent_set_program_running(concurrent, value);
}

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    onClassLoad
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_enterIgnoreRegion(
		JNIEnv *jnienv,
		jclass klass,
		jboolean entering)
{
	struct thread_local_state *state = thread_local_state_get();
	state->ignore_depth += entering == JNI_TRUE ? 1 : -1;
}

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    allocateTag
 * Signature: (Ljava/lang/String;Ljava/lang/Object;)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_allocateTag(
		JNIEnv *jnienv,
		jclass klass,
		jstring clazz,
		jobject obj)
{
	struct any_string s = ALLOC_STRING_JSTRING(clazz);
	allocate_object_tag(jnienv, obj, &s);
}

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    allocateTagForArray
 * Signature: (ILjava/lang/Object;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_allocateTagForArray(
		JNIEnv *jnienv,
		jclass klass,
		jint size,
		jobject array,
		jstring clazz)
{
	struct any_string s = ALLOC_STRING_JSTRING(clazz);
	allocate_array_tag(jnienv, array, size, &s);
}

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    allocateTagForMultiDimArray
 * Signature: (Ljava/lang/Object;ILjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_allocateTagForMultiDimArray(
		JNIEnv *jnienv,
		jclass klass,
		jobject array,
		jint dims,
		jstring clazz)
{
	struct any_string s = ALLOC_STRING_JSTRING(clazz);
	allocate_tags_for_multidim_array(jnienv, array, dims, &s);
}

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    allocateTagForConstant
 * Signature: (Ljava/lang/Object;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_allocateTagForConstant(
		JNIEnv *jnienv,
		jclass klass,
		jobject obj,
		jstring clazz)
{
	if (get_tag(obj) == 0)
	{
		struct any_string s = ALLOC_STRING_JSTRING(clazz);
		allocate_object_tag(jnienv, obj, &s);

		Java_ms_domwillia_jvmemory_monitor_Monitor_toStringObject(jnienv, klass, obj);
	}
}

/*
 * class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    getTag
 * Signature: (Ljava/lang/Object;)J
 */
JNIEXPORT jlong JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_getTag(
		JNIEnv *jnienv,
		jclass klass,
		jobject obj)
{
	return get_tag(obj);
}

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    enterMethod
 * Signature: (Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_enterMethod(
		JNIEnv *jnienv,
		jclass klass,
		jobject obj,
		jstring class_name,
		jstring method_name)
{
	const char *cls = (*jnienv)->GetStringUTFChars(jnienv, class_name, NULL);
	const char *mthd = (*jnienv)->GetStringUTFChars(jnienv, method_name, NULL);
	on_enter_method(logger, get_thread_id(jnienv), get_tag(obj), cls, mthd);
	(*jnienv)->ReleaseStringUTFChars(jnienv, class_name, cls);
	(*jnienv)->ReleaseStringUTFChars(jnienv, method_name, mthd);
}

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    exitMethod
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_exitMethod(
		JNIEnv *jnienv,
		jclass klass)
{
	on_exit_method(logger, get_thread_id(jnienv));
}

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    exitSystemMethod
 * Signature: (Ljava/lang/Object;)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_exitSystemMethod(
		JNIEnv *jnienv,
		jclass klass,
		jobject obj)
{
	thread_local_state_get()->tracked_system_obj = get_tag(obj);
}

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    processSystemMethodChanges
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_processSystemMethodChanges(
		JNIEnv *jnienv,
		jclass klass)
{
	struct thread_local_state *state = thread_local_state_get();
	if (state->tracked_system_obj != 0L)
	{
		jlong tag = state->tracked_system_obj;
		state->tracked_system_obj = 0L;

		emit_heap_differences(explore_cache, jnienv, tag);
	}
}

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    onGetField
 * Signature: (Ljava/lang/Object;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_onGetField(
		JNIEnv *jnienv,
		jclass klass,
		jobject obj,
		jstring field)
{
	jlong obj_id = get_tag(obj);
	const char *field_str = (*jnienv)->GetStringUTFChars(jnienv, field, NULL);
	on_get_field(logger, get_thread_id(jnienv), obj_id, field_str);
	(*jnienv)->ReleaseStringUTFChars(jnienv, field, field_str);
}

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    onPutFieldObject
 * Signature: (Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_onPutFieldObject(
		JNIEnv *jnienv,
		jclass klass,
		jobject obj,
		jobject value,
		jstring field)
{
	jlong obj_id = get_tag(obj);
	jlong value_id = get_tag(value);

	const char *field_str = (*jnienv)->GetStringUTFChars(jnienv, field, NULL);
	on_put_field_object(logger, get_thread_id(jnienv), obj_id, field_str, value_id);
	(*jnienv)->ReleaseStringUTFChars(jnienv, field, field_str);
}

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    onPutFieldPrimitive
 * Signature: (Ljava/lang/Object;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_onPutFieldPrimitive(
		JNIEnv *jnienv,
		jclass klass,
		jobject obj,
		jstring field)
{
	jlong obj_id = get_tag(obj);
	const char *field_str = (*jnienv)->GetStringUTFChars(jnienv, field, NULL);
	on_put_field_primitive(logger, get_thread_id(jnienv), obj_id, field_str);
	(*jnienv)->ReleaseStringUTFChars(jnienv, field, field_str);
}

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    onGetStatic
 * Signature: (Ljava/lang/String;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_onGetStatic(
		JNIEnv *jnienv,
		jclass klass,
		jstring class,
		jstring field)
{
	const char *class_str = (*jnienv)->GetStringUTFChars(jnienv, class, NULL);
	const char *field_str = (*jnienv)->GetStringUTFChars(jnienv, field, NULL);
	on_get_static(logger, get_thread_id(jnienv), class_str, field_str);
	(*jnienv)->ReleaseStringUTFChars(jnienv, field, field_str);
	(*jnienv)->ReleaseStringUTFChars(jnienv, class, class_str);
}

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    onPutStaticObject
 * Signature: (Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_onPutStaticObject(
		JNIEnv *jnienv,
		jclass klass,
		jobject value,
		jstring class,
		jstring field)
{
	const char *class_str = (*jnienv)->GetStringUTFChars(jnienv, class, NULL);
	const char *field_str = (*jnienv)->GetStringUTFChars(jnienv, field, NULL);
	on_put_static_object(logger, get_thread_id(jnienv), class_str, field_str, get_tag(value));
	(*jnienv)->ReleaseStringUTFChars(jnienv, field, field_str);
	(*jnienv)->ReleaseStringUTFChars(jnienv, class, class_str);
}

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    onStoreLocalVarObject
 * Signature: (Ljava/lang/Object;I)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_onStoreLocalVarObject(
		JNIEnv *jnienv,
		jclass klass,
		jobject value,
		jint index)
{
	jlong value_id = get_tag(value);
	on_store_object(logger, get_thread_id(jnienv), value_id, index);
}

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    onStoreLocalVarPrimitive
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_onStoreLocalVarPrimitive(
		JNIEnv *jnienv,
		jclass klass,
		jint index)
{
	on_store_primitive(logger, get_thread_id(jnienv), index);
}

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    onStoreObjectInArray
 * Signature: (Ljava/lang/Object;Ljava/lang/Object;I)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_onStoreObjectInArray(
		JNIEnv *jnienv,
		jclass klass,
		jobject value,
		jobject array,
		jint index)
{
	jlong value_id = get_tag(value);
	jlong array_id = get_tag(array);
	on_store_object_in_array(logger, get_thread_id(jnienv), value_id, array_id, index);
}

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    onStorePrimitiveInArray
 * Signature: (Ljava/lang/Object;I)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_onStorePrimitiveInArray(
		JNIEnv *jnienv,
		jclass klass,
		jobject array,
		jint index)
{
	jlong array_id = get_tag(array);
	on_store_primitive_in_array(logger, get_thread_id(jnienv), array_id, index);
}

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    onLoadFromArray
 * Signature: (Ljava/lang/Object;I)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_onLoadFromArray(
		JNIEnv *jnienv,
		jclass klass,
		jobject array,
		jint index)
{
	jlong array_id = get_tag(array);
	on_load_from_array(logger, get_thread_id(jnienv), array_id, index);
}

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    onLoadLocalVar
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_onLoadLocalVar(
		JNIEnv *jnienv,
		jclass klass,
		jint index)
{
	on_load(logger, get_thread_id(jnienv), index);
}

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    onDefineClass
 * Signature: ([B)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_onDefineClass(
		JNIEnv *jnienv,
		jclass klass,
		jbyteArray def)
{

	// TODO avoid possible copy
	jbyte *array = (*jnienv)->GetByteArrayElements(jnienv, def, NULL);
	jint len = (*jnienv)->GetArrayLength(jnienv, def);

	on_define_class(logger, get_thread_id(jnienv), (const char *) array, len);

	(*jnienv)->ReleaseByteArrayElements(jnienv, def, array, JNI_ABORT);
}

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    toStringObject
 * Signature: (Ljava/lang/Object;)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_toStringObject(
		JNIEnv *jnienv,
		 jclass klass,
		 jobject obj)
{
	if (concurrent_is_program_running(concurrent) == JNI_FALSE)
		return;

	jlong tag = get_tag(obj);
	if (tag == 0L)
		return;

	struct thread_local_state *state = thread_local_state_get();
	if (state->ignore_depth > 0)
		return;

	static jmethodID to_string_method = NULL;

	if (to_string_method == NULL)
	{
		to_string_method = (*jnienv)->GetMethodID(jnienv, klass, "toString", "()Ljava/lang/String;");
		EXCEPTION_CHECK(jnienv);
	}

	state->ignore_depth += 1;
	jstring to_string = (*jnienv)->CallObjectMethod(jnienv, obj, to_string_method);
	state->ignore_depth -= 1;

	jthrowable ex = (*jnienv)->ExceptionOccurred(jnienv);
	if (ex != NULL)
	{
		// TODO i sicken myself
		(*jnienv)->ExceptionClear(jnienv);
		fprintf(stderr, "exception swallowed while evaluating toString on object %lu\n", tag);
		return;
	}

	if (to_string == NULL)
		return;

	const char *str = (*jnienv)->GetStringUTFChars(jnienv, to_string, NULL);
	to_string_object(logger, get_thread_id(jnienv), tag, str);
	(*jnienv)->ReleaseStringUTFChars(jnienv, to_string, str);
}
