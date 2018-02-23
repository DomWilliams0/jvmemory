#ifndef JVMTI_AGENT_FIELDS
#define JVMTI_AGENT_FIELDS

struct explore_cache;
struct heap_explorer;
struct field;

typedef struct explore_cache *explore_cache_p;
typedef struct field_discovery *fields_discovery_p;
typedef struct heap_explorer *heap_explorer_p;

extern explore_cache_p explore_cache_init();

extern void explore_cache_free(explore_cache_p cache);

extern void emit_heap_differences(explore_cache_p cache,
                                  void *jnienv,
                                  jlong tag);

extern jboolean heap_explore_should_explore(heap_explorer_p explorer,
                                            jlong tag);

extern void heap_explore_visit_field(heap_explorer_p explorer,
                                     jlong referrer,
                                     jlong tag,
                                     int index);

extern void heap_explore_visit_array_element(heap_explorer_p explorer,
                                             jlong referrer,
                                             jlong tag,
                                             int index);

jobject get_single_object(jlong tag);


// in: count-number of tags
// out: class names in string array
// called from rust
void discover_fields_if_necessary(JNIEnv *jnienv,
                                  jint count,
                                  jlong *tags,
                                  char **clazzes_out);

// called from rust
void follow_references(heap_explorer_p explorer,
                       jlong obj);

#endif
