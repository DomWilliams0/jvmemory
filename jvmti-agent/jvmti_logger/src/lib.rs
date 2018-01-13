extern crate libc;

use libc::*;
use std::ffi::CStr;

#[no_mangle]
pub extern fn on_enter_method(class: *const c_char, method: *const c_char) {
    let class_str = unsafe {CStr::from_ptr(class)}.to_str();
    let method_str = unsafe {CStr::from_ptr(method)}.to_str();
    println!("entering method in rust: {:?}:{:?}", class_str, method_str)
}
