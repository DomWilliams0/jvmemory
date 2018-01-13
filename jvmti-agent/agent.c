#include <string.h>
#include <jni.h>
#include <jvmti.h>

#include "array.h"
#include "util.h"
#include "logger.h"

// TODO are any of these globals thread safe?
//		i dont think so
//		does it matter?
static JavaVM *jvm = NULL;
jvmtiEnv *env = NULL;
logger_p logger = NULL;

static struct array freed_objects;
static jrawMonitorID free_lock;

static jvmtiError add_capabilities() {
	jvmtiCapabilities capa = {0};

	capa.can_tag_objects                        = 1;
	capa.can_generate_object_free_events        = 1;
	capa.can_generate_garbage_collection_events = 1;

	return (*env)->AddCapabilities(env, &capa);
}

static void JNICALL callback_dealloc(jvmtiEnv *env, jlong tag);
static void JNICALL callback_vm_init(jvmtiEnv *env, JNIEnv *jnienv, jthread thread);
static void JNICALL callback_gc_finish(jvmtiEnv *env);

static jvmtiError register_callbacks() {
	jvmtiEventCallbacks callbacks = {0};
	callbacks.ObjectFree = &callback_dealloc;
	callbacks.VMInit = &callback_vm_init;
	callbacks.GarbageCollectionFinish = &callback_gc_finish;

	DO_SAFE_RETURN((*env)->SetEventCallbacks(env, &callbacks, sizeof(callbacks)));
	DO_SAFE_RETURN((*env)->SetEventNotificationMode(env, JVMTI_ENABLE, JVMTI_EVENT_OBJECT_FREE, (jthread)NULL));
	DO_SAFE_RETURN((*env)->SetEventNotificationMode(env, JVMTI_ENABLE, JVMTI_EVENT_VM_INIT, (jthread)NULL));
	DO_SAFE_RETURN((*env)->SetEventNotificationMode(env, JVMTI_ENABLE, JVMTI_EVENT_GARBAGE_COLLECTION_FINISH, (jthread)NULL));

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

	// create free thread lock
	DO_SAFE((*env)->CreateRawMonitor(env, "free_lock", &free_lock), "creating raw monitor");

	// init logger
	DO_SAFE_COND((logger = logger_init("jvmemory.log")) != NULL, "logger initialisation");

	return JNI_OK;
}

JNIEXPORT void JNICALL Agent_OnUnload(JavaVM *vm) {
	array_free(&freed_objects);
    logger_free(logger);
    logger = NULL;
}

static void flush_queued_frees(JNIEnv *jnienv) {

	for (size_t i = 0; i < freed_objects.count; i++) {
        on_dealloc(logger, freed_objects.data[i]);
	}

	array_clear(&freed_objects);

}

static void JNICALL free_thread_runnable(jvmtiEnv *env, JNIEnv *jnienv, void *arg) {
	while (1) {
		DO_SAFE((*env)->RawMonitorEnter(env, free_lock), "entering free monitor");

		jvmtiError err = (*env)->RawMonitorWait(env, free_lock, 0);

		if (err == JVMTI_ERROR_NONE)
			flush_queued_frees(jnienv);

		DO_SAFE((*env)->RawMonitorExit(env, free_lock), "exiting free monitor");

		if (err != JVMTI_ERROR_NONE)
			return;
	}
}

// callbacks

static void JNICALL callback_dealloc(jvmtiEnv *env, jlong tag) {
	// no JVMTI or JNI functions can be called in this callback
	array_add(&freed_objects, tag);
}

static void JNICALL callback_vm_init(jvmtiEnv *env, JNIEnv *jnienv, jthread thread) {
    jclass cls = (*jnienv)->FindClass(jnienv, "java/lang/Thread");
	EXCEPTION_CHECK(jnienv);
	jmethodID method = (*jnienv)->GetMethodID(jnienv, cls, "<init>", "()V");
	EXCEPTION_CHECK(jnienv);
    jthread new_thread = (*jnienv)->NewObject(jnienv, cls, method);
	EXCEPTION_CHECK(jnienv);

    DO_SAFE((*env)->RunAgentThread(env, new_thread, &free_thread_runnable, NULL, JVMTI_THREAD_MAX_PRIORITY), "creating agent thread");
}

static void JNICALL callback_gc_finish(jvmtiEnv *env) {
    DO_SAFE((*env)->RawMonitorEnter(env, free_lock), "entering free monitor");
    DO_SAFE((*env)->RawMonitorNotify(env, free_lock), "notifying free monitor");
    DO_SAFE((*env)->RawMonitorExit(env, free_lock), "exiting free monitor");
}
