#include <string.h>
#include <jni.h>
#include <jvmti.h>

#include "id_array.h"
#include "util.h"
#include "logger.h"
#include "fields.h"
#include "alloc.h"

// TODO are any of these globals thread safe?
//		i dont think so
//		does it matter?
static JavaVM *jvm = NULL;
jvmtiEnv *env = NULL;
logger_p logger = NULL;
fields_map_p fields_map = NULL;

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

static void JNICALL callback_vm_init(jvmtiEnv *env,
                                     JNIEnv *jnienv,
                                     jthread thread);

static void JNICALL callback_gc_finish(jvmtiEnv *env);

void JNICALL callback_native_bind(
		jvmtiEnv *env,
		JNIEnv *jnienv,
		jthread thread,
		jmethodID method,
		void *address,
		void **new_address_ptr);

static jvmtiError register_callbacks()
{
#define ENABLE_EVENT(e) \
    DO_SAFE_RETURN((*env)->SetEventNotificationMode(env, JVMTI_ENABLE, e, (jthread) NULL))

	jvmtiEventCallbacks callbacks = {0};
	callbacks.ObjectFree = &callback_dealloc;
	callbacks.VMInit = &callback_vm_init;
	callbacks.GarbageCollectionFinish = &callback_gc_finish;
	callbacks.NativeMethodBind = &callback_native_bind;

	DO_SAFE_RETURN((*env)->SetEventCallbacks(env, &callbacks, sizeof(callbacks)));
	ENABLE_EVENT(JVMTI_EVENT_OBJECT_FREE);
	ENABLE_EVENT(JVMTI_EVENT_VM_INIT);
	ENABLE_EVENT(JVMTI_EVENT_GARBAGE_COLLECTION_FINISH);
	ENABLE_EVENT(JVMTI_EVENT_NATIVE_METHOD_BIND);

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

	// init fields_map map
	DO_SAFE_COND((fields_map = fields_init()) != NULL, "fields_map map initialisation");

	return JNI_OK;
}

JNIEXPORT void JNICALL Agent_OnUnload(JavaVM *vm)
{
	DO_SAFE((*env)->RawMonitorEnter(env, free_lock), "entering free monitor");
	id_array_free(&freed_objects);
	logger_free(logger);
	logger = NULL;
	fields_free(fields_map);
	fields_map = NULL;
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

static void JNICALL callback_gc_finish(jvmtiEnv *env)
{
	DO_SAFE((*env)->RawMonitorEnter(env, free_lock), "entering free monitor");
	DO_SAFE((*env)->RawMonitorNotify(env, free_lock), "notifying free monitor");
	DO_SAFE((*env)->RawMonitorExit(env, free_lock), "exiting free monitor");
}

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


long get_thread_id(JNIEnv *jnienv)
{
	jthread thread;
	long id = 0;
	if ((*env)->GetCurrentThread(env, &thread) == JVMTI_ERROR_NONE)
	{
		// TODO cache
		jclass cls = (*jnienv)->GetObjectClass(jnienv, thread);
		EXCEPTION_CHECK(jnienv);
		jmethodID method = (*jnienv)->GetMethodID(jnienv, cls, "getId", "()J");
		EXCEPTION_CHECK(jnienv);
		id = (*jnienv)->CallLongMethod(jnienv, thread, method);
		EXCEPTION_CHECK(jnienv);
	}

	return id;
}
