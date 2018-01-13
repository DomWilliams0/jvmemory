extern crate libc;
extern crate protobuf;

mod proto;

use libc::*;
use std::ffi::CStr;
use proto::*;
use proto::message::{Variant, MessageType};

type Long = i64;
type Int = i32;
type String = *const c_char;

macro_rules! null_check {
    ($p:ident) => (if $p.is_null() {
        eprintln!("jni passed null to logger as '{}'", stringify!($p));
        return;
    })
}

macro_rules! get_string {
    ($p:ident) => {{
        null_check!($p);
        match unsafe {CStr::from_ptr($p)}.to_str() {
            Err(_) => {
                eprintln!("jni passed bad String as '{}'", stringify!($p));
                return;
            },
            Ok(cstr) => cstr.to_owned(),
        }
    }}
}

// TODO this is all horribly repetitive and can definitely be replaced with some macro magic

#[no_mangle]
pub extern fn on_enter_method(class: String, method: String) {
    let mut payload = flow::MethodEnter::new();
    payload.class = get_string!(class);
    payload.method = get_string!(method);

    let mut msg = Variant::new();
    msg.set_method_enter(payload);
    msg.set_field_type(MessageType::METHOD_ENTER);
    log_message(msg);}

#[no_mangle]
pub extern fn on_exit_method() {
    let payload = flow::MethodExit::new();

    let mut msg = Variant::new();
    msg.set_method_exit(payload);
    msg.set_field_type(MessageType::METHOD_EXIT);
    log_message(msg);}

#[no_mangle]
pub extern fn on_get_field(obj_id: Long, field: String) {
    let mut payload = access::GetField::new();
    payload.id = obj_id;
    payload.field = get_string!(field);

    let mut msg = Variant::new();
    msg.set_get_field(payload);
    msg.set_field_type(MessageType::GETFIELD);
    log_message(msg);}

#[no_mangle]
pub extern fn on_put_field(obj_id: Long, field: String, value_id: Long) {
    let mut payload = access::PutField::new();
    payload.id = obj_id;
    payload.field = get_string!(field);
    payload.value_id = value_id;

    let mut msg = Variant::new();
    msg.set_put_field(payload);
    msg.set_field_type(MessageType::PUTFIELD);
    log_message(msg);
}

#[no_mangle]
pub extern fn on_store(value_id: Long, index: Int) {
    let mut payload = access::Store::new();
    payload.value_id = value_id;
    payload.index = index;

    let mut msg = Variant::new();
    msg.set_store(payload);
    msg.set_field_type(MessageType::STORE);
    log_message(msg);
}

#[no_mangle]
pub extern fn on_load(index: Int) {
    let mut payload = access::Load::new();
    payload.index = index;

    let mut msg = Variant::new();
    msg.set_load(payload);
    msg.set_field_type(MessageType::LOAD);
    log_message(msg);
}

#[no_mangle]
pub extern fn on_alloc(obj_id: Long, class: String) {
    let mut payload = allocations::Allocation::new();
    payload.id = obj_id;
    payload.field_type = get_string!(class);

    let mut msg = Variant::new();
    msg.set_alloc(payload);
    msg.set_field_type(MessageType::ALLOC);
    log_message(msg);
}

#[no_mangle]
pub extern fn on_dealloc(obj_id: Long) {
    let mut payload = allocations::Deallocation::new();
    payload.id = obj_id;

    let mut msg = Variant::new();
    msg.set_dealloc(payload);
    msg.set_field_type(MessageType::DEALLOC);
    log_message(msg);
}

#[no_mangle]
pub extern fn on_define_class(buffer: *const u8, len: Int) {
    use proto::definitions::*;

    null_check!(buffer);
    let array = unsafe{ std::slice::from_raw_parts(buffer, len as usize) };

    let def: ClassDefinition = match protobuf::core::parse_from_bytes(array) {
        Err(e) => {
            eprintln!("failed to deserialize class definition: {:?}", e);
            return;
        }
        Ok(def) => def
    };

    let mut msg = Variant::new();
    msg.set_class_def(def);
    msg.set_field_type(MessageType::CLASS_DEF);
    log_message(msg)
}

fn log_message(message: Variant) {
    eprintln!("logging {:?}", message);
}