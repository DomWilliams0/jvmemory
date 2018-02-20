#include <jni.h>
#include <jvmti.h>
#include <stdlib.h>

#include "fields.h"
#include "agent.h"
#include "util.h"

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

	if (reference_kind == JVMTI_HEAP_REFERENCE_FIELD)
	{
		printf(
				"%lu: field %d, tagged from %lu, length %d\n",
				*tag_ptr,
				reference_info->field.index,
				*referrer_tag_ptr,
				length);
		return 0;
	}

	return 0;
}

static const jvmtiHeapCallbacks heap_callbacks = {
		.heap_reference_callback = callback_heap_ref
};


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
		DEALLOCATE(name);
		DEALLOCATE(sig);
	}
	DEALLOCATE(fields);
}

void discover_all_fields(JNIEnv *jnienv,
                         jclass cls,
                         fields_discovery_p discover)
{
	{
		char *cls_name;
		DO_SAFE((*env)->GetClassSignature(env, cls, &cls_name, NULL), "get class sig");
		int check = fields_discovery_check(discover, cls_name);
		DEALLOCATE(cls_name);

		if (check)
			return;
	}

	jint count;
	jclass *interfaces;
	DO_SAFE((*env)->GetImplementedInterfaces(env, cls, &count, &interfaces), "get interfaces");

	for (int i = 0; i < count; ++i)
	{
		jclass iface = interfaces[i];
		discover_all_fields(jnienv, iface, discover);
		(*jnienv)->DeleteLocalRef(jnienv, iface);
	}
	DEALLOCATE(interfaces);

	jclass super = cls;
	while ((super = (*jnienv)->GetSuperclass(jnienv, super)) != NULL)
	{
		discover_all_fields(jnienv, super, discover);
	}

	discover_fields(cls, discover);

}

void follow_references(jobject obj,
                       fields_p fields,
                       int count)
{

	// TODO pass in this array of fields_map as user_data
	DO_SAFE((*env)->FollowReferences(
			env,
			0,
			NULL,
			obj,
			&heap_callbacks,
			NULL
	), "following refs");

}
