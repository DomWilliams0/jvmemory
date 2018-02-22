use std::{ptr, mem};
use std::collections::{HashSet, HashMap};
use libc::*;
use std::ffi::{CStr, CString};
use std::io::Write;

pub struct FieldsMap {
    fields: HashMap<CString, Vec<DeclaredField>>,
}

pub struct FieldDiscovery {
    discovered: HashSet<CString>,
    fields: Vec<DeclaredField>,
}

#[repr(C)]
pub struct DeclaredField {
    name: *mut c_char,
    clazz: *mut c_char,
}

#[no_mangle]
pub extern fn fields_init() -> *const FieldsMap {
    Box::into_raw(Box::new(FieldsMap {
        fields: HashMap::new(),
    }))
}

#[no_mangle]
pub extern fn fields_free(ptr: *mut FieldsMap) {
    unsafe { Box::from_raw(ptr); }
}

#[no_mangle]
pub extern fn fields_get(ptr: *mut FieldsMap, cls: *const c_char, count: *mut usize) -> *const DeclaredField {
    let f = unsafe { &mut *ptr };
    let cls_str = unsafe { CStr::from_ptr(cls) };
    let (vec, n) = match f.fields.get(cls_str) {
        None => (ptr::null(), 0),
        Some(v) => (v.as_ptr(), v.len()),
    };

    unsafe { *count = n };
    vec
}

#[no_mangle]
pub extern fn fields_discovery_init() -> *const FieldDiscovery {
    Box::into_raw(Box::new(FieldDiscovery {
        discovered: HashSet::new(),
        fields: Vec::new(),
    }))
}

#[no_mangle]
pub extern fn fields_discovery_check(discover: *mut FieldDiscovery, cls: *const c_char) -> bool {
    let d = unsafe { &mut *discover };
    let s = unsafe { CStr::from_ptr(cls) };
    let contains = d.discovered.contains(s);
    if !contains {
        d.discovered.insert(copy_str(cls));
    }
    contains
}

fn copy_str(ptr: *const c_char) -> CString {
    unsafe { CStr::from_ptr(ptr) }.to_owned()
}

#[no_mangle]
pub extern fn fields_discovery_register(discover: *mut FieldDiscovery, name: *const c_char, clazz: *const c_char) {
    let d = unsafe { &mut *discover };
    d.fields.push(DeclaredField {
        name: copy_str(name).into_raw(),
        clazz: copy_str(clazz).into_raw(),
    })
}

#[no_mangle]
pub extern fn fields_discovery_finish(discover: *mut FieldDiscovery, map: *mut FieldsMap, clazz: *const c_char) {
    let mut d = unsafe { Box::from_raw(discover) };
    let f = unsafe { &mut *map };
    let fields = mem::replace(&mut d.fields, Vec::new());
    println!("fields for {:?}:", copy_str(clazz));
    for (i, f) in fields.iter().enumerate() {
        println!("{}: {:?} {:?}", i, unsafe { CStr::from_ptr(f.clazz) }, unsafe { CStr::from_ptr(f.name) });
    }
    ::std::io::stdout().flush().expect("flush");
    f.fields.insert(copy_str(clazz), fields);
}

impl Drop for DeclaredField {
    fn drop(&mut self) {
        unsafe {
            CString::from_raw(self.name);
            CString::from_raw(self.clazz);
        }
    }
}
