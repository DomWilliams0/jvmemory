/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class ms_domwillia_jvmemory_monitor_Monitor */

#ifndef _Included_ms_domwillia_jvmemory_monitor_Monitor
#define _Included_ms_domwillia_jvmemory_monitor_Monitor
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    setProgramInProgress
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_setProgramInProgress
  (JNIEnv *, jclass, jboolean);

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    onClassLoad
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_onClassLoad
  (JNIEnv *, jclass, jboolean);

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
 * Method:    onPutFieldObject
 * Signature: (JLjava/lang/String;J)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_onPutFieldObject
  (JNIEnv *, jclass, jlong, jstring, jlong);

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    onPutFieldPrimitive
 * Signature: (JLjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_onPutFieldPrimitive
  (JNIEnv *, jclass, jlong, jstring);

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    onStoreLocalVarObject
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_onStoreLocalVarObject
  (JNIEnv *, jclass, jlong, jint);

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    onStoreLocalVarPrimitive
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_onStoreLocalVarPrimitive
  (JNIEnv *, jclass, jint);

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    onLoadLocalVar
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_onLoadLocalVar
  (JNIEnv *, jclass, jint);

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    onDefineClass
 * Signature: ([B)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_onDefineClass
  (JNIEnv *, jclass, jbyteArray);

#ifdef __cplusplus
}
#endif
#endif
