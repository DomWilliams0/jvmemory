#ifndef JVMTI_AGENT_LOGGER
#define JVMTI_AGENT_LOGGER

struct logger;
typedef struct logger *logger_p;

extern logger_p logger_init(const char *path);

extern void logger_free(logger_p logger);

extern void on_enter_method(logger_p logger,
                            long thread_id,
                            long obj_id,
                            const char *clazz,
                            const char *method);

extern void on_exit_method(logger_p logger,
                           long thread_id);

extern void on_get_field(logger_p logger,
                         long thread_id,
                         long obj_id,
                         const char *method);

extern void on_put_field_object(logger_p logger,
                                long thread_id,
                                long obj_id,
                                const char *field,
                                long value_id);

extern void on_put_field_primitive(logger_p logger,
                                   long thread_id,
                                   long obj_id,
                                   const char *field);

extern void on_get_static(logger_p logger,
                          long thread_id,
                          const char *clazz,
                          const char *field);

extern void on_put_static_object(logger_p logger,
                                 long thread_id,
                                 const char *clazz,
                                 const char *field,
                                 long value_id);

extern void on_store_object(logger_p logger,
                            long thread_id,
                            long value_id,
                            int index);

extern void on_store_primitive(logger_p logger,
                               long thread_id,
                               int index);

extern void on_store_object_in_array(logger_p logger,
                                     long thread_id,
                                     long value_id,
                                     long array_id,
                                     int index);

extern void on_store_primitive_in_array(logger_p logger,
                                        long thread_id,
                                        long array_id,
                                        int index);

extern void on_load(logger_p logger,
                    long thread_id,
                    int index);

extern void on_load_from_array(logger_p logger,
                               long thread_id,
                               long array_id,
                               int index);

extern void on_alloc_object(logger_p logger,
                            long thread_id,
                            long obj_id,
                            const char *class);

extern void on_alloc_array(logger_p logger,
                           long thread_id,
                           long obj_id,
                           const char *class,
                           int size);

extern void on_alloc_array_in_array(logger_p logger,
                                    long thread_id,
                                    long obj_id,
                                    const char *class,
                                    int size,
                                    long src_array_id,
                                    int src_index);

extern void on_dealloc(logger_p logger,
                       long obj_id);

extern void on_define_class(logger_p logger,
                            long thread_id,
                            const char *buffer,
                            int len);

#endif
