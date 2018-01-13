macro_rules! null_check {
    ($p:ident) => (null_check!($p, ()));
    ($p:ident, $ret:expr) => (if $p.is_null() {
        eprintln!("jni passed null to logger as '{}'", stringify!($p));
        return $ret;
    })
}

macro_rules! get_string_ref {
    ($p:ident) => (get_string_ref!($p, ()));
    ($p:ident, $ret:expr) => {{
        null_check!($p, $ret);
        match unsafe {CStr::from_ptr($p)}.to_str() {
            Err(_) => {
                eprintln!("jni passed bad String as '{}'", stringify!($p));
                return $ret;
            },
            Ok(cstr) => cstr,
        }
    }}
}

macro_rules! get_string {
    ($p:ident) => (get_string!($p, ()));
    ($p:ident, $ret:expr) => (get_string_ref!($p, $ret).to_owned())
}
