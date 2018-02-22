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

pub struct DeclaredField {
    name: CString,
    clazz: CString,
}

pub struct HeapExplorer<'a> {
    fields: &'a Vec<DeclaredField>,
    tags: HashSet<i64>,
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
pub extern fn heap_explore_init<'a>(ptr: *mut FieldsMap, cls: *const c_char, tag: i64) -> *const HeapExplorer<'a> {
    let f = unsafe { &mut *ptr };
    let cls_str = unsafe { CStr::from_ptr(cls) };
    match f.fields.get(cls_str) {
        None => ptr::null(),
        Some(v) => Box::into_raw(Box::new(HeapExplorer {
            fields: v,
            tags: {
                let mut tags = HashSet::new();
                if tag != 0 {
                    println!("exploriting {}", tag);
                    tags.insert(tag);
                }
                tags
            },
        }))
    }
}

#[no_mangle]
pub extern fn heap_explore_should_explore<'a>(explorer: *mut HeapExplorer<'a>, tag: i64) -> i8 {
    let e = unsafe { &mut *explorer };
    if (e.tags.contains(&tag)) {
        1
    } else {
        0
    }
}

#[no_mangle]
pub extern fn heap_explore_add<'a>(explorer: *mut HeapExplorer<'a>, tag: i64) {
    let e = unsafe { &mut *explorer };
    e.tags.insert(tag);
}

#[no_mangle]
pub extern fn heap_explore_get_field<'a>(explorer: *mut HeapExplorer<'a>, tag: i64, index: usize, name: *mut *const c_char, clazz: *mut *const c_char) {
    let e = unsafe { &mut *explorer };

    if let Some(field) = e.fields.get(index) {
        if field.clazz.to_bytes().len() > 1 {
            e.tags.insert(tag);
            unsafe {
                *name = field.name.as_ptr();
                *clazz = field.clazz.as_ptr();
            }
        }
    }
}

#[no_mangle]
pub extern fn heap_explore_finish<'a>(explorer: *mut HeapExplorer<'a>) {
    unsafe { Box::from_raw(explorer); }
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
        name: copy_str(name),
        clazz: copy_str(clazz),
    })
}

#[no_mangle]
pub extern fn fields_discovery_finish(discover: *mut FieldDiscovery, map: *mut FieldsMap, clazz: *const c_char) {
    let mut d = unsafe { Box::from_raw(discover) };
    let f = unsafe { &mut *map };
    let fields = mem::replace(&mut d.fields, Vec::new());
    println!("fields for {:?}:", copy_str(clazz));
    for (i, f) in fields.iter().enumerate() {
        println!("{}: {:?} {:?}", i, f.clazz, f.name);
    }
    ::std::io::stdout().flush().expect("flush");
    f.fields.insert(copy_str(clazz), fields);
}

/*
impl Drop for DeclaredField {
    fn drop(&mut self) {
        unsafe {
            CString::from_raw(self.name);
            CString::from_raw(self.clazz);
        }
    }
}
*/
