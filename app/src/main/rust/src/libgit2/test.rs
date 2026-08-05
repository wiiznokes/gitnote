use std::{collections::HashMap, time::Instant};

use super::*;

#[test]
#[ignore = "local repo"]
fn timestamp() {
    open_repo("../../../../../repo_test").unwrap();

    let mut timestamps = HashMap::new();

    let now = Instant::now();

    get_timestamps(|path, time| {
        timestamps.insert(path.to_string(), time);
        Ok(())
    })
    .unwrap();

    let elapsed = now.elapsed();

    println!("{elapsed:?}");
}

// cargo test timestamp2 --release -- --nocapture --ignored
#[test]
#[ignore = "local repo"]
fn timestamp2() {
    open_repo("../../../../../note-pv").unwrap();

    let mut timestamps = HashMap::new();
    let now = Instant::now();

    get_timestamps(|path, time| {
        timestamps.insert(path.to_string(), time);
        Ok(())
    })
    .unwrap();

    let mut res = timestamps.into_iter().collect::<Vec<_>>();

    res.sort_by(|a, b| a.1.cmp(&b.1));

    dbg!(&res);

    let elapsed = now.elapsed();

    println!("{elapsed:?}");
}
