/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class ms_domwillia_jvmemory_monitor_Monitor */

#ifndef _Included_ms_domwillia_jvmemory_monitor_Monitor
#define _Included_ms_domwillia_jvmemory_monitor_Monitor
#ifdef __cplusplus
extern "C" {
#endif
#undef ms_domwillia_jvmemory_monitor_Monitor_invalidInstanceId
#define ms_domwillia_jvmemory_monitor_Monitor_invalidInstanceId 0LL
/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    allocateTag
 * Signature: (Ljava/lang/Object;)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_allocateTag
  (JNIEnv *, jclass, jobject);

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    getTag
 * Signature: (Ljava/lang/Object;)J
 */
JNIEXPORT jlong JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_getTag
  (JNIEnv *, jclass, jobject);

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    enterMethod
 * Signature: (Ljava/lang/String;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_enterMethod
  (JNIEnv *, jclass, jstring, jstring);

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    exitMethod
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_exitMethod
  (JNIEnv *, jclass);

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    onAlloc
 * Signature: (JLjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_onAlloc
  (JNIEnv *, jclass, jlong, jstring);

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    onDealloc
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_onDealloc
  (JNIEnv *, jclass, jlong);

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    onGetField
 * Signature: (JLjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_onGetField
  (JNIEnv *, jclass, jlong, jstring);

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    onPutField
 * Signature: (JLjava/lang/String;J)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_onPutField
  (JNIEnv *, jclass, jlong, jstring, jlong);

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    onStoreLocalVar
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_onStoreLocalVar
  (JNIEnv *, jclass, jlong, jint);

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    onLoadLocalVar
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_onLoadLocalVar
  (JNIEnv *, jclass, jint);

#ifdef __cplusplus
}
#endif
#endif
