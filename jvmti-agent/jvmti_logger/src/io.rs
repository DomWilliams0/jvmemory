use std::{io, fs, ptr, thread, mem};
use proto::message::Variant;
use protobuf::Message;
use libc::*;
use std::ffi::CStr;
use std::sync::mpsc;

const BUFFER_SIZE: usize = 1024;

#[repr(C)]
pub struct Logger {
    buffer_thread: thread::JoinHandle<()>,
    drainpipe: mpsc::Sender<Variant>,
    // TODO it seems that using a BufWriter here causes a panic during allocation event logging
    //writer: fs::File
}

fn spawn_buffer_thread(path: &str, recv: mpsc::Receiver<Variant>) -> io::Result<thread::JoinHandle<()>> {
    fn flush_buffer<W: io::Write>(buffer: &mut Vec<Variant>, out: &mut W) {
        for msg in buffer.iter() {
            if let Err(e) = msg.write_length_delimited_to_writer(out) {
                eprintln!("failed to write to log file: {:?}", e);
            }
        }
        buffer.clear();
    }


    let out_file = fs::File::create(path)?;

    Ok(thread::spawn(|| {
        let pipe = recv;
        let mut file = out_file;
        let mut buffer = Vec::<Variant>::with_capacity(BUFFER_SIZE);
        loop {
            let msg = match pipe.recv() {
                Ok(msg) => msg,
                Err(_) => break,
            };

            buffer.push(msg);

            if buffer.len() == BUFFER_SIZE {
                flush_buffer(&mut buffer, &mut file);
            }

        }
        flush_buffer(&mut buffer, &mut file);
    }))
}

impl Logger {
    fn new(path: &str) -> io::Result<Self> {
        let (send, recv) = mpsc::channel();
        Ok(Self {
            drainpipe: send,
            buffer_thread: spawn_buffer_thread(path, recv)?,
        })
    }

    fn safe_log(&mut self, message: Variant) -> Result<(), mpsc::SendError<Variant>> {
         self.drainpipe.send(message)?;
         Ok(())
    }

    fn log(&mut self, message: Variant) {
        if let Err(e) = self.safe_log(message) {
            eprintln!("failed to write to log: {:?}", e);
        }
    }
}

#[no_mangle]
pub extern fn logger_init(out_file: *const c_char) -> *const Logger {
    let path = get_string_ref!(out_file, ptr::null());
    match Logger::new(&path) {
        Err(e) => {
            eprintln!("logger_init failed: {:?}", e);
            ptr::null()
        }
        Ok(l) => Box::into_raw(Box::new(l)),
    }
}

#[no_mangle]
pub extern fn logger_free(logger_ptr: *mut Logger) {
    if !logger_ptr.is_null() {
        let mut logger = unsafe { Box::from_raw(logger_ptr) };

        let (dummy, _) = mpsc::channel();
        let sender = mem::replace(&mut logger.drainpipe, dummy);
        drop(sender);
        logger.buffer_thread.join().expect("waiting on buffer thread");
    }
}

pub fn log_message(logger: *mut Logger, message: Variant) {
    if !logger.is_null() {
        let inst = unsafe { &mut *logger };
        inst.log(message);
    };
}
