extern crate libc;

use libc::*;
use std::ffi::CStr;

type long = isize;
type int = i32;
type string = *const c_char;

#[no_mangle]
pub extern fn on_enter_method(class: string, method: string) {
    let class_str = unsafe {CStr::from_ptr(class)}.to_str().unwrap();
    let method_str = unsafe {CStr::from_ptr(method)}.to_str().unwrap();
    println!(">>> {:?}:{:?}", class_str, method_str);
}

#[no_mangle]
pub extern fn on_exit_method() {
    println!("<<<");
}

#[no_mangle]
pub extern fn on_get_field(obj_id: long, field: string) {
    let field_str = unsafe {CStr::from_ptr(field)}.to_str().unwrap();
    println!("getfield {:?} on {}", field_str, obj_id);
}

#[no_mangle]
pub extern fn on_put_field(obj_id: long, field: string, value_id: long) {
    let field_str = unsafe {CStr::from_ptr(field)}.to_str().unwrap();
    println!("putfield {:?} on {} with value {}", field_str, obj_id, value_id);
}

#[no_mangle]
pub extern fn on_store(value_id: long, index: int) {
    println!("store {} in index {}", value_id, index);
}

#[no_mangle]
pub extern fn on_load(index: int) {
    println!("load index {}",index);
}

#[no_mangle]
pub extern fn on_alloc(obj_id: long, class: string) {
    let class_str = unsafe {CStr::from_ptr(class)}.to_str().unwrap();
    println!("alloc {} of type {}", obj_id, class_str);

}

#[no_mangle]
pub extern fn on_dealloc(obj_id: long) {
    println!("dealloc {}", obj_id);
}
