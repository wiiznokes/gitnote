#![allow(non_snake_case)]

use std::fmt::Display;

use jni::JNIEnv;
use jni::objects::{JClass, JObject, JString};
use jni::sys::{jint, jstring};

use crate::utils::install_panic_hook;

#[macro_use]
extern crate log;
#[macro_use]
mod utils;
mod libgit2;

const OK: jint = 0;

enum Error {
    Git2 { error: git2::Error, msg: String },
}

impl Error {
    fn git2(error: git2::Error, msg: &str) -> Self {
        Self::Git2 {
            error,
            msg: msg.into(),
        }
    }
}

impl From<Error> for jint {
    fn from(value: Error) -> Self {
        match value {
            Error::Git2 { error, .. } => error.raw_code(),
        }
    }
}

impl Display for Error {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            Error::Git2 { error, msg } => {
                write!(f, "{msg}: {error}")
            }
        }
    }
}

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

    OK
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

    unwrap_or_log!(libgit2::create_repo(&repo_path), "create_repo");

    OK
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

    unwrap_or_log!(libgit2::open_repo(&repo_path), "open_repo");

    OK
}

pub struct Creeds {
    pub username: String,
    pub password: String,
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

    let creeds = if !username.is_null() && !password.is_null() {
        let username: String = env.get_string(&username).unwrap().into();
        let password: String = env.get_string(&password).unwrap().into();

        Some(Creeds { username, password })
    } else {
        None
    };

    unwrap_or_log!(
        libgit2::clone_repo(&repo_path, &remote_url, creeds),
        "clone_repo"
    );

    OK
}
#[unsafe(no_mangle)]
pub extern "C" fn Java_io_github_wiiznokes_gitnote_manager_GitManagerKt_lastCommitLib(
    env: JNIEnv,
    _class: JClass,
) -> jstring {
    let commit = match libgit2::last_commit() {
        Some(commit) => commit,
        None => return std::ptr::null_mut(),
    };

    env.new_string(commit)
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
    let username: String = env.get_string(&username).unwrap().into();
    let message: String = env.get_string(&message).unwrap().into();

    unwrap_or_log!(libgit2::commit_all(&username, &message), "commit_all");

    OK
}
#[unsafe(no_mangle)]
pub extern "C" fn Java_io_github_wiiznokes_gitnote_manager_GitManagerKt_pushLib<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    username: JString<'local>,
    password: JString<'local>,
    _progressCallback: JObject<'local>,
) -> jint {
    let creeds = if !username.is_null() && !password.is_null() {
        let username: String = env.get_string(&username).unwrap().into();
        let password: String = env.get_string(&password).unwrap().into();

        Some(Creeds { username, password })
    } else {
        None
    };

    unwrap_or_log!(libgit2::push(creeds), "push");

    OK
}

#[unsafe(no_mangle)]
pub extern "C" fn Java_io_github_wiiznokes_gitnote_manager_GitManagerKt_pullLib<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    username: JString<'local>,
    password: JString<'local>,
    _progressCallback: JObject<'local>,
) -> jint {
    let creeds = if !username.is_null() && !password.is_null() {
        let username: String = env.get_string(&username).unwrap().into();
        let password: String = env.get_string(&password).unwrap().into();

        Some(Creeds { username, password })
    } else {
        None
    };

    unwrap_or_log!(libgit2::pull(creeds), "pull");

    OK
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
    libgit2::close();
}
#[unsafe(no_mangle)]
pub extern "C" fn Java_io_github_wiiznokes_gitnote_manager_GitManagerKt_isChangeLib(
    _env: JNIEnv,
    _class: JClass,
) -> jint {
    let is_change = unwrap_or_log!(libgit2::is_change(), "is_change");

    is_change as jint
}
