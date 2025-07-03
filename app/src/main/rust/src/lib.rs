use std::fmt::{Debug, Display};
use std::sync::OnceLock;

use anyhow::anyhow;
use jni::JNIEnv;
use jni::objects::{JClass, JObject, JString, JValue};
use jni::sys::{jint, jobject, jstring};

use crate::callback::ProgressCB;
use crate::key_gen::gen_keys;
use crate::utils::install_panic_hook;

#[macro_use]
extern crate log;
#[macro_use]
mod utils;
mod key_gen;
mod libgit2;

#[cfg(test)]
mod test;

const OK: jint = 0;

#[derive(Debug)]
enum Error {
    Git2 { error: git2::Error, msg: String },
}

impl From<git2::Error> for Error {
    fn from(value: git2::Error) -> Self {
        Self::git2(value, "")
    }
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

static HOME_PATH: OnceLock<String> = OnceLock::new();
fn apply_ssh_workaround() {
    let home = HOME_PATH.get().unwrap();
    unsafe {
        std::env::set_var("HOME", home);
    }

    let _ = std::fs::create_dir_all(format!("{home}/.ssh"));
    let _ = std::fs::File::create(format!("{home}/.ssh/known_hosts"));
}

#[unsafe(no_mangle)]
pub extern "C" fn Java_io_github_wiiznokes_gitnote_manager_GitManagerKt_initLib<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    home_path: JString<'local>,
) -> jint {
    // https://github.com/libgit2/libgit2/pull/7056
    let home_path: String = env
        .get_string(&home_path)
        .expect("Couldn't get java string!")
        .into();
    HOME_PATH.set(home_path).unwrap();

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
    repo_path: JString<'local>,
) -> jint {
    let repo_path: String = env
        .get_string(&repo_path)
        .expect("Couldn't get java string!")
        .into();

    unwrap_or_log!(libgit2::create_repo(&repo_path), "create_repo");

    OK
}
#[unsafe(no_mangle)]
pub extern "C" fn Java_io_github_wiiznokes_gitnote_manager_GitManagerKt_openRepoLib<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    repo_path: JString<'local>,
) -> jint {
    let repo_path: String = env
        .get_string(&repo_path)
        .expect("Couldn't get java string!")
        .into();

    unwrap_or_log!(libgit2::open_repo(&repo_path), "open_repo");

    OK
}

pub enum Cred {
    UserPassPlainText {
        username: String,
        password: String,
    },
    Ssh {
        username: String,
        public_key: String,
        private_key: String,
    },
}

impl Debug for Cred {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            Self::UserPassPlainText {
                username,
                password: _password,
            } => f
                .debug_struct("UserPassPlainText")
                .field("username", username)
                .finish(),
            Self::Ssh {
                username,
                public_key,
                private_key: _private_key,
            } => f
                .debug_struct("Ssh")
                .field("username", username)
                .field("public_key", public_key)
                .finish(),
        }
    }
}

impl Cred {
    pub fn from_jni(env: &mut JNIEnv, cred_obj: &JObject) -> anyhow::Result<Option<Self>> {
        if cred_obj.is_null() {
            return Ok(None);
        }

        let class = env.get_object_class(cred_obj)?;
        let class_name_jstring: JString = env
            .call_method(class, "getName", "()Ljava/lang/String;", &[])?
            .l()?
            .into();
        let class_name: String = env.get_string(&class_name_jstring)?.into();

        match class_name.as_str() {
            "io.github.wiiznokes.gitnote.ui.model.Cred$UserPassPlainText" => {
                let username_obj: JString = env
                    .get_field(cred_obj, "username", "Ljava/lang/String;")?
                    .l()?
                    .into();
                let password_obj: JString = env
                    .get_field(cred_obj, "password", "Ljava/lang/String;")?
                    .l()?
                    .into();

                let username: String = env.get_string(&username_obj)?.into();
                let password: String = env.get_string(&password_obj)?.into();

                Ok(Some(Cred::UserPassPlainText { username, password }))
            }
            "io.github.wiiznokes.gitnote.ui.model.Cred$Ssh" => {
                let username_key_obj: JString = env
                    .get_field(cred_obj, "username", "Ljava/lang/String;")?
                    .l()?
                    .into();

                let public_key_obj: JString = env
                    .get_field(cred_obj, "publicKey", "Ljava/lang/String;")?
                    .l()?
                    .into();

                let private_key_obj: JString = env
                    .get_field(cred_obj, "privateKey", "Ljava/lang/String;")?
                    .l()?
                    .into();

                let username: String = env.get_string(&username_key_obj)?.into();
                let public_key: String = env.get_string(&public_key_obj)?.into();
                let private_key: String = env.get_string(&private_key_obj)?.into();

                Ok(Some(Cred::Ssh {
                    username,
                    public_key,
                    private_key,
                }))
            }
            other => Err(anyhow!("Unknown class name: {}", other)),
        }
    }
}

mod callback {
    use jni::{JNIEnv, objects::JObject};

    pub struct ProgressCB<'a, 'b> {
        env: &'b mut JNIEnv<'a>,
        callback_class: JObject<'a>,
    }

    impl<'a, 'b> ProgressCB<'a, 'b> {
        pub fn new(env: &'b mut JNIEnv<'a>, callback_class: JObject<'a>) -> Self {
            Self {
                env,
                callback_class,
            }
        }
        pub fn progress(&mut self, progress: i32) {
            match self.env.call_method(
                &self.callback_class,
                "progressCb",
                "(I)V",
                &[progress.into()],
            ) {
                Ok(_) => {}
                Err(e) => {
                    error!("{e}");
                }
            }
        }
    }
}
#[unsafe(no_mangle)]
pub extern "C" fn Java_io_github_wiiznokes_gitnote_manager_GitManagerKt_cloneRepoLib<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    repo_path: JString<'local>,
    remote_url: JString<'local>,
    cred: JString<'local>,
    progress_callback: JObject<'local>,
) -> jint {
    apply_ssh_workaround();
    let repo_path: String = env.get_string(&repo_path).unwrap().into();
    let remote_url: String = env.get_string(&remote_url).unwrap().into();

    let cred = match Cred::from_jni(&mut env, &cred) {
        Ok(cred) => cred,
        Err(e) => {
            error!("Cred::from_jni: {e}");
            panic!()
        }
    };

    let cb = ProgressCB::new(&mut env, progress_callback);

    unwrap_or_log!(
        libgit2::clone_repo(&repo_path, &remote_url, cred, cb),
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
    cred: JString<'local>,
) -> jint {
    apply_ssh_workaround();
    let cred = Cred::from_jni(&mut env, &cred).unwrap();
    unwrap_or_log!(libgit2::push(cred), "push");
    OK
}

#[unsafe(no_mangle)]
pub extern "C" fn Java_io_github_wiiznokes_gitnote_manager_GitManagerKt_pullLib<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    cred: JString<'local>,
) -> jint {
    apply_ssh_workaround();
    let cred = Cred::from_jni(&mut env, &cred).unwrap();
    unwrap_or_log!(libgit2::pull(cred), "pull");
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

#[unsafe(no_mangle)]
pub extern "C" fn Java_io_github_wiiznokes_gitnote_manager_GitManagerKt_getTimestampsLib<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    j_map: JObject<'local>,
) -> jint {
    let timestamps = unwrap_or_log!(libgit2::get_timestamps(), "get_timestamps");

    if let Err(e) = get_timestamps_jni(&mut env, &j_map, timestamps.iter()) {
        error!("get_timestamps_jni: {e}");
        return -1;
    }

    OK
}

fn get_timestamps_jni<'local, 'a>(
    env: &mut JNIEnv<'local>,
    j_map: &JObject<'local>,
    timestamps: impl Iterator<Item = (&'a String, &'a i64)>,
) -> Result<(), Box<dyn std::error::Error>> {
    let map_class = env.get_object_class(j_map)?;
    let put_method = env.get_method_id(
        map_class,
        "put",
        "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
    )?;

    let long_class = env.find_class("java/lang/Long")?;
    let long_ctor = env.get_method_id(&long_class, "<init>", "(J)V")?;

    for (path, timestamp) in timestamps {
        let j_key: JString = env.new_string(path)?;

        unsafe {
            let j_value = env.new_object_unchecked(
                &long_class,
                long_ctor,
                &[JValue::Long(*timestamp).as_jni()],
            )?;

            env.call_method_unchecked(
                j_map,
                put_method,
                jni::signature::ReturnType::Object,
                &[
                    JValue::Object(&JObject::from(j_key)).as_jni(),
                    JValue::Object(&j_value).as_jni(),
                ],
            )?;
        }
    }

    Ok(())
}

#[unsafe(no_mangle)]
pub extern "C" fn Java_io_github_wiiznokes_gitnote_manager_GitManagerKt_generateSshKeysLib<
    'local,
>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
) -> jobject {
    let keys = match gen_keys() {
        Ok(keys) => keys,
        Err(e) => {
            error!("can't gen keys: {e}");
            return std::ptr::null_mut();
        }
    };

    let public_jstring = env.new_string(&keys.public).unwrap();
    let private_jstring = env.new_string(&keys.private).unwrap();

    let pair_class = env.find_class("kotlin/Pair").unwrap();

    let pair_obj = env
        .new_object(
            &pair_class,
            "(Ljava/lang/Object;Ljava/lang/Object;)V",
            &[(&public_jstring).into(), (&private_jstring).into()],
        )
        .unwrap();

    pair_obj.into_raw()
}
