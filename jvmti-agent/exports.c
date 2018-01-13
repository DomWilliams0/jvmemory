#include <jni.h>
#include <jvmti.h>
#include "agent.h"
#include "exports.h"

static jlong next_id = 1;

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    allocateTag
 * Signature: (Ljava/lang/Object;)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_allocateTag(
        JNIEnv *jnienv,
        jclass klass,
        jobject obj) {

    jlong new_tag = next_id++;

    jvmtiError err;
    if ((err = (*env)->SetTag(env, obj, new_tag)) == JVMTI_ERROR_NONE) {
        // debug log
        char *name = NULL;
        jclass cls = (*jnienv)->GetObjectClass(jnienv, obj);
        if ((err = (*env)->GetClassSignature(env, cls, &name, NULL)) == JVMTI_ERROR_NONE) {
            on_alloc(logger, new_tag, name);
            (*env)->Deallocate(env, (unsigned char *)name);
            name = NULL;
        } else {
            fprintf(stderr, "could not get class name: %d\n", err);
        }
    } else {
        fprintf(stderr, "could not allocate tag: %d\n", err);
    }
}

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    getTag
 * Signature: (Ljava/lang/Object;)J
 */
JNIEXPORT jlong JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_getTag(
        JNIEnv *jnienv,
        jclass klass,
        jobject obj) {

    jlong tag = 0L;
    (*env)->GetTag(env, obj, &tag);
    return tag;
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
        jstring method_name) {

    const char *cls = (*jnienv)->GetStringUTFChars(jnienv, class_name, NULL);
    const char *mthd = (*jnienv)->GetStringUTFChars(jnienv, method_name, NULL);
    on_enter_method(logger, cls, mthd);
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
        jclass klass) {
    on_exit_method(logger);
}

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    onGetField
 * Signature: (JLjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_onGetField(
        JNIEnv *jnienv,
        jclass klass,
        jlong obj_id,
        jstring field) {
    const char *field_str = (*jnienv)->GetStringUTFChars(jnienv, field, NULL);
    on_get_field(logger, obj_id, field_str);
    (*jnienv)->ReleaseStringUTFChars(jnienv, field, field_str);
}

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    onPutField
 * Signature: (JLjava/lang/String;J)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_onPutField(
        JNIEnv *jnienv,
        jclass klass,
        jlong obj_id,
        jstring field,
        jlong value_id) {
    const char *field_str = (*jnienv)->GetStringUTFChars(jnienv, field, NULL);
    on_put_field(logger, obj_id, field_str, value_id);
    (*jnienv)->ReleaseStringUTFChars(jnienv, field, field_str);
}

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    onStoreLocalVar
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_onStoreLocalVar(
        JNIEnv *jnienv,
        jclass klass,
        jlong value_id,
        jint index) {
    on_store(logger, value_id, index);
}

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    onLoadLocalVar
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_onLoadLocalVar(
        JNIEnv *jnienv,
        jclass klass,
        jint index) {
    on_load(logger, index);
}

/*
 * Class:     ms_domwillia_jvmemory_monitor_Monitor
 * Method:    onDefineClass
 * Signature: ([B)V
 */
JNIEXPORT void JNICALL Java_ms_domwillia_jvmemory_monitor_Monitor_onDefineClass(
        JNIEnv *jnienv,
        jclass klass,
        jbyteArray def) {

    // TODO avoid possible copy
    jbyte *array = (*jnienv)->GetByteArrayElements(jnienv, def, NULL);
    jint len = (*jnienv)->GetArrayLength(jnienv, def);

    on_define_class(logger, (const char *) array, len);

    (*jnienv)->ReleaseByteArrayElements(jnienv, def, array, JNI_ABORT);
}
