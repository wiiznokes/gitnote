#![allow(non_snake_case)]

use std::sync::{LazyLock, Mutex};

use git2::{
    CertificateCheckStatus, Cred, FetchOptions, IndexAddOption, Progress, PushOptions,
    RemoteCallbacks, Repository, Signature, StatusOptions,
};
use jni::JNIEnv;
use jni::objects::{JClass, JObject, JString};
use jni::sys::{jint, jstring};

use crate::utils::install_panic_hook;

#[macro_use]
extern crate log;
#[macro_use]
mod utils;
mod merge;

static REPO: LazyLock<Mutex<Option<Repository>>> = LazyLock::new(|| Mutex::new(None));

#[unsafe(no_mangle)]
pub extern "C" fn Java_io_github_wiiznokes_gitnote_manager_GitManagerKt_initLib(
    _env: JNIEnv,
    _class: JClass,
) -> jint {
    install_panic_hook();

    android_logger::init_once(
        android_logger::Config::default()
            .with_max_level(log::LevelFilter::Trace)
            .with_tag("rust")
            .with_filter(
                android_logger::FilterBuilder::new()
                    .parse("warn,git_wrapper=debug")
                    .build(),
            ),
    );

    0
}

#[unsafe(no_mangle)]
pub extern "C" fn Java_io_github_wiiznokes_gitnote_manager_GitManagerKt_createRepoLib<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    repoPath: JString<'local>,
) -> jint {
    let repo_path: String = env
        .get_string(&repoPath)
        .expect("Couldn't get java string!")
        .into();

    let repo = unwrap_or_log!(Repository::init(repo_path), "Repository::init");

    REPO.lock().unwrap().replace(repo);

    0
}
#[unsafe(no_mangle)]
pub extern "C" fn Java_io_github_wiiznokes_gitnote_manager_GitManagerKt_openRepoLib<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    repoPath: JString<'local>,
) -> jint {
    let repo_path: String = env
        .get_string(&repoPath)
        .expect("Couldn't get java string!")
        .into();

    let repo = unwrap_or_log!(Repository::open(repo_path), "Repository::open");

    REPO.lock().unwrap().replace(repo);

    0
}
#[unsafe(no_mangle)]
pub extern "C" fn Java_io_github_wiiznokes_gitnote_manager_GitManagerKt_cloneRepoLib<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    repoPath: JString<'local>,
    remoteUrl: JString<'local>,
    username: JString<'local>,
    password: JString<'local>,
    _progressCallback: JObject<'local>,
) -> jint {
    let repo_path: String = env.get_string(&repoPath).unwrap().into();
    let remote_url: String = env.get_string(&remoteUrl).unwrap().into();

    let mut callbacks = RemoteCallbacks::new();

    callbacks.certificate_check(|_cert, _| Ok(CertificateCheckStatus::CertificateOk));

    if !username.is_null() && !password.is_null() {
        let username: String = env.get_string(&username).unwrap().into();
        let password: String = env.get_string(&password).unwrap().into();

        callbacks.credentials(move |_url, _username_from_url, _allowed_types| {
            Cred::userpass_plaintext(&username, &password)
        });
    }

    callbacks.transfer_progress(|stats: Progress| {
        // TODO: use `progress_callback` to send progress info back to Java
        info!(
            "received {}/{} objects",
            stats.received_objects(),
            stats.total_objects()
        );
        true
    });

    let mut fetch_options = FetchOptions::new();
    fetch_options.remote_callbacks(callbacks);

    let mut builder = git2::build::RepoBuilder::new();

    let repo_result = builder
        //.with_checkout(checkout) add this when using progress
        .fetch_options(fetch_options)
        .clone(&remote_url, std::path::Path::new(&repo_path));

    let repo = unwrap_or_log!(repo_result, "git_clone");

    REPO.lock().unwrap().replace(repo);

    0
}
#[unsafe(no_mangle)]
pub extern "C" fn Java_io_github_wiiznokes_gitnote_manager_GitManagerKt_lastCommitLib(
    env: JNIEnv,
    _class: JClass,
) -> jstring {
    let repo = REPO.lock().expect("repo lock");
    let repo = repo.as_ref().expect("repo");

    // new repo how no commit, so this function can fail
    let head = unwrap_or_log!(
        repo.refname_to_id("HEAD"),
        "refname_to_id",
        std::ptr::null_mut()
    );

    let head = head.to_string();

    env.new_string(head)
        .expect("Couldn't create Java string!")
        .into_raw()
}
#[unsafe(no_mangle)]
pub extern "C" fn Java_io_github_wiiznokes_gitnote_manager_GitManagerKt_commitAllLib<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    username: JString<'local>,
    message: JString<'local>,
) -> jint {
    let repo = REPO.lock().expect("repo lock");
    let repo = repo.as_ref().expect("repo");

    let mut index = unwrap_or_log!(repo.index(), "index");

    unwrap_or_log!(repo.index(), "index");

    unwrap_or_log!(
        index.add_all(["*"].iter(), IndexAddOption::DEFAULT, None),
        "add_all"
    );

    // Write index to disk
    unwrap_or_log!(index.write(), "write");

    // Write tree
    let tree_oid = unwrap_or_log!(index.write_tree(), "write_tree");

    let tree = unwrap_or_log!(repo.find_tree(tree_oid), "find_tree");

    // Get HEAD commit as parent, and Allow initial commit
    let parent_commit = repo.head().and_then(|r| r.peel_to_commit()).ok();

    let username: String = env.get_string(&username).unwrap().into();
    let message: String = env.get_string(&message).unwrap().into();

    let sig = unwrap_or_log!(Signature::now(&username, &username), "Signature::now");

    // Create commit
    let commit_result = match parent_commit {
        Some(ref parent) => repo.commit(Some("HEAD"), &sig, &sig, &message, &tree, &[parent]),
        None => repo.commit(Some("HEAD"), &sig, &sig, &message, &tree, &[]),
    };

    unwrap_or_log!(commit_result, "commit");

    0
}
#[unsafe(no_mangle)]
pub extern "C" fn Java_io_github_wiiznokes_gitnote_manager_GitManagerKt_pushLib<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    username: JString<'local>,
    password: JString<'local>,
    _progressCallback: JObject<'local>,
) -> jint {
    let repo = REPO.lock().expect("repo lock");
    let repo = repo.as_ref().expect("repo");

    let mut remote = unwrap_or_log!(repo.find_remote("origin"), "find_remote");

    let refspecs = ["refs/heads/main:refs/heads/main"];

    let mut callbacks = RemoteCallbacks::new();

    callbacks.certificate_check(|_cert, _| Ok(CertificateCheckStatus::CertificateOk));

    if !username.is_null() && !password.is_null() {
        let username: String = env.get_string(&username).unwrap().into();
        let password: String = env.get_string(&password).unwrap().into();

        callbacks.credentials(move |_url, _username_from_url, _allowed_types| {
            Cred::userpass_plaintext(&username, &password)
        });
    }

    let mut push_opts = PushOptions::new();
    push_opts.remote_callbacks(callbacks);

    let res = remote.push(&refspecs, Some(&mut push_opts));
    unwrap_or_log!(res, "push");

    0
}

#[unsafe(no_mangle)]
pub extern "C" fn Java_io_github_wiiznokes_gitnote_manager_GitManagerKt_pullLib<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    username: JString<'local>,
    password: JString<'local>,
    _progressCallback: JObject<'local>,
) -> jint {
    let repo = REPO.lock().expect("repo lock");
    let repo = repo.as_ref().expect("repo");

    let mut remote = unwrap_or_log!(repo.find_remote("origin"), "find_remote");

    let mut callbacks = RemoteCallbacks::new();

    callbacks.certificate_check(|_cert, _| Ok(CertificateCheckStatus::CertificateOk));

    if !username.is_null() && !password.is_null() {
        let username: String = env.get_string(&username).unwrap().into();
        let password: String = env.get_string(&password).unwrap().into();

        callbacks.credentials(move |_url, _username_from_url, _allowed_types| {
            Cred::userpass_plaintext(&username, &password)
        });
    }

    let mut fetch_options = FetchOptions::new();
    fetch_options.remote_callbacks(callbacks);

    let res = remote.fetch(&[] as &[&str], Some(&mut fetch_options), None);

    unwrap_or_log!(res, "fetch");

    let fetch_head = unwrap_or_log!(repo.find_reference("FETCH_HEAD"), "find_reference");

    let commit = unwrap_or_log!(
        repo.reference_to_annotated_commit(&fetch_head),
        "reference_to_annotated_commit"
    );

    unwrap_or_log!(merge::do_merge(repo, "main", commit), "merge");

    0
}

#[unsafe(no_mangle)]
pub extern "C" fn Java_io_github_wiiznokes_gitnote_manager_GitManagerKt_freeLib(
    _env: JNIEnv,
    _class: JClass,
) {
}

#[unsafe(no_mangle)]
pub extern "C" fn Java_io_github_wiiznokes_gitnote_manager_GitManagerKt_closeRepoLib(
    _env: JNIEnv,
    _class: JClass,
) {
    let mut repo = REPO.lock().expect("repo lock");
    repo.take();
}
#[unsafe(no_mangle)]
pub extern "C" fn Java_io_github_wiiznokes_gitnote_manager_GitManagerKt_isChangeLib(
    _env: JNIEnv,
    _class: JClass,
) -> jint {
    let repo = REPO.lock().expect("repo lock");
    let repo = repo.as_ref().expect("repo");

    let mut opts = StatusOptions::new();
    opts.include_untracked(true).recurse_untracked_dirs(true);

    let statuses = unwrap_or_log!(repo.statuses(Some(&mut opts)), "statuses");

    let count = statuses.len();

    (count > 0) as jint
}
