use std::backtrace::Backtrace;
use std::panic;
use std::sync::{LazyLock, Mutex};

static LAST_BACKTRACE: LazyLock<Mutex<Option<Backtrace>>> = LazyLock::new(|| Mutex::new(None));

pub fn install_panic_hook() {
    panic::set_hook(Box::new(|info| {
        let bt = Backtrace::force_capture();
        LAST_BACKTRACE.lock().unwrap().replace(bt);

        if let Some(s) = info.payload().downcast_ref::<&str>() {
            error!("panic occurred: {:?}", s);
        } else {
            error!("panic occurred: unknown");
        }

        if let Some(loc) = info.location() {
            error!("panic location: {}:{}", loc.file(), loc.line());
        }
    }));
}

macro_rules! unwrap_or_log {
    ($res:expr, $name:expr, $ret:expr) => {
        match $res {
            Ok(v) => v,
            Err(e) => {
                error!("{}: {}", $name, e);
                return $ret;
            }
        }
    };

    ($res:expr, $name:expr) => {
        match $res {
            Ok(v) => v,
            Err(e) => {
                error!("{}: {}", $name, e);
                return e.raw_code();
            }
        }
    };
}
