use std::{io, fs, ptr};
use proto::message::Variant;
use protobuf::Message;
use libc::*;
use std::ffi::CStr;

#[repr(C)]
pub struct Logger {
    // TODO it seems that using a BufWriter here causes a panic during allocation event logging
    writer: fs::File
}

impl Logger {
    fn new(path: &str) -> io::Result<Self> {
        Ok(Self {
            writer: fs::File::create(path)?
        })
    }

    fn log(&mut self, message: Variant) {
        if let Err(e) = message.write_length_delimited_to_writer(&mut self.writer) {
            eprintln!("failed to write to log: {:?}", e);
        }
    }
}

#[no_mangle]
pub extern fn logger_init(out_file: *const c_char) -> *const Logger {
    let path = get_string!(out_file, ptr::null());
    match Logger::new(&path) {
        Err(e) => {
            eprintln!("logger_init failed: {:?}", e);
            ptr::null()
        }
        Ok(l) => Box::into_raw(Box::new(l)),
    }
}

#[no_mangle]
pub extern fn logger_free(logger: *mut Logger) {
    if !logger.is_null() {
        unsafe { Box::from_raw(logger); }
    };
}

pub fn log_message(logger: *mut Logger, message: Variant) {
    if !logger.is_null() {
        let inst = unsafe { &mut *logger };
        inst.log(message);
    };
}
