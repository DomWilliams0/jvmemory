#ifndef JVMTI_ALLOC
#define JVMTI_ALLOC

#include <jni.h>

void allocate_object_tag(JNIEnv *jnienv,
                         jobject obj);

void allocate_array_tag(JNIEnv *jnienv,
                        jobject obj,
                        jint array_size);

void allocate_tags_for_multidim_array(JNIEnv *jnienv,
                                      jobject arr,
                                      jint dims);

jlong get_tag(jobject obj);

#endif
