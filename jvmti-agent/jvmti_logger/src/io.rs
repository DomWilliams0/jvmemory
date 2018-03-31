use std::{fs, io, ptr, thread};
use proto::message::Variant;
use protobuf::Message;
use libc::*;
use std::ffi::CStr;
use std::sync::Arc;
use spin;
use crossbeam_deque::*;

const BUFFER_SIZE: usize = 1024 * 1024;

#[repr(C)]
pub struct Logger {
    running: Arc<spin::Mutex<bool>>,
    dump_thread: thread::JoinHandle<()>,

    // TODO instead of every thread sharing a single logger instance, give them thread local
    //      buffers that only block to flush their buffers? profile first!
    drainpipe: Deque<Variant>,
}

fn spawn_dump_thread (
    path: &str,
    stealer: Stealer<Variant>,
    running: Arc<spin::Mutex<bool>>,
) -> io::Result<thread::JoinHandle<()>> {

    // TODO it seems that using a BufWriter here causes a panic during allocation event logging
    let out_file = io::BufWriter::new(fs::File::create(path)?);
    Ok(thread::spawn(move || {
        let mut out = out_file;
        loop {
            if let Steal::Data(msg) = stealer.steal() {
                if let Err(e) = msg.write_length_delimited_to_writer(&mut out) {
                    eprintln!("failed to write to log file: {:?}", e);
                }
            }
            else if !*running.lock() {
                println!("exiting dump thread");
                break;
            }
        }
    }))
}

impl Logger {
    fn new(path: &str) -> io::Result<Self> {
        let deque = Deque::with_min_capacity(BUFFER_SIZE);
        let stealer = deque.stealer();
        let running = Arc::new(spin::Mutex::new(true));

        Ok(Self {
            drainpipe: deque,
            dump_thread: spawn_dump_thread(path, stealer, running.clone())?,
            running,
        })
    }

    fn log(&mut self, message: Variant) {
        self.drainpipe.push(message);
    }
}

#[no_mangle]
pub extern "C" fn logger_init(out_file: *const c_char) -> *const Logger {
    let path = get_string_ref!(out_file, ptr::null());
    match Logger::new(path) {
        Err(e) => {
            eprintln!("logger_init failed: {:?}", e);
            ptr::null()
        }
        Ok(l) => Box::into_raw(Box::new(l)),
    }
}

#[no_mangle]
pub extern "C" fn logger_free(logger_ptr: *mut Logger) {
    if !logger_ptr.is_null() {
        let logger = unsafe { Box::from_raw(logger_ptr) };

        *logger.running.lock() = false;
        println!("waiting for dump thread to finish");
        logger.dump_thread.join().expect("waiting on dump thread");
    }
}

pub fn log_message(logger: *mut Logger, message: Variant) {
    if !logger.is_null() {
        let inst = unsafe { &mut *logger };
        inst.log(message);
    };
}
