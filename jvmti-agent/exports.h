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
		(JNIEnv *,
		 jclass,
		 jboolean);

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    onClassLoad
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_onClassLoad
		(JNIEnv *,
		 jclass,
		 jboolean);

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    allocateTag
 * Signature: (Ljava/lang/String;Ljava/lang/Object;)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_allocateTag
		(JNIEnv *,
		 jclass,
		 jstring,
		 jobject);

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    allocateTagForArray
 * Signature: (ILjava/lang/Object;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_allocateTagForArray
		(JNIEnv *,
		 jclass,
		 jint,
		 jobject,
		 jstring);

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    allocateTagForMultiDimArray
 * Signature: (Ljava/lang/Object;ILjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_allocateTagForMultiDimArray
		(JNIEnv *,
		 jclass,
		 jobject,
		 jint,
		 jstring);

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    allocateTagForConstant
 * Signature: (Ljava/lang/Object;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_allocateTagForConstant
		(JNIEnv *,
		 jclass,
		 jobject,
		 jstring);

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    newArrayWrapper
 * Signature: (Ljava/lang/Class;I)Ljava/lang/Object;
 */
JNIEXPORT jobject JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_newArrayWrapper
		(JNIEnv *,
		 jclass,
		 jclass,
		 jint);

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    multiNewArrayWrapper
 * Signature: (Ljava/lang/Class;[Ljava/lang/Integer;)Ljava/lang/Object;
 */
JNIEXPORT jobject JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_multiNewArrayWrapper
		(JNIEnv *,
		 jclass,
		 jclass,
		 jobjectArray);

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    getTag
 * Signature: (Ljava/lang/Object;)J
 */
JNIEXPORT jlong JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_getTag
		(JNIEnv *,
		 jclass,
		 jobject);

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    enterMethod
 * Signature: (Ljava/lang/String;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_enterMethod
		(JNIEnv *,
		 jclass,
		 jstring,
		 jstring);

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    exitMethod
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_exitMethod
		(JNIEnv *,
		 jclass);

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    primeForSystemMethod
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_primeForSystemMethod
		(JNIEnv *,
		 jclass);

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    enterSystemMethod
 * Signature: (Ljava/lang/Object;)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_enterSystemMethod
		(JNIEnv *,
		 jclass,
		 jobject);

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    onGetField
 * Signature: (Ljava/lang/Object;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_onGetField
		(JNIEnv *,
		 jclass,
		 jobject,
		 jstring);

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    onPutFieldObject
 * Signature: (Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_onPutFieldObject
		(JNIEnv *,
		 jclass,
		 jobject,
		 jobject,
		 jstring);

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    onPutFieldPrimitive
 * Signature: (Ljava/lang/Object;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_onPutFieldPrimitive
		(JNIEnv *,
		 jclass,
		 jobject,
		 jstring);

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    onStoreLocalVarObject
 * Signature: (Ljava/lang/Object;I)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_onStoreLocalVarObject
		(JNIEnv *,
		 jclass,
		 jobject,
		 jint);

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    onStoreLocalVarPrimitive
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_onStoreLocalVarPrimitive
		(JNIEnv *,
		 jclass,
		 jint);

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    onStoreObjectInArray
 * Signature: (Ljava/lang/Object;Ljava/lang/Object;I)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_onStoreObjectInArray
		(JNIEnv *,
		 jclass,
		 jobject,
		 jobject,
		 jint);

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    onStorePrimitiveInArray
 * Signature: (Ljava/lang/Object;I)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_onStorePrimitiveInArray
		(JNIEnv *,
		 jclass,
		 jobject,
		 jint);

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    onLoadFromArray
 * Signature: (Ljava/lang/Object;I)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_onLoadFromArray
		(JNIEnv *,
		 jclass,
		 jobject,
		 jint);

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    onLoadLocalVar
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_onLoadLocalVar
		(JNIEnv *,
		 jclass,
		 jint);

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    onDefineClass
 * Signature: ([B)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_onDefineClass
		(JNIEnv *,
		 jclass,
		 jbyteArray);

#ifdef __cplusplus
}
#endif
#endif
