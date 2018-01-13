#ifndef JVMTI_AGENT_UTIL
#define JVMTI_AGENT_UTIL

// one liners that return jvmtiError only
#define DO_SAFE_RETURN(code) do {\
	jvmtiError err = code;\
	if (err != JVMTI_ERROR_NONE)\
		return err;\
} while (0)

#define DO_SAFE_COND(condition, what) do {\
	if (!(condition)) {\
		fprintf(stderr, "Fatal error: %s",\
				what\
				);\
		fflush(stderr);\
		exit(99);\
	}\
} while (0)

#define EXCEPTION_CHECK(env) do {\
	if ((*env)->ExceptionCheck(env) == JNI_TRUE) {\
		(*env)->ExceptionDescribe(env);\
		exit(99);\
	}\
} while (0)


#define DO_SAFE(code, what) do {\
	jvmtiError err = code;\
	if (err != JVMTI_ERROR_NONE) {\
		char *str = NULL;\
		(*env)->GetErrorName(env, err, &str);\
		fprintf(stderr, "Fatal error: %s: %s\n",\
				what,\
				str ? str : "unknown error"\
				);\
		fflush(stderr);\
		exit(99);\
	}\
} while (0)

#define DEBUG_PRINT_STRING(jnienv, str, what) do {\
    const char *chars = (*jnienv)->GetStringUTFChars(jnienv, str, NULL);\
    printf("DEBUG: %s: '%s'\n", what, chars);\
    (*jnienv)->ReleaseStringUTFChars(jnienv, str, chars);\
} while (0)


#define GET_THREAD_ID\
    long thread_id = get_thread_id(jnienv)

long get_thread_id(JNIEnv *jnienv);


#endif
