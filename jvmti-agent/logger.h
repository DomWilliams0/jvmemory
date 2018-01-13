#ifndef JVMTI_AGENT_LOGGER
#define JVMTI_AGENT_LOGGER

extern void on_enter_method(const char *clazz, const char *method);
extern void on_exit_method();

extern void on_get_field(long obj_id, const char *method);
extern void on_put_field(long obj_id, const char *field, long value_id);

extern void on_store(long value_id, int index);
extern void on_load(int index);

extern void on_alloc(long obj_id, const char *class);
extern void on_dealloc(long obj_id);

#endif
