extern crate libc;
extern crate protobuf;

mod proto;

use libc::*;
use std::ffi::CStr;

type Long = isize;
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
            Ok(cstr) => cstr,
        }
    }}
}

#[no_mangle]
pub extern fn on_enter_method(class: String, method: String) {
    println!(">>> {}:{}", get_string!(class), get_string!(method));
}

#[no_mangle]
pub extern fn on_exit_method() {
    println!("<<<");
}

#[no_mangle]
pub extern fn on_get_field(obj_id: Long, field: String) {
    println!("getfield {} on {}", get_string!(field), obj_id);
}

#[no_mangle]
pub extern fn on_put_field(obj_id: Long, field: String, value_id: Long) {
    println!("putfield {} on {} with value {}", get_string!(field), obj_id, value_id);
}

#[no_mangle]
pub extern fn on_store(value_id: Long, index: Int) {
    println!("store {} in index {}", value_id, index);
}

#[no_mangle]
pub extern fn on_load(index: Int) {
    println!("load index {}",index);
}

#[no_mangle]
pub extern fn on_alloc(obj_id: Long, class: String) {
    println!("alloc {} of type {}", obj_id, get_string!(class));
}

#[no_mangle]
pub extern fn on_dealloc(obj_id: Long) {
    println!("dealloc {}", obj_id);
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

    println!("class defIntion: {:?}", def);
}
