extern crate libc;
extern crate protobuf;
extern crate diff;

#[macro_use]
mod cstr;

mod proto;
mod io;
mod fields;
mod concurrent;

use libc::*;
use std::ffi::CStr;
use proto::*;
use proto::message::{MessageType, Variant};
use io::Logger;

pub use io::{logger_free, logger_init};
pub use fields::*;
pub use concurrent::*;

type Long = i64;
type Int = i32;
type String = *const c_char;
// TODO this is all horribly repetitive and can definitely be replaced with some macro magic

#[no_mangle]
pub extern "C" fn on_enter_method(
    logger: *mut Logger,
    thread_id: Long,
    obj_id: Long,
    class: String,
    method: String,
) {
    let mut payload = flow::MethodEnter::new();
    payload.class = get_string!(class);
    payload.method = get_string!(method);
    payload.obj_id = obj_id;

    let mut msg = Variant::new();
    msg.set_thread_id(thread_id);
    msg.set_method_enter(payload);
    msg.set_field_type(MessageType::METHOD_ENTER);
    io::log_message(logger, msg);
}

#[no_mangle]
pub extern "C" fn on_exit_method(logger: *mut Logger, thread_id: Long) {
    let mut msg = Variant::new();
    msg.set_thread_id(thread_id);
    msg.set_method_exit(flow::MethodExit::new());
    msg.set_field_type(MessageType::METHOD_EXIT);
    io::log_message(logger, msg);
}

#[no_mangle]
pub extern "C" fn on_get_field(logger: *mut Logger, thread_id: Long, obj_id: Long, field: String) {
    let mut payload = access::GetField::new();
    payload.id = obj_id;
    payload.field = get_string!(field);

    let mut msg = Variant::new();
    msg.set_thread_id(thread_id);
    msg.set_get_field(payload);
    msg.set_field_type(MessageType::GETFIELD);
    io::log_message(logger, msg);
}

#[no_mangle]
pub extern "C" fn on_put_field_object(
    logger: *mut Logger,
    thread_id: Long,
    obj_id: Long,
    field: String,
    value_id: Long,
) {
    let mut payload = access::PutFieldObject::new();
    payload.id = obj_id;
    payload.field = get_string!(field);
    payload.value_id = value_id;

    let mut msg = Variant::new();
    msg.set_thread_id(thread_id);
    msg.set_put_field_object(payload);
    msg.set_field_type(MessageType::PUTFIELD_OBJECT);
    io::log_message(logger, msg);
}

#[no_mangle]
pub extern "C" fn on_put_field_primitive(
    logger: *mut Logger,
    thread_id: Long,
    obj_id: Long,
    field: String,
) {
    let mut payload = access::PutFieldPrimitive::new();
    payload.id = obj_id;
    payload.field = get_string!(field);

    let mut msg = Variant::new();
    msg.set_thread_id(thread_id);
    msg.set_put_field_primitive(payload);
    msg.set_field_type(MessageType::PUTFIELD_PRIMITIVE);
    io::log_message(logger, msg);
}

#[no_mangle]
pub extern "C" fn on_get_static(
    logger: *mut Logger,
    thread_id: Long,
    class: String,
    field: String,
) {
    let mut payload = access::GetStatic::new();
    payload.class = get_string!(class);
    payload.field = get_string!(field);

    let mut msg = Variant::new();
    msg.set_thread_id(thread_id);
    msg.set_get_static(payload);
    msg.set_field_type(MessageType::GETSTATIC);
    io::log_message(logger, msg);
}

#[no_mangle]
pub extern "C" fn on_put_static_object(
    logger: *mut Logger,
    thread_id: Long,
    class: String,
    field: String,
    value_id: Long,
) {
    let mut payload = access::PutStaticObject::new();
    payload.class = get_string!(class);
    payload.field = get_string!(field);
    payload.value_id = value_id;

    let mut msg = Variant::new();
    msg.set_thread_id(thread_id);
    msg.set_put_static_object(payload);
    msg.set_field_type(MessageType::PUTSTATIC_OBJECT);
    io::log_message(logger, msg);
}

#[no_mangle]
pub extern "C" fn on_store_object(
    logger: *mut Logger,
    thread_id: Long,
    value_id: Long,
    index: Int,
) {
    let mut payload = access::StoreObject::new();
    payload.value_id = value_id;
    payload.index = index;

    let mut msg = Variant::new();
    msg.set_thread_id(thread_id);
    msg.set_store_object(payload);
    msg.set_field_type(MessageType::STORE_OBJECT);
    io::log_message(logger, msg);
}

#[no_mangle]
pub extern "C" fn on_store_primitive(logger: *mut Logger, thread_id: Long, index: Int) {
    let mut payload = access::StorePrimitive::new();
    payload.index = index;

    let mut msg = Variant::new();
    msg.set_thread_id(thread_id);
    msg.set_store_primitive(payload);
    msg.set_field_type(MessageType::STORE_PRIMITIVE);
    io::log_message(logger, msg);
}

#[no_mangle]
pub extern "C" fn on_store_object_in_array(
    logger: *mut Logger,
    thread_id: Long,
    value_id: Long,
    array_id: Long,
    index: Int,
) {
    let mut payload = access::StoreObjectInArray::new();
    payload.id = array_id;
    payload.index = index;
    payload.value_id = value_id;

    let mut msg = Variant::new();
    msg.set_thread_id(thread_id);
    msg.set_store_object_in_array(payload);
    msg.set_field_type(MessageType::STORE_OBJECT_IN_ARRAY);
    io::log_message(logger, msg);
}

#[no_mangle]
pub extern "C" fn on_store_primitive_in_array(
    logger: *mut Logger,
    thread_id: Long,
    array_id: Long,
    index: Int,
) {
    let mut payload = access::StorePrimitiveInArray::new();
    payload.id = array_id;
    payload.index = index;

    let mut msg = Variant::new();
    msg.set_thread_id(thread_id);
    msg.set_store_primitive_in_array(payload);
    msg.set_field_type(MessageType::STORE_PRIMITIVE_IN_ARRAY);
    io::log_message(logger, msg);
}

#[no_mangle]
pub extern "C" fn on_load(logger: *mut Logger, thread_id: Long, index: Int) {
    let mut payload = access::Load::new();
    payload.index = index;

    let mut msg = Variant::new();
    msg.set_thread_id(thread_id);
    msg.set_load(payload);
    msg.set_field_type(MessageType::LOAD);
    io::log_message(logger, msg);
}

#[no_mangle]
pub extern "C" fn on_load_from_array(
    logger: *mut Logger,
    thread_id: Long,
    array_id: Long,
    index: Int,
) {
    let mut payload = access::LoadFromArray::new();
    payload.id = array_id;
    if index >= 0 {
        payload.index = index;
    }

    let mut msg = Variant::new();
    msg.set_thread_id(thread_id);
    msg.set_load_from_array(payload);
    msg.set_field_type(MessageType::LOAD_ARRAY);
    io::log_message(logger, msg);
}

#[no_mangle]
pub extern "C" fn on_alloc_object(
    logger: *mut Logger,
    thread_id: Long,
    obj_id: Long,
    class: String,
) {
    let mut payload = allocations::AllocationObject::new();
    payload.id = obj_id;
    payload.field_type = get_string!(class);

    let mut msg = Variant::new();
    msg.set_thread_id(thread_id);
    msg.set_alloc_object(payload);
    msg.set_field_type(MessageType::ALLOC_OBJECT);
    io::log_message(logger, msg);
}

#[no_mangle]
pub extern "C" fn on_alloc_array(
    logger: *mut Logger,
    thread_id: Long,
    array_id: Long,
    class: String,
    size: i32,
) {
    let mut payload = allocations::AllocationArray::new();
    payload.id = array_id;
    payload.size = size;
    payload.field_type = get_string!(class);

    let mut msg = Variant::new();
    msg.set_thread_id(thread_id);
    msg.set_alloc_array(payload);
    msg.set_field_type(MessageType::ALLOC_ARRAY);
    io::log_message(logger, msg);
}

#[no_mangle]
pub extern "C" fn on_alloc_array_in_array(
    logger: *mut Logger,
    thread_id: Long,
    array_id: Long,
    class: String,
    size: i32,
    src_array_id: Long,
    src_index: Int,
) {
    let mut payload = allocations::AllocationArray::new();
    payload.id = array_id;
    payload.size = size;
    payload.field_type = get_string!(class);

    if src_array_id != 0 {
        payload.src_array_id = src_array_id;
        payload.src_index = src_index;
    }

    let mut msg = Variant::new();
    msg.set_thread_id(thread_id);
    msg.set_alloc_array(payload);
    msg.set_field_type(MessageType::ALLOC_ARRAY);
    io::log_message(logger, msg);
}

#[no_mangle]
pub extern "C" fn on_dealloc(logger: *mut Logger, obj_id: Long) {
    let mut payload = allocations::Deallocation::new();
    payload.id = obj_id;

    let mut msg = Variant::new();
    msg.set_thread_id(0);
    msg.set_dealloc(payload);
    msg.set_field_type(MessageType::DEALLOC);
    io::log_message(logger, msg);
}

#[no_mangle]
pub extern "C" fn on_define_class(
    logger: *mut Logger,
    thread_id: Long,
    buffer: *const u8,
    len: Int,
) {
    use proto::definitions::*;

    null_check!(buffer);
    let array = unsafe { std::slice::from_raw_parts(buffer, len as usize) };

    let def: ClassDefinition = match protobuf::core::parse_from_bytes(array) {
        Err(e) => {
            eprintln!("failed to deserialize class definition: {:?}", e);
            return;
        }
        Ok(def) => def,
    };

    let mut msg = Variant::new();
    msg.set_thread_id(thread_id);
    msg.set_class_def(def);
    msg.set_field_type(MessageType::CLASS_DEF);
    io::log_message(logger, msg)
}

#[no_mangle]
pub extern "C" fn to_string_object(
    logger: *mut Logger,
    thread_id: Long,
    obj_id: Long,
    to_string: String
) {
    let mut payload = definitions::ToStringObject::new();
    payload.obj_id = obj_id;
    payload.to_string = get_string!(to_string);

    let mut msg = Variant::new();
    msg.set_thread_id(thread_id);
    msg.set_to_string_object(payload);
    msg.set_field_type(MessageType::TO_STRING_OBJECT);
    io::log_message(logger, msg);
}
