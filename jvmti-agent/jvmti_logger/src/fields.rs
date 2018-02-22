use std::mem;
use std::collections::{HashMap, HashSet};
use libc::*;
use std::ffi::{CStr, CString};
use std::io::Write;
use diff;

type ObjectId = i64;
type Index = i32;

#[derive(Default)]
pub struct ExploreCache {
    last_refs: HashMap<ObjectId, Vec<Access>>,
    // fields: HashMap<CString, Vec<DeclaredField>>,
}

pub struct FieldDiscovery {
    discovered: HashSet<CString>,
    fields: Vec<DeclaredField>,
}

pub struct DeclaredField {
    name: CString,
    clazz: CString,
}

#[derive(Eq, PartialEq, Debug)]
enum Access {
    Field {
        from: ObjectId,
        to: ObjectId,
        index: Index,
    },
    Array {
        from: ObjectId,
        to: ObjectId,
        index: Index,
    },
}

#[derive(Default)]
pub struct HeapExplorer {
    src_obj: ObjectId,
    tags: HashSet<ObjectId>,
    accesses: Vec<Access>,
}

impl HeapExplorer {
    fn add_access(&mut self, access: Access) {
        self.tags.insert(match access {
            Access::Field { to, .. } => to,
            Access::Array { to, .. } => to,
        });
        self.accesses.push(access);
    }
}

#[no_mangle]
pub extern "C" fn explore_cache_init() -> *const ExploreCache {
    Box::into_raw(Box::new(ExploreCache::default()))
}

#[no_mangle]
pub extern "C" fn explore_cache_free(cache: *mut ExploreCache) {
    unsafe {
        Box::from_raw(cache);
    }
}

#[no_mangle]
pub extern "C" fn heap_explore_init(tag: ObjectId) -> *const HeapExplorer {
    let mut e = Box::new(HeapExplorer::default());
    if tag != 0 {
        e.tags.insert(tag);
        e.src_obj = tag;
    }

    println!("exploring {}", tag);
    Box::into_raw(e)
}

#[no_mangle]
pub extern "C" fn heap_explore_should_explore(explorer: *mut HeapExplorer, tag: ObjectId) -> bool {
    let e = unsafe { &mut *explorer };
    e.tags.contains(&tag)
}

#[no_mangle]
pub extern "C" fn heap_explore_visit_array_element(
    explorer: *mut HeapExplorer,
    referrer: ObjectId,
    tag: ObjectId,
    index: Index,
) {
    let e = unsafe { &mut *explorer };
    e.add_access(Access::Array {
        from: referrer,
        to: tag,
        index,
    });
}

#[no_mangle]
pub extern "C" fn heap_explore_visit_field(
    explorer: *mut HeapExplorer,
    referrer: ObjectId,
    tag: ObjectId,
    index: Index,
) {
    let e = unsafe { &mut *explorer };
    e.add_access(Access::Field {
        from: referrer,
        to: tag,
        index,
    });
}

fn find_differences(explorer: &HeapExplorer, cache: &ExploreCache) -> bool {
    let blank = Vec::new();
    let last = cache
        .last_refs
        .get(&explorer.src_obj)
        .unwrap_or(&blank);

    let diffs = diff::slice(last, &explorer.accesses);
    for diff in &diffs {
        match diff {
            &diff::Result::Both(x, _) => println!(" {:?}", x),
            &diff::Result::Left(x) => println!("-{:?}", x),
            &diff::Result::Right(x) => println!("+{:?}", x),
        }
    }

    diffs.iter().all(|x| match x {
        &diff::Result::Both(_, _) => true,
        _ => false,
    })
}

#[no_mangle]
pub extern "C" fn heap_explore_finish(explorer: *mut HeapExplorer, cache: *mut ExploreCache) {
    let e = unsafe { Box::from_raw(explorer) };
    let c = unsafe { &mut *cache };

    if find_differences(&e, &c) {
        println!("all the same, no changes!");
    }

    c.last_refs.insert(e.src_obj, e.accesses);
    println!("=====");
}

#[no_mangle]
pub extern "C" fn fields_discovery_init() -> *const FieldDiscovery {
    Box::into_raw(Box::new(FieldDiscovery {
        discovered: HashSet::new(),
        fields: Vec::new(),
    }))
}

#[no_mangle]
pub extern "C" fn fields_discovery_check(
    discover: *mut FieldDiscovery,
    cls: *const c_char,
) -> bool {
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
pub extern "C" fn fields_discovery_register(
    discover: *mut FieldDiscovery,
    name: *const c_char,
    clazz: *const c_char,
) {
    let d = unsafe { &mut *discover };
    d.fields.push(DeclaredField {
        name: copy_str(name),
        clazz: copy_str(clazz),
    })
}

#[no_mangle]
pub extern "C" fn fields_discovery_finish(
    discover: *mut FieldDiscovery,
    map: *mut ExploreCache,
    clazz: *const c_char,
) {
    let mut d = unsafe { Box::from_raw(discover) };
    // let f = unsafe { &mut *map };
    let fields = mem::replace(&mut d.fields, Vec::new());
    println!("fields for {:?}:", copy_str(clazz));
    for (i, f) in fields.iter().enumerate() {
        println!("{}: {:?} {:?}", i, f.clazz, f.name);
    }
    ::std::io::stdout().flush().expect("flush");
    // TODO f.fields.insert(copy_str(clazz), fields);
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
