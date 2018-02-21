#ifndef JVMTI_ALLOC
#define JVMTI_ALLOC

#include <jni.h>

struct any_string {
	union {
		jstring jstring;
		char *str;
	};
	jboolean is_jstring;
};

#define ALLOC_STRING_JSTRING(s) (struct any_string){.is_jstring = JNI_TRUE, .jstring = (s)}

void allocate_object_tag(JNIEnv *jnienv,
                         jobject obj,
                         struct any_string *clazz);

void allocate_array_tag(JNIEnv *jnienv,
                        jobject obj,
                        jint array_size,
                        struct any_string *clazz);

void allocate_tags_for_multidim_array(JNIEnv *jnienv,
                                      jobject arr,
                                      jint dims,
                                      struct any_string *clazz);

jlong get_tag(jobject obj);

#endif
