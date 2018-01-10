#include <string.h>
#include <jni.h>
#include <jvmti.h>

#include "exports.h"
#include "array.h"
#include "util.h"

static JavaVM *jvm = NULL;
static jvmtiEnv *env = NULL;

static jlong next_id = 1;
static jlong last_id = 0;

static struct array freed_objects;

static jvmtiError add_capabilities() {
	jvmtiCapabilities capa = {0};

	capa.can_tag_objects                 = 1;
	capa.can_generate_object_free_events = 1;

	return (*env)->AddCapabilities(env, &capa);
}

static void JNICALL callback_dealloc(jvmtiEnv *jvmti_env, jlong tag);

static jvmtiError register_callbacks() {
	jvmtiEventCallbacks callbacks = {0};
	callbacks.ObjectFree = &callback_dealloc;

	DO_SAFE_RETURN((*env)->SetEventCallbacks(env, &callbacks, sizeof(callbacks)));
	DO_SAFE_RETURN((*env)->SetEventNotificationMode(env, JVMTI_ENABLE, JVMTI_EVENT_OBJECT_FREE, (jthread)NULL));

	return JVMTI_ERROR_NONE;
}

JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *javavm, char *options, void *reserved) {
	jint ret;

	// set globals
	jvm = javavm;
	if ((ret = (*jvm)->GetEnv(jvm, (void **)&env, JVMTI_VERSION_1_2)) != JNI_OK) {
		fprintf(stderr, "failed to create environment: %d\n", ret);
		return ret;
	}

	// add required capabilities
	DO_SAFE(add_capabilities(), "adding required capabilities");

	// register callbacks
	DO_SAFE(register_callbacks(), "registering callbacks");

	// init list
	DO_SAFE_COND(array_init(&freed_objects) == 0, "initialising freed objects array");

	return JNI_OK;
}

JNIEXPORT void JNICALL Agent_OnUnload(JavaVM *vm) {
	array_free(&freed_objects);
}

static void log_allocation(JNIEnv *jnienv, jlong tag, jclass klass) {
	jstring class_name;

	// TODO cache these field and method IDs
	// TODO exception checks

	// get internal name
	{
		jclass cls = (*jnienv)->FindClass(jnienv, "org/objectweb/asm/Type");
		EXCEPTION_CHECK(jnienv);
		jmethodID method = (*jnienv)->GetStaticMethodID(jnienv, cls, "getInternalName", "(Ljava/lang/Class;)Ljava/lang/String;");
		EXCEPTION_CHECK(jnienv);
		jstring result = (*jnienv)->CallStaticObjectMethod(jnienv, cls, method, klass);
		EXCEPTION_CHECK(jnienv);

		class_name = result;
	}

	// call onAlloc
	{
		jclass cls = (*jnienv)->FindClass(jnienv, "ms/domwillia/jvmemory/monitor/Monitor");
		EXCEPTION_CHECK(jnienv);
		jfieldID instanceID = (*jnienv)->GetStaticFieldID(jnienv, cls, "INSTANCE", "Lms/domwillia/jvmemory/monitor/Monitor;");
		EXCEPTION_CHECK(jnienv);
		jobject instance = (*jnienv)->GetStaticObjectField(jnienv, cls, instanceID);
		EXCEPTION_CHECK(jnienv);
		jmethodID method = (*jnienv)->GetMethodID(jnienv, cls, "onAlloc", "(JLjava/lang/String;)V");
		EXCEPTION_CHECK(jnienv);
		(*jnienv)->CallVoidMethod(jnienv, instance, method, tag, class_name);
	}
}

static void JNICALL callback_dealloc(jvmtiEnv *jvmti_env, jlong tag) {
	// no JVMTI or JNI functions can be called in this callback
	// TODO add tag to a list of dealloced tags, which is processed at a safe place
	//		for every queued free, call the java Monitor onDealloc method
	array_add(&freed_objects, tag);
}

// exported functions

JNIEXPORT jlong JNICALL Java_ms_domwillia_jvmemory_monitor_Tagger_allocateTag(
		JNIEnv *jnienv,
		jclass klass,
		jobject obj,
		jclass expectedClass) {

	jclass runtimeClass = (*jnienv)->GetObjectClass(jnienv, obj);
	jlong new_tag;

	if ((*jnienv)->IsSameObject(jnienv, runtimeClass, expectedClass) == JNI_TRUE) {
		new_tag = next_id++;
		last_id = new_tag;

		log_allocation(jnienv, new_tag, expectedClass);

	} else {
		new_tag = last_id;
	}

	jvmtiError err;
	if ((err = (*env)->SetTag(env, obj, new_tag)) == JVMTI_ERROR_NONE) {
		// debug log
		char *name = NULL;
		if ((err = (*env)->GetClassSignature(env, expectedClass, &name, NULL)) == JVMTI_ERROR_NONE) {
			printf("allocated tag %ld to object of class '%s'\n", new_tag, name);
			(*env)->Deallocate(env, (unsigned char *)name);
			name = NULL;
		} else {
			printf("could not get class name: %d\n", err);
		}
	} else {
		printf("could not allocate tag: %d\n", err);
		new_tag = 0;
	}

	return new_tag;
}

JNIEXPORT jlong JNICALL Java_ms_domwillia_jvmemory_monitor_Tagger_getTag(
		JNIEnv *jnienv,
		jclass klass,
		jobject obj) {

	jlong tag = 0L;
	(*env)->GetTag(env, obj, &tag);
	return tag;
}

JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Tagger_flushQueuedDeallocations(
		JNIEnv *jnienv,
		jclass klass) {

	// TODO get from cache
	jclass cls = (*jnienv)->FindClass(jnienv, "ms/domwillia/jvmemory/monitor/Monitor");
	jfieldID instanceID = (*jnienv)->GetStaticFieldID(jnienv, cls, "INSTANCE", "Lms/domwillia/jvmemory/monitor/Monitor;");
	jobject instance = (*jnienv)->GetStaticObjectField(jnienv, cls, instanceID);
	jmethodID method = (*jnienv)->GetMethodID(jnienv, cls, "onDealloc", "(J)V");

	for (size_t i = 0; i < freed_objects.count; i++) {
		(*jnienv)->CallVoidMethod(jnienv, instance, method, freed_objects.data[i]);
	}

	array_clear(&freed_objects);

}
