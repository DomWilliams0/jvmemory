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
    fields: HashMap<CString, Vec<DeclaredField>>,
}

#[derive(Default)]
pub struct FieldDiscovery {
    discovered: HashSet<CString>,
    fields: Vec<DeclaredField>,
}

pub struct DeclaredField {
    name: CString,
    clazz: CString,
}

#[derive(Debug, Copy, Clone)]
enum Change {
    Add(Access),
    Del(Access),
}
#[derive(Eq, PartialEq, Debug, Copy, Clone)]
struct AccessData {
    from: ObjectId,
    to: ObjectId,
    index: Index,
}

#[derive(Eq, PartialEq, Debug, Copy, Clone)]
enum Access {
    Field(AccessData),
    Array(AccessData),
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
            Access::Field(data) | Access::Array(data) => data.to,
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
    e.add_access(Access::Array(AccessData {
        from: referrer,
        to: tag,
        index,
    }));
}

#[no_mangle]
pub extern "C" fn heap_explore_visit_field(
    explorer: *mut HeapExplorer,
    referrer: ObjectId,
    tag: ObjectId,
    index: Index,
) {
    let e = unsafe { &mut *explorer };
    e.add_access(Access::Field(AccessData {
        from: referrer,
        to: tag,
        index,
    }));
}

extern "C" {
    fn follow_references(explorer: *mut HeapExplorer, tag: ObjectId);
    fn discover_fields_if_necessary(
        jnienv: *const c_void,
        len: i32,
        tags: *const ObjectId,
        clazzes: *mut *mut c_char,
    );

// TODO can all of these be in the same extern block?
}

#[no_mangle]
pub extern "C" fn emit_heap_differences(
    cache: *mut ExploreCache,
    jnienv: *const c_void,
    tag: ObjectId,
) {
    let mut explorer = {
        let mut h = HeapExplorer::default();
        h.src_obj = tag;
        h.tags.insert(tag);
        h
    };
    let explorer_p = &mut explorer as *mut HeapExplorer;
    let mut cache = unsafe { &mut *cache };

    unsafe {
        follow_references(explorer_p, tag);
    }

    let changes = find_differences(&explorer, &mut cache);

    if changes.is_empty() {
        println!("all the same, no changes!");
    } else {
        // collect all tag ids
        let to_discover = {
            let mut tags = Vec::with_capacity(changes.len() * 2);
            changes.iter().for_each(|d| match *d {
                Change::Add(Access::Array(data))
                | Change::Add(Access::Field(data))
                | Change::Del(Access::Array(data))
                | Change::Del(Access::Field(data)) => {
                    let AccessData { from, to, .. } = data;
                    tags.push(from);
                    tags.push(to);
                }
            });
            tags.sort_unstable();
            tags.dedup();
            tags
        };

        // collect class names
        let mut clazzes = Vec::<*mut c_char>::with_capacity(to_discover.len());
        unsafe {
            discover_fields_if_necessary(
                jnienv,
                to_discover.len() as i32,
                to_discover.as_ptr(),
                clazzes.as_mut_ptr(),
            );
            clazzes.set_len(to_discover.len());
        }

        // generate map of tag->clazz for easy access
        let clazz_map = {
            let mut map = HashMap::with_capacity(clazzes.len());
            for (tag, clazz) in to_discover.iter().zip(clazzes.iter()) {
                map.insert(*tag, unsafe { CStr::from_ptr(*clazz) }); // TODO ok?
            }
            map
        };

        generate_diff_events(cache, changes, &clazz_map);

        // TODO deallocate clazzes

        // now we can generate events with class names
        // TODO debug print for now
    }

    cache.last_refs.insert(explorer.src_obj, explorer.accesses);
}

fn find_differences(explorer: &HeapExplorer, cache: &mut ExploreCache) -> Vec<Change> {
    let last = cache
        .last_refs
        .remove(&explorer.src_obj)
        .unwrap_or_default();

    diff::slice(&last, &explorer.accesses)
        .into_iter()
        .filter_map(|d| match d {
            diff::Result::Left(x) => Some(Change::Del(*x)),
            diff::Result::Right(x) => Some(Change::Add(*x)),
            _ => None,
        })
        .collect()
}

fn generate_diff_events(
    cache: &ExploreCache,
    changes: Vec<Change>,
    clazz_map: &HashMap<ObjectId, &CStr>,
) {
    fn debug_print_field(
        cache: &ExploreCache,
        clazz_map: &HashMap<ObjectId, &CStr>,
        field: &AccessData,
        del: bool,
    ) {
        let f = {
            let cls = clazz_map.get(&field.from).expect("clazz map is incomplete");
            let fields = cache.fields.get(*cls).expect("dicovered field is wrong");
            fields.get(field.index as usize).expect("bad field")
        };
        let val = if del { 0 } else { field.to };
        println!("{}.{:?} ({:?}) = {}", field.from, f.name, f.clazz, val);
    }

    fn debug_print_array(array: &AccessData, del: bool) {
        let val = if del { 0 } else { array.to };
        println!("{}[{}] = {}", array.from, array.index, val);
    }
    for c in changes {
        match c {
            Change::Add(Access::Field(data)) => debug_print_field(cache, clazz_map, &data, false),
            Change::Del(Access::Field(data)) => debug_print_field(cache, clazz_map, &data, true),
            Change::Add(Access::Array(data)) => debug_print_array(&data, false),
            Change::Del(Access::Array(data)) => debug_print_array(&data, true),
        }
    }

    println!("====");
}

#[no_mangle]
pub extern "C" fn fields_discovery_init() -> *const FieldDiscovery {
    Box::into_raw(Box::new(FieldDiscovery::default()))
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
        d.discovered.insert(copy_str(cls)); // only calls copy_str if necessary
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
    cache: *mut ExploreCache,
    clazz: *const c_char,
) {
    let mut d = unsafe { Box::from_raw(discover) };
    let c = unsafe { &mut *cache };
    let fields = mem::replace(&mut d.fields, Vec::new());
    println!("fields for {:?}:", copy_str(clazz));
    for (i, f) in fields.iter().enumerate() {
        println!("{}: {:?} {:?}", i, f.clazz, f.name);
    }
    ::std::io::stdout().flush().expect("flush");
    c.fields.insert(copy_str(clazz), fields);
}

#[no_mangle]
pub extern "C" fn fields_discovery_has_discovered(
    cache: *const ExploreCache,
    clazz: *const c_char,
) -> bool {
    let c = unsafe { &*cache };
    c.fields.contains_key(unsafe { CStr::from_ptr(clazz) })
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
