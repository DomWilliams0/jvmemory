#ifndef JVMTI_AGENT_FIELDS
#define JVMTI_AGENT_FIELDS

struct fields_map;
struct field_discovery;
struct field {
    const char *name;
	const char *clazz;
};

typedef struct fields_map *fields_map_p;
typedef struct field_discovery *fields_discovery_p;
typedef struct field *fields_p;

extern fields_map_p fields_init();

extern fields_p fields_get(fields_map_p map,
                           const char *cls,
                           int *count);

extern void fields_free(fields_map_p map);

extern fields_discovery_p fields_discovery_init();

extern int fields_discovery_check(fields_discovery_p discover,
                                  const char *cls);

extern void fields_discovery_register(fields_discovery_p discover,
                                      const char *name,
                                      const char *cls);

extern void fields_discovery_finish(fields_discovery_p discover,
                                    fields_map_p map,
                                    const char *cls);

void discover_all_fields(JNIEnv *jnienv,
                         jclass cls,
                         fields_discovery_p discover);

void follow_references(jobject obj,
                       fields_p fields,
                       jint count);

#endif
