#include <jni.h>
#include <jvmti.h>
#include <stdlib.h>
#include "agent.h"
#include "exports.h"
#include "alloc.h"
#include "util.h"

#define DEALLOCATE(p) (*env)->Deallocate(env, (unsigned char *)(p))

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    setProgramInProgress
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_setProgramInProgress(
		JNIEnv *jnienv,
		jclass klass,
		jboolean value)
{
	program_running = value;
}

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    onClassLoad
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_onClassLoad(
		JNIEnv *jnienv,
		jclass klass,
		jboolean starting)
{
	classes_loading += starting == JNI_TRUE ? 1 : -1;
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
	allocate_object_tag(jnienv, obj, clazz);
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
	allocate_array_tag(jnienv, array, size, clazz);
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
	allocate_tags_for_multidim_array(jnienv, array, dims, clazz);
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
		allocate_object_tag(jnienv, obj, clazz);
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
 * Signature: (Ljava/lang/String;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_enterMethod(
		JNIEnv *jnienv,
		jclass klass,
		jstring class_name,
		jstring method_name)
{
	const char *cls = (*jnienv)->GetStringUTFChars(jnienv, class_name, NULL);
	const char *mthd = (*jnienv)->GetStringUTFChars(jnienv, method_name, NULL);
	on_enter_method(logger, get_thread_id(jnienv), cls, mthd);
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

static jboolean primed = JNI_FALSE;

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    primeForSystemMethod
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_primeForSystemMethod(
		JNIEnv *jnienv,
		jclass klass)
{
	primed = JNI_TRUE;
}

static jint JNICALL callback_heap_ref(
		jvmtiHeapReferenceKind reference_kind,
		const jvmtiHeapReferenceInfo *reference_info,
		jlong class_tag,
		jlong referrer_class_tag,
		jlong size,
		jlong *tag_ptr,
		jlong *referrer_tag_ptr,
		jint length,
		void *user_data)
{

	if (reference_kind == JVMTI_HEAP_REFERENCE_FIELD)
	{
		printf("field ref %d, tagged from %lu, length %d\n", reference_info->field.index, *referrer_tag_ptr, length);
		return JVMTI_VISIT_OBJECTS;
	}

	return 0;
}

static const jvmtiHeapCallbacks heap_callbacks = {
		.heap_reference_callback = callback_heap_ref
};


static void print_fields(JNIEnv *jnienv,
                         jclass cls,
                         int *acc)
{
	jint field_count;
	jfieldID *fields;
	DO_SAFE((*env)->GetClassFields(env, cls, &field_count, &fields), "get fields");
	for (int i = 0; i < field_count; ++i)
	{
		jfieldID fid = fields[i];
		char *name;
		char *sig;
		DO_SAFE((*env)->GetFieldName(env, cls, fid, &name, &sig, NULL), "get field name");

		printf("field %d called '%s' of type %s \n", (*acc)++, name, sig);
		DEALLOCATE(name);
		DEALLOCATE(sig);
	}
	DEALLOCATE(fields);
}

static void print_all_fields(JNIEnv *jnienv,
                             jclass cls,
                             int *acc)
{
	// TODO use a rust hashset to track visited classes instead of a second cleanup phase
	int cleanup = acc == NULL;

	if (!cleanup)
	{
		long tag;
		DO_SAFE((*env)->GetTag(env, cls, &tag), "get class tag");
		if (tag != 0)
			return;

		DO_SAFE((*env)->SetTag(env, cls, 1), "set class tag");
	}


	jint count;
	jclass *interfaces;
	DO_SAFE((*env)->GetImplementedInterfaces(env, cls, &count, &interfaces), "get interfaces");

	for (int i = 0; i < count; ++i)
	{
		jclass iface = interfaces[i];
		print_all_fields(jnienv, iface, acc);
		(*jnienv)->DeleteLocalRef(jnienv, iface);
	}
	DEALLOCATE(interfaces);

	jclass super = cls;
	while ((super = (*jnienv)->GetSuperclass(jnienv, super)) != NULL)
	{
		print_all_fields(jnienv, super, acc);
	}

	if (!cleanup)
		print_fields(jnienv, cls, acc);
	else
		DO_SAFE((*env)->SetTag(env, cls, 0), "set class tag");
}

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    enterSystemMethod
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_enterSystemMethod(
		JNIEnv *jnienv,
		jclass klass,
		jobject obj)
{
	if (primed == JNI_TRUE)
	{
		primed = JNI_FALSE;

		jclass cls = (*jnienv)->GetObjectClass(jnienv, obj);

		// TODO calculate and cache fields once for each class
		int acc = 0;
		print_all_fields(jnienv, cls, &acc);
		print_all_fields(jnienv, cls, NULL); // horrendous
		puts("######");


		// TODO pass in this array of fields as user_data
		DO_SAFE((*env)->FollowReferences(
				env,
				0,
				NULL,
				obj,
				&heap_callbacks,
				NULL
		), "following refs");
		puts("=======");
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
