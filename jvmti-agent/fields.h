#ifndef JVMTI_AGENT_FIELDS
#define JVMTI_AGENT_FIELDS

struct explore_cache;
struct heap_explorer;
struct field;

typedef struct explore_cache *explore_cache_p;
//typedef struct field_discovery *fields_discovery_p;
typedef struct heap_explorer *heap_explorer_p;

extern explore_cache_p explore_cache_init();

extern void explore_cache_free(explore_cache_p cache);

extern heap_explorer_p heap_explore_init(long tag);

extern void heap_explore_finish(heap_explorer_p explorer,
                                explore_cache_p cache,
                                jlong **tags_to_discover,
                                jint *discover_count);

extern void heap_explore_free_discover_tags(jlong *tags_to_discover);

extern jboolean heap_explore_should_explore(heap_explorer_p explorer,
                                            long tag);

extern void heap_explore_visit_field(heap_explorer_p explorer,
                                     long referrer,
                                     long tag,
                                     int index);

extern void heap_explore_visit_array_element(heap_explorer_p explorer,
                                             long referrer,
                                             long tag,
                                             int index);

/*
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
                         */

void follow_references(heap_explorer_p explorer,
                       jobject obj);

#endif
