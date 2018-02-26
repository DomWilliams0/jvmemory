#include <string.h>
#include <jni.h>
#include <jvmti.h>

#include "id_array.h"
#include "util.h"
#include "native_array.h"
#include "thread_local.h"

static JavaVM *jvm = NULL;
jvmtiEnv *env = NULL;
logger_p logger = NULL;
concurrent_p concurrent = NULL;
explore_cache_p explore_cache = NULL;

static struct id_array freed_objects;
static jrawMonitorID free_lock;

static jvmtiError add_capabilities()
{
	jvmtiCapabilities capa = {0};

	capa.can_tag_objects                        = 1;
	capa.can_generate_object_free_events        = 1;
	capa.can_generate_garbage_collection_events = 1;
	capa.can_generate_native_method_bind_events = 1;

	return (*env)->AddCapabilities(env, &capa);
}

static void JNICALL callback_dealloc(jvmtiEnv *env,
                                     jlong tag);

static void JNICALL callback_vm_start(jvmtiEnv *env,
                                      JNIEnv *jnienv);

static void JNICALL callback_vm_init(jvmtiEnv *env,
                                     JNIEnv *jnienv,
                                     jthread thread);

static void JNICALL callback_vm_death(jvmtiEnv *env,
                                      JNIEnv *jnienv);

static void JNICALL callback_gc_finish(jvmtiEnv *env);

static void JNICALL callback_thread_start(jvmtiEnv *env,
                                          JNIEnv *jnienv,
                                          jthread thread);

static void JNICALL callback_thread_end(jvmtiEnv *env,
                                        JNIEnv *jnienv,
                                        jthread thread);

static jvmtiError register_callbacks()
{
#define ENABLE_EVENT(e) \
    DO_SAFE_RETURN((*env)->SetEventNotificationMode(env, JVMTI_ENABLE, e, (jthread) NULL))

	jvmtiEventCallbacks callbacks = {0};
	callbacks.ObjectFree = &callback_dealloc;
	callbacks.VMStart = &callback_vm_start;
	callbacks.VMInit = &callback_vm_init;
	callbacks.VMDeath = &callback_vm_death;
	callbacks.GarbageCollectionFinish = &callback_gc_finish;
	callbacks.NativeMethodBind = &callback_native_bind;
	callbacks.ThreadStart = &callback_thread_start;
	callbacks.ThreadEnd = &callback_thread_end;

	DO_SAFE_RETURN((*env)->SetEventCallbacks(env, &callbacks, sizeof(callbacks)));
	ENABLE_EVENT(JVMTI_EVENT_OBJECT_FREE);
	ENABLE_EVENT(JVMTI_EVENT_VM_INIT);
	ENABLE_EVENT(JVMTI_EVENT_VM_START);
	ENABLE_EVENT(JVMTI_EVENT_VM_DEATH);
	ENABLE_EVENT(JVMTI_EVENT_GARBAGE_COLLECTION_FINISH);
	ENABLE_EVENT(JVMTI_EVENT_NATIVE_METHOD_BIND);
	ENABLE_EVENT(JVMTI_EVENT_THREAD_START);
	ENABLE_EVENT(JVMTI_EVENT_THREAD_END);

	return JVMTI_ERROR_NONE;
}

JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *javavm,
                                    char *options,
                                    void *reserved)
{
	jint ret;

	// parse options
	char *out_path = options == NULL ? "jvmemory.log" : options;

	// set globals
	jvm = javavm;
	if ((ret = (*jvm)->GetEnv(jvm, (void **) &env, JVMTI_VERSION_1_2)) != JNI_OK)
	{
		fprintf(stderr, "failed to create environment: %d\n", ret);
		return ret;
	}

	// add required capabilities
	DO_SAFE(add_capabilities(), "adding required capabilities");

	// register callbacks
	DO_SAFE(register_callbacks(), "registering callbacks");

	// init list
	DO_SAFE_COND(id_array_init(&freed_objects) == 0, "initialising freed objects id_array");

	// create free thread lock
	DO_SAFE((*env)->CreateRawMonitor(env, "free_lock", &free_lock), "creating raw monitor");

	// init logger
	DO_SAFE_COND((logger = logger_init(out_path)) != NULL, "logger initialisation");

	// init concurrent
	DO_SAFE_COND((concurrent = concurrent_init()) != NULL, "concurrent initialisation");

	// init explore cache
	DO_SAFE_COND((explore_cache = explore_cache_init()) != NULL, "explore cache initialisation");

	return JNI_OK;
}

JNIEXPORT void JNICALL Agent_OnUnload(JavaVM *vm)
{
	DO_SAFE((*env)->RawMonitorEnter(env, free_lock), "entering free monitor");
	id_array_free(&freed_objects);
	logger_free(logger);
	logger = NULL;
	concurrent_free(concurrent);
	concurrent = NULL;
	explore_cache_free(explore_cache);
	explore_cache = NULL;
	DO_SAFE((*env)->RawMonitorExit(env, free_lock), "exiting free monitor");
}

static void flush_queued_frees(JNIEnv *jnienv)
{

	for (size_t i = 0; i < freed_objects.count; i++)
	{
		on_dealloc(logger, freed_objects.data[i]);
	}

	id_array_clear(&freed_objects);

}

static void JNICALL free_thread_runnable(jvmtiEnv *env,
                                         JNIEnv *jnienv,
                                         void *arg)
{
	while (1)
	{
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

static void JNICALL callback_dealloc(jvmtiEnv *env,
                                     jlong tag)
{
	// no JVMTI or JNI functions can be called in this callback
	id_array_add(&freed_objects, tag);
}

static void JNICALL callback_vm_init(jvmtiEnv *env,
                                     JNIEnv *jnienv,
                                     jthread thread)
{
	jclass cls = (*jnienv)->FindClass(jnienv, "java/lang/Thread");
	EXCEPTION_CHECK(jnienv);
	jmethodID method = (*jnienv)->GetMethodID(jnienv, cls, "<init>", "()V");
	EXCEPTION_CHECK(jnienv);
	jthread new_thread = (*jnienv)->NewObject(jnienv, cls, method);
	EXCEPTION_CHECK(jnienv);

	DO_SAFE((*env)->RunAgentThread(env, new_thread, &free_thread_runnable, NULL, JVMTI_THREAD_MAX_PRIORITY),
	        "creating agent thread");
}

static void JNICALL callback_vm_start(jvmtiEnv *env,
                                      JNIEnv *jnienv)
{
	thread_local_state_init(1);
}

static void JNICALL callback_vm_death(jvmtiEnv *env,
                                      JNIEnv *jnienv)
{
	thread_local_state_free();
}

static void JNICALL callback_gc_finish(jvmtiEnv *env)
{
	DO_SAFE((*env)->RawMonitorEnter(env, free_lock), "entering free monitor");
	DO_SAFE((*env)->RawMonitorNotify(env, free_lock), "notifying free monitor");
	DO_SAFE((*env)->RawMonitorExit(env, free_lock), "exiting free monitor");
}

static void JNICALL callback_thread_start(jvmtiEnv *env,
                                          JNIEnv *jnienv,
                                          jthread thread)
{
	thread_local_state_init(get_thread_id(jnienv));
}

static void JNICALL callback_thread_end(jvmtiEnv *env,
                                        JNIEnv *jnienv,
                                        jthread thread)
{
	thread_local_state_free();
}

void deallocate(void *p)
{
	(*env)->Deallocate(env, (unsigned char *) (p));
}

long get_thread_id(JNIEnv *jnienv)
{
	static jclass thread_cls;
	static jmethodID thread_get_id;

	jthread t;
	DO_SAFE((*env)->GetCurrentThread(env, &t), "get current thread");
	EXCEPTION_CHECK(jnienv);

	if (thread_cls == NULL)
	{
		thread_cls = (*jnienv)->GetObjectClass(jnienv, t);
		EXCEPTION_CHECK(jnienv);
	}

	if (thread_get_id == NULL)
	{
		thread_get_id = (*jnienv)->GetMethodID(jnienv, thread_cls, "getId", "()J");
		EXCEPTION_CHECK(jnienv);
	}

	long id = (*jnienv)->CallLongMethod(jnienv, t, thread_get_id);
	EXCEPTION_CHECK(jnienv);
	return id;
}
