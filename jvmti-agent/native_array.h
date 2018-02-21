#ifndef JVMTI_AGENT_NATIVE_ARRAY
#define JVMTI_AGENT_NATIVE_ARRAY

#include <jvmti.h>

void JNICALL callback_native_bind(jvmtiEnv
                                  *env,
                                  JNIEnv *jnienv,
                                  jthread
                                  thread,
                                  jmethodID method,
                                  void *address,
                                  void **new_address_ptr);

#endif
