use std::fs;

use crate::{callback::DummyProgressCB, libgit2::clone_repo};

fn setup_logs() {
    env_logger::init();
}

// RUST_LOG=debug cargo test test_clone::basic -- --nocapture --ignored
#[test]
#[ignore = "local testing"]
fn basic() {
    setup_logs();

    let repo_path = "test_clone/repo1";
    let remote_url = "https://codeberg.org/wiiznokes/test.git";

    let cred = Some(crate::Cred::UserPassPlainText {
        username: String::from(""),
        password: String::from(""),
    });

    let _ = fs::remove_dir_all(repo_path);

    git2::trace_set(git2::TraceLevel::Trace, |_level, msg| {
        let msg = String::from_utf8_lossy(msg);
        debug!("{msg}");
    })
    .unwrap();

    clone_repo(repo_path, remote_url, cred, DummyProgressCB).unwrap();
}
