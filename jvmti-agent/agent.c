#include <string.h>
#include <jni.h>
#include <jvmti.h>

#include "exports.h"

static JavaVM *jvm = NULL;
static jvmtiEnv *env = NULL;

static jlong last_id = 1;

static jvmtiError add_capabilities() {
	jvmtiCapabilities capa = {0};

	capa.can_tag_objects                 = 1;
	capa.can_generate_object_free_events = 1;

	return (*env)->AddCapabilities(env, &capa);
}

static void JNICALL callback_dealloc(jvmtiEnv *jvmti_env, jlong tag) {
	// no JVMTI or JNI functions can be called in this callback
	// TODO add tag to a list of dealloced tags, which is processed at a safe place
	//		for every queued free, call the java Monitor onDealloc method
	printf("deallocating object %ld\n", tag);
}

// one liners that return jvmtiError only
#define DO_SAFE(code) do {\
	jvmtiError err = code;\
	if (err != JVMTI_ERROR_NONE)\
	return err;\
} while (0)

static jvmtiError register_callbacks() {
	jvmtiEventCallbacks callbacks = {0};
	callbacks.ObjectFree = &callback_dealloc;

	DO_SAFE((*env)->SetEventCallbacks(env, &callbacks, sizeof(callbacks)));
	DO_SAFE((*env)->SetEventNotificationMode(env, JVMTI_ENABLE, JVMTI_EVENT_OBJECT_FREE, (jthread)NULL));

	return JVMTI_ERROR_NONE;
}

JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *javavm, char *options, void *reserved) {
	jint ret;

	// set globals
	jvm = javavm;
	if ((ret = (*jvm)->GetEnv(jvm, (void **)&env, JVMTI_VERSION_1_2)) != JNI_OK) {
		printf("failed to create environment: %d\n", ret);
		return ret;
	}

	// TODO check_jvmti_error and get error message

	// add required capabilities
	if ((ret = add_capabilities()) != JVMTI_ERROR_NONE) {
		printf("failed to add required capabilities: %d\n", ret);
		return JNI_ABORT;
	}

	// register callbacks
	if ((ret = register_callbacks()) != JVMTI_ERROR_NONE) {
		printf("failed to register event callbacks: %d\n", ret);
		return JNI_ABORT;
	}

	return JNI_OK;
}

// exported functions

JNIEXPORT jlong JNICALL Java_ms_domwillia_jvmemory_monitor_Tagger_allocateTag(
		JNIEnv *jnienv,
		jclass klass,
		jobject obj) {

	jlong new_tag = last_id;
	jvmtiError err;
	if ((err = (*env)->SetTag(env, obj, new_tag)) == JVMTI_ERROR_NONE) {
		last_id++;

		char *name = NULL;
		jclass obj_klass = (*jnienv)->GetObjectClass(jnienv, obj);
		if ((err = (*env)->GetClassSignature(env, obj_klass, &name, NULL)) == JVMTI_ERROR_NONE) {
			printf("allocated tag %ld to object of class '%s'\n", new_tag, name);
			(*env)->Deallocate(env, name);
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

JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Tagger_assignCurrentTag(
		JNIEnv *jnienv,
		jclass klass,
		jobject obj) {

	// TODO
}

JNIEXPORT jlong JNICALL Java_ms_domwillia_jvmemory_monitor_Tagger_getTag(
		JNIEnv *jnienv,
		jclass klass,
		jobject obj) {

	// TODO
	return 0L;
}
