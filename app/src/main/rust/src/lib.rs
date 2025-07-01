#![allow(non_snake_case)]

use std::sync::{LazyLock, Mutex};

use git2::{IndexAddOption, Repository, Signature};
use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::{jint, jobject, jstring};

#[macro_use]
extern crate log;
#[macro_use]
mod utils;

static REPO: LazyLock<Mutex<Option<Repository>>> = LazyLock::new(|| Mutex::new(None));

#[unsafe(no_mangle)]
pub extern "C" fn Java_io_github_wiiznokes_gitnote_manager_GitManagerKt_initLib(
    _env: JNIEnv,
    _class: JClass,
) -> jint {
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

    return 0;
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

    return 0;
}
#[unsafe(no_mangle)]
pub extern "C" fn Java_io_github_wiiznokes_gitnote_manager_GitManagerKt_cloneRepoLib(
    _env: JNIEnv,
    _class: JClass,
    repoPath: jstring,
    remoteUrl: jstring,
    username: jobject,
    password: jobject,
    progressCallback: jobject,
) {
    info!(
        "Parameters: {:?}",
        (repoPath, remoteUrl, username, password, progressCallback)
    );
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

    // Get HEAD commit as parent
    let parent_commit = match repo.head().and_then(|r| r.peel_to_commit()) {
        Ok(commit) => Some(commit),
        Err(_) => None, // Allow initial commit
    };

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
pub extern "C" fn Java_io_github_wiiznokes_gitnote_manager_GitManagerKt_pushLib(
    _env: JNIEnv,
    _class: JClass,
    username: jobject,
    password: jobject,
    progressCallback: jobject,
) {
    println!("Parameters: {:?}", (username, password, progressCallback));
}
#[unsafe(no_mangle)]
pub extern "C" fn Java_io_github_wiiznokes_gitnote_manager_GitManagerKt_pullLib(
    _env: JNIEnv,
    _class: JClass,
    username: jobject,
    password: jobject,
    progressCallback: jobject,
) {
    println!("Parameters: {:?}", (username, password, progressCallback));
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
    println!("Parameters: {:?}", ());

    42
}
