#include <jni.h>
#include <jvmti.h>
#include <stdlib.h>

#include "fields.h"
#include "agent.h"
#include "util.h"
#include "alloc.h"

static jint JNICALL callback_heap_ref(
		jvmtiHeapReferenceKind reference_kind,
		const jvmtiHeapReferenceInfo *reference_info,
		jlong class_tag,
		jlong referrer_class_tag,
		jlong size,
		jlong *tag_ptr,
		jlong *referrer_tag_ptr,
		jint length,
		void *user_data)
{
	heap_explorer_p explorer = (heap_explorer_p) user_data;

	if (!heap_explore_should_explore(explorer, *referrer_tag_ptr))
		return 0;

	if (reference_kind == JVMTI_HEAP_REFERENCE_FIELD)
	{
		heap_explore_visit_field(explorer, *referrer_tag_ptr, *tag_ptr, reference_info->field.index);
		return JVMTI_VISIT_OBJECTS;
	} else if (reference_kind == JVMTI_HEAP_REFERENCE_ARRAY_ELEMENT)
	{
		heap_explore_visit_array_element(explorer, *referrer_tag_ptr, *tag_ptr, reference_info->array.index);
		return JVMTI_VISIT_OBJECTS;
	}

	return JVMTI_VISIT_OBJECTS;
}

static const jvmtiHeapCallbacks heap_callbacks = {
		.heap_reference_callback = callback_heap_ref
};


extern fields_discovery_p fields_discovery_init();

extern int fields_discovery_check(fields_discovery_p discover,
                                  const char *cls);

extern void fields_discovery_register(fields_discovery_p discover,
                                      const char *name,
                                      const char *cls);

extern void fields_discovery_finish(fields_discovery_p discover,
                                    explore_cache_p cache,
                                    const char *cls);

extern jboolean fields_discovery_has_discovered(explore_cache_p explore_cache,
                                                const char *cls);

static void discover_fields(jclass cls,
                            fields_discovery_p discover)
{
	jint field_count;
	jfieldID *fields;
	DO_SAFE((*env)->GetClassFields(env, cls, &field_count, &fields), "get class fields");
	for (int i = 0; i < field_count; ++i)
	{
		jfieldID fid = fields[i];
		char *name;
		char *sig;
		DO_SAFE((*env)->GetFieldName(env, cls, fid, &name, &sig, NULL), "get field name");
		fields_discovery_register(discover, name, sig);
		deallocate(name);
		deallocate(sig);
	}
	deallocate(fields);
}

// if cls_name_already is null, it will be fetched
static void discover_all_fields(JNIEnv *jnienv,
                                jclass cls,
                                char *cls_name_already,
                                fields_discovery_p discover)
{
	{
		char *cls_name;
		if (cls_name_already != NULL)
		{
			cls_name = cls_name_already;
		} else
		{
			DO_SAFE((*env)->GetClassSignature(env, cls, &cls_name, NULL), "get class sig");
		}

		int check = fields_discovery_check(discover, cls_name);

		if (cls_name_already == NULL)
			deallocate(cls_name);

		if (check)
			return;
	}

	jint count;
	jclass *interfaces;
	DO_SAFE((*env)->GetImplementedInterfaces(env, cls, &count, &interfaces), "get interfaces");

	for (int i = 0; i < count; ++i)
	{
		jclass iface = interfaces[i];
		discover_all_fields(jnienv, iface, NULL, discover);
		(*jnienv)->DeleteLocalRef(jnienv, iface);
	}
	deallocate(interfaces);

	jclass super = cls;
	while ((super = (*jnienv)->GetSuperclass(jnienv, super)) != NULL)
	{
		discover_all_fields(jnienv, super, NULL, discover);
	}

	discover_fields(cls, discover);

}

jobject get_single_object(jlong tag)
{
	jobject *objs = NULL;
	jint count = 0;
	DO_SAFE((*env)->GetObjectsWithTags(env, 1, &tag, &count, &objs, NULL), "get obj with tag");

	jobject obj = count == 1 ? objs[0] : NULL;
	deallocate(objs);

	return obj;
}

void discover_fields_if_necessary(JNIEnv *jnienv,
                                  jint count,
                                  jlong *tags,
                                  char **clazzes_out)
{
	// get classes and names
	jint found_count = 0;
	jobject *objs = NULL;
	DO_SAFE((*env)->GetObjectsWithTags(env, count, tags, &found_count, &objs, NULL), "get objects with tags");

	// this assertion should never break!
	DO_SAFE_COND(found_count == count, "tags are not unique! oh dear!");

	for (int i = 0; i < count; ++i)
	{
		jobject obj = objs[i];
		jclass cls = (*jnienv)->GetObjectClass(jnienv, obj);
		EXCEPTION_CHECK(jnienv);

		char *class_name = NULL;
		DO_SAFE((*env)->GetClassSignature(env, cls, &class_name, NULL), "get class sig");

		if (!fields_discovery_has_discovered(explore_cache, class_name))
		{
			fields_discovery_p discover = fields_discovery_init();
			discover_all_fields(jnienv, cls, class_name, discover);
			fields_discovery_finish(discover, explore_cache, class_name);
		}

		// make sure to deallocate class name later
		clazzes_out[i] = class_name;
		tags[i] = get_tag(obj);

		(*jnienv)->DeleteLocalRef(jnienv, obj);
		(*jnienv)->DeleteLocalRef(jnienv, cls);
	}

	deallocate(objs);
}

void follow_references(heap_explorer_p explorer,
                       jlong obj)
{

	DO_SAFE((*env)->FollowReferences(
			env,
			JVMTI_HEAP_FILTER_UNTAGGED,
			NULL,
			get_single_object(obj),
			&heap_callbacks,
			explorer
	), "following refs");

}
