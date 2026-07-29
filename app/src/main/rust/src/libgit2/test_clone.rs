use std::fs;

use crate::{callback::DummyProgressCB, libgit2::clone_repo};

fn setup_logs() {
    env_logger::init();
}

#[test]
#[ignore = "local testing"]
fn basic() {
    setup_logs();

    let repo_path = "test_clone/repo1";
    let remote_url = "ssh://name@9.9.9.9:111/name/name.git";

    let cred = Some(crate::Cred::Ssh {
        public_key: String::from(""),
        private_key: String::from(""),
        passphrase: None,
    });

    let _ = fs::remove_dir_all(repo_path);

    clone_repo(repo_path, remote_url, cred, DummyProgressCB).unwrap();
}
