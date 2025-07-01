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