#![allow(non_snake_case)]

use std::sync::{LazyLock, Mutex};

use git2::Repository;
use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::{jint, jobject, jstring};

static REPO: LazyLock<Mutex<Option<Repository>>> = LazyLock::new(|| Mutex::new(None));

#[unsafe(no_mangle)]
pub extern "C" fn Java_io_github_wiiznokes_gitnote_manager_GitManagerKt_initLib(
    _env: JNIEnv,
    _class: JClass,
) -> jint {
    println!("Parameters: {:?}", ());

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

    let repo = match Repository::init(repo_path) {
        Ok(repo) => repo,
        Err(e) => {
            eprintln!("{e}");
            return e.code() as jint;
        }
    };

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

    let repo = match Repository::open(repo_path) {
        Ok(repo) => repo,
        Err(e) => {
            eprintln!("{e}");
            return e.code() as jint;
        }
    };

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
    println!(
        "Parameters: {:?}",
        (repoPath, remoteUrl, username, password, progressCallback)
    );
}
#[unsafe(no_mangle)]
pub extern "C" fn Java_io_github_wiiznokes_gitnote_manager_GitManagerKt_lastCommitLib(
    env: JNIEnv,
    _class: JClass,
) -> jstring {
    println!("Parameters: {:?}", ());

    let output = r#"Rust Method: lastCommitLib"#;
    env.new_string(output)
        .expect("Couldn't create Java string!")
        .into_raw()
}
#[unsafe(no_mangle)]
pub extern "C" fn Java_io_github_wiiznokes_gitnote_manager_GitManagerKt_commitAllLib(
    _env: JNIEnv,
    _class: JClass,
    username: jstring,
    message: jstring,
) -> jint {
    println!("Parameters: {:?}", (username, message));

    42
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
    println!("Parameters: {:?}", ());
}
#[unsafe(no_mangle)]
pub extern "C" fn Java_io_github_wiiznokes_gitnote_manager_GitManagerKt_closeRepoLib(
    _env: JNIEnv,
    _class: JClass,
) {
    println!("Parameters: {:?}", ());
}
#[unsafe(no_mangle)]
pub extern "C" fn Java_io_github_wiiznokes_gitnote_manager_GitManagerKt_isChangeLib(
    _env: JNIEnv,
    _class: JClass,
) -> jint {
    println!("Parameters: {:?}", ());

    42
}
