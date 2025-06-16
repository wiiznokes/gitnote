use jni::JNIEnv;
use jni::objects::JClass;
use jni::sys::{jint, jobject, jstring};

#[unsafe(no_mangle)]
pub extern "C" fn Java_io_github_wiiznokes_gitnote_manager_GitManager_initLib(
    env: JNIEnv,
    _class: JClass,
) -> jint {
    println!("Parameters: {:?}", ());

    42
}

#[unsafe(no_mangle)]
pub extern "C" fn Java_io_github_wiiznokes_gitnote_manager_GitManager_createRepoLib(
    env: JNIEnv,
    _class: JClass,
    repoPath: jstring,
) -> jint {
    println!("Parameters: {:?}", (repoPath));

    42
}
#[unsafe(no_mangle)]
pub extern "C" fn Java_io_github_wiiznokes_gitnote_manager_GitManager_openRepoLib(
    env: JNIEnv,
    _class: JClass,
    repoPath: jstring,
) -> jint {
    println!("Parameters: {:?}", (repoPath));

    42
}
#[unsafe(no_mangle)]
pub extern "C" fn Java_io_github_wiiznokes_gitnote_manager_GitManager_cloneRepoLib(
    env: JNIEnv,
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
pub extern "C" fn Java_io_github_wiiznokes_gitnote_manager_GitManager_lastCommitLib(
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
pub extern "C" fn Java_io_github_wiiznokes_gitnote_manager_GitManager_commitAllLib(
    env: JNIEnv,
    _class: JClass,
    username: jstring,
    message: jstring,
) -> jint {
    println!("Parameters: {:?}", (username, message));

    42
}
#[unsafe(no_mangle)]
pub extern "C" fn Java_io_github_wiiznokes_gitnote_manager_GitManager_pushLib(
    env: JNIEnv,
    _class: JClass,
    username: jobject,
    password: jobject,
    progressCallback: jobject,
) {
    println!("Parameters: {:?}", (username, password, progressCallback));
}
#[unsafe(no_mangle)]
pub extern "C" fn Java_io_github_wiiznokes_gitnote_manager_GitManager_pullLib(
    env: JNIEnv,
    _class: JClass,
    username: jobject,
    password: jobject,
    progressCallback: jobject,
) {
    println!("Parameters: {:?}", (username, password, progressCallback));
}
#[unsafe(no_mangle)]
pub extern "C" fn Java_io_github_wiiznokes_gitnote_manager_GitManager_freeLib(
    env: JNIEnv,
    _class: JClass,
) {
    println!("Parameters: {:?}", ());
}
#[unsafe(no_mangle)]
pub extern "C" fn Java_io_github_wiiznokes_gitnote_manager_GitManager_closeRepoLib(
    env: JNIEnv,
    _class: JClass,
) {
    println!("Parameters: {:?}", ());
}
#[unsafe(no_mangle)]
pub extern "C" fn Java_io_github_wiiznokes_gitnote_manager_GitManager_isChangeLib(
    env: JNIEnv,
    _class: JClass,
) -> jint {
    println!("Parameters: {:?}", ());

    42
}