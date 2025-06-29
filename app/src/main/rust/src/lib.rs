#![allow(non_snake_case)]

use jni::JNIEnv;
use jni::objects::JClass;
use jni::sys::{jint, jobject, jstring};

#[unsafe(no_mangle)]
pub extern "C" fn Java_io_github_wiiznokes_gitnote_manager_GitManagerKt_initLib(
    _env: JNIEnv,
    _class: JClass,
) -> jint {
    println!("Parameters: {:?}", ());

    10
}

#[unsafe(no_mangle)]
pub extern "C" fn Java_io_github_wiiznokes_gitnote_manager_GitManagerKt_createRepoLib(
    _env: JNIEnv,
    _class: JClass,
    repoPath: jstring,
) -> jint {
    println!("Parameters: {:?}", (repoPath));

    42
}
#[unsafe(no_mangle)]
pub extern "C" fn Java_io_github_wiiznokes_gitnote_manager_GitManagerKt_openRepoLib(
    _env: JNIEnv,
    _class: JClass,
    repoPath: jstring,
) -> jint {
    println!("Parameters: {:?}", (repoPath));

    42
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