use std::sync::RwLock;
use std::sync::atomic::{AtomicIsize, Ordering};

#[repr(C)]
pub struct Concurrent {
    prog_running: RwLock<bool>,
    next_tag: AtomicIsize,
}

#[no_mangle]
pub extern "C" fn concurrent_init() -> *const Concurrent {
    Box::into_raw(Box::new(Concurrent {
        prog_running: RwLock::new(false),
        next_tag: AtomicIsize::new(1),
    }))
}

#[no_mangle]
pub extern "C" fn concurrent_free(c: *mut Concurrent) {
    unsafe {
        Box::from_raw(c);
    }
}

#[no_mangle]
pub extern "C" fn concurrent_is_program_running(c: *mut Concurrent) -> bool {
    let c = unsafe { &*c };

    *c.prog_running.read().expect("prog_running.read()")
}

#[no_mangle]
pub extern "C" fn concurrent_set_program_running(c: *mut Concurrent, running: bool) {
    let c = unsafe { &*c };

    *c.prog_running.write().expect("prog_running.write()") = running;
}

#[no_mangle]
pub extern "C" fn concurrent_get_next_tag(c: *mut Concurrent) -> isize {
    let c = unsafe { &*c };

    c.next_tag.fetch_add(1, Ordering::SeqCst)
}
