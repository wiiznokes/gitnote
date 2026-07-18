use std::fmt::{Debug, Display};

use anyhow::anyhow;
use git2::Signature;
use jni::objects::{JObject, JString, JValue};
use jni::sys::{jboolean, jint};
use jni::{Env, NativeMethod, jni_sig, jni_str, native_method};

use crate::callback::ProgressCB;
use crate::key_gen::gen_keys;
use crate::utils::install_panic_hook;

#[macro_use]
extern crate log;
#[macro_use]
mod utils;
mod key_gen;
mod libgit2;
mod mime_types;
mod url;

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

    fn add_message(self, msg1: &str) -> Self {
        match self {
            Error::Git2 { error, msg } => Error::Git2 {
                error,
                msg: format!("{}: {}", msg1, msg),
            },
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

const INIT_LIB_METHOD: NativeMethod = native_method! {
    java_type = "io.github.wiiznokes.gitnote.manager.GitManager",
    extern fn init_lib(home_path: JString) -> jint,
};

const CREATE_REPO_LIB_METHOD: NativeMethod = native_method! {
    java_type = "io.github.wiiznokes.gitnote.manager.GitManager",
    extern fn create_repo_lib(repo_path: JString) -> jint,
};

const OPEN_REPO_LIB_METHOD: NativeMethod = native_method! {
    java_type = "io.github.wiiznokes.gitnote.manager.GitManager",
    extern fn open_repo_lib(repo_path: JString) -> jint,
};

const CLONE_REPO_LIB_METHOD: NativeMethod = native_method! {
    java_type = "io.github.wiiznokes.gitnote.manager.GitManager",
    extern fn clone_repo_lib(repo_path: JString, remote_url: JString, cred: JObject, progress_callback: JObject) -> jint,
};

const LAST_COMMIT_LIB_METHOD: NativeMethod = native_method! {
    java_type = "io.github.wiiznokes.gitnote.manager.GitManager",
    extern fn last_commit_lib() -> JString,
};

const COMMIT_ALL_LIB_METHOD: NativeMethod = native_method! {
    java_type = "io.github.wiiznokes.gitnote.manager.GitManager",
    extern fn commit_all_lib(name: JString, email: JString, message: JString) -> jint,
};

const CURRENT_SIGNATURE_LIB_METHOD: NativeMethod = native_method! {
    java_type = "io.github.wiiznokes.gitnote.manager.GitManager",
    extern fn current_signature_lib() -> JObject,
};

const PUSH_LIB_METHOD: NativeMethod = native_method! {
    java_type = "io.github.wiiznokes.gitnote.manager.GitManager",
    extern fn push_lib(cred: JObject) -> jint,
};

const PULL_LIB_METHOD: NativeMethod = native_method! {
    java_type = "io.github.wiiznokes.gitnote.manager.GitManager",
    extern fn pull_lib(cred: JObject, name: JString, email: JString) -> jint,
};

const FREE_LIB_METHOD: NativeMethod = native_method! {
    java_type = "io.github.wiiznokes.gitnote.manager.GitManager",
    extern fn free_lib(),
};

const CLOSE_REPO_LIB_METHOD: NativeMethod = native_method! {
    java_type = "io.github.wiiznokes.gitnote.manager.GitManager",
    extern fn close_repo_lib(),
};

const IS_CHANGE_LIB_LIB_METHOD: NativeMethod = native_method! {
    java_type = "io.github.wiiznokes.gitnote.manager.GitManager",
    extern fn is_change_lib() -> jint,
};

const GET_TIMESTAMPS_LIB_METHOD: NativeMethod = native_method! {
    java_type = "io.github.wiiznokes.gitnote.manager.GitManager",
    extern fn get_timestamps_lib(j_map: JObject) -> jint,
};

const GENERATE_SSH_KEYS_LIB_METHOD: NativeMethod = native_method! {
    java_type = "io.github.wiiznokes.gitnote.manager.GitManager",
    extern fn generate_ssh_keys_lib() -> JObject,
};

const EXTENSION_TYPE_LIB_METHOD: NativeMethod = native_method! {
    java_type = "io.github.wiiznokes.gitnote.manager.GitManager",
    extern fn extension_type_lib(extension: JString) -> jint,
};

const IS_EXTENSION_SUPPORTED_LIB_METHOD: NativeMethod = native_method! {
    java_type = "io.github.wiiznokes.gitnote.manager.GitManager",
    extern fn is_extension_supported_lib(extension: JString) -> jboolean,
};

const GET_URL_INFO_LIB_METHOD: NativeMethod = native_method! {
    java_type = "io.github.wiiznokes.gitnote.manager.GitManager",
    extern fn get_url_info_lib(url: JString) -> JObject,
};

fn init_lib<'local>(
    env: &mut Env<'local>,
    _this: JObject<'local>,
    home_path: JString<'local>,
) -> Result<jint, jni::errors::Error> {
    let home_path = home_path.try_to_string(env).unwrap();
    libgit2::init_lib(home_path);

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

    Ok(OK)
}

fn create_repo_lib<'local>(
    env: &mut Env<'local>,
    _this: JObject<'local>,
    repo_path: JString<'local>,
) -> Result<jint, jni::errors::Error> {
    let repo_path = repo_path.try_to_string(env).unwrap();

    unwrap_or_log!(libgit2::create_repo(&repo_path), "create_repo");

    Ok(OK)
}
fn open_repo_lib<'local>(
    env: &mut Env<'local>,
    _this: JObject<'local>,
    repo_path: JString<'local>,
) -> Result<jint, jni::errors::Error> {
    let repo_path = repo_path.try_to_string(env).unwrap();

    unwrap_or_log!(libgit2::open_repo(&repo_path), "open_repo");

    Ok(OK)
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
        passphrase: Option<String>,
    },
}

pub struct GitAuthor {
    pub name: String,
    pub email: String,
}

impl<'a> From<Signature<'a>> for GitAuthor {
    fn from(value: Signature<'a>) -> Self {
        GitAuthor {
            name: value.name().unwrap_or("").to_string(),
            email: value.email().unwrap_or("").to_string(),
        }
    }
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
                passphrase: _passphrase,
            } => f
                .debug_struct("Ssh")
                .field("username", username)
                .field("public_key", public_key)
                .finish(),
        }
    }
}

macro_rules! jstring_field {
    ($env:expr, $obj:expr, $field:literal) => {{
        let obj = $env
            .get_field($obj, jni_str!($field), jni_sig!(JString))?
            .l()?;

        $env.as_cast::<JString>(&obj)?
            .mutf8_chars($env)?
            .to_string()
    }};
}

macro_rules! jstring_field_nullable {
    ($env:expr, $obj:expr, $field:literal) => {{
        let obj = $env
            .get_field($obj, jni_str!($field), jni_sig!(JString))?
            .l()?;

        if obj.is_null() {
            None
        } else {
            Some(
                $env.as_cast::<JString>(&obj)?
                    .mutf8_chars($env)?
                    .to_string(),
            )
        }
    }};
}

impl Cred {
    pub fn from_jni(env: &mut Env, cred_obj: &JObject) -> anyhow::Result<Option<Self>> {
        if cred_obj.is_null() {
            return Ok(None);
        }

        let class_name = {
            let class = env.get_object_class(cred_obj)?;

            let obj = env
                .call_method(class, jni_str!("getName"), jni_sig!(() -> JString), &[])?
                .l()?;

            let jstring = env.as_cast::<JString>(&obj)?;

            jstring.mutf8_chars(env)?.to_string()
        };

        match class_name.as_str() {
            "io.github.wiiznokes.gitnote.ui.model.Cred$UserPassPlainText" => {
                let username = jstring_field!(env, cred_obj, "username");
                let password = jstring_field!(env, cred_obj, "username");

                Ok(Some(Cred::UserPassPlainText { username, password }))
            }
            "io.github.wiiznokes.gitnote.ui.model.Cred$Ssh" => {
                let username = jstring_field!(env, cred_obj, "username");
                let public_key = jstring_field!(env, cred_obj, "publicKey");

                let private_key = jstring_field!(env, cred_obj, "privateKey");
                let passphrase = jstring_field_nullable!(env, cred_obj, "passphrase");

                Ok(Some(Cred::Ssh {
                    username,
                    public_key,
                    private_key,
                    passphrase,
                }))
            }
            other => Err(anyhow!("Unknown class name: {}", other)),
        }
    }
}

mod callback {
    use jni::{Env, jni_sig, jni_str, objects::JObject};

    pub struct ProgressCB<'ptr, 'local> {
        env: &'ptr mut Env<'local>,
        callback_class: JObject<'local>,
    }

    impl<'ptr, 'local> ProgressCB<'ptr, 'local> {
        pub fn new(env: &'ptr mut Env<'local>, callback_class: JObject<'local>) -> Self {
            Self {
                env,
                callback_class,
            }
        }
        pub fn progress(&mut self, progress: i32) -> bool {
            let res = self
                .env
                .call_method(
                    &self.callback_class,
                    jni_str!("progressCb"),
                    jni_sig!((jint) -> jboolean),
                    &[progress.into()],
                )
                .unwrap();

            

            res.z().unwrap()
        }
    }
}
fn clone_repo_lib<'local>(
    env: &mut Env<'local>,
    _this: JObject<'local>,
    repo_path: JString<'local>,
    remote_url: JString<'local>,
    cred: JObject<'local>,
    progress_callback: JObject<'local>,
) -> Result<jint, jni::errors::Error> {
    let repo_path = repo_path.try_to_string(env).unwrap();
    let remote_url = remote_url.try_to_string(env).unwrap();

    let cred = match Cred::from_jni(env, &cred) {
        Ok(cred) => cred,
        Err(e) => {
            error!("Cred::from_jni: {e}");
            panic!()
        }
    };

    let cb = ProgressCB::new(env, progress_callback);

    unwrap_or_log!(
        libgit2::clone_repo(&repo_path, &remote_url, cred, cb),
        "clone_repo"
    );

    Ok(OK)
}
fn last_commit_lib<'local>(
    env: &mut Env<'local>,
    _this: JObject<'local>,
) -> Result<JString<'local>, jni::errors::Error> {
    let commit = match libgit2::last_commit() {
        Some(commit) => commit,
        None => return Ok(JString::null()),
    };

    let s = env
        .new_string(commit)
        .expect("Couldn't create Java string!");

    Ok(s)
}
fn commit_all_lib<'local>(
    env: &mut Env<'local>,
    _this: JObject<'local>,
    name: JString<'local>,
    email: JString<'local>,
    message: JString<'local>,
) -> Result<jint, jni::errors::Error> {
    let name = name.try_to_string(env).unwrap();
    let email = email.try_to_string(env).unwrap();
    let message = message.try_to_string(env).unwrap();

    unwrap_or_log!(libgit2::commit_all(&name, &email, &message), "commit_all");

    Ok(OK)
}

fn current_signature_lib<'local>(
    env: &mut Env<'local>,
    _this: JObject<'local>,
) -> Result<JObject<'local>, jni::errors::Error> {
    let signature = match libgit2::signature() {
        Some(signature) => signature,
        None => return Ok(JObject::null()),
    };

    let name_jstring = env.new_string(&signature.0).unwrap();
    let email_jstring = env.new_string(&signature.1).unwrap();

    let pair_class = env.find_class(jni_str!("kotlin/Pair")).unwrap();

    let pair_obj = env
        .new_object(
            &pair_class,
            jni_sig!((JObject, JObject)),
            &[(&name_jstring).into(), (&email_jstring).into()],
        )
        .unwrap();

    Ok(pair_obj)
}
fn push_lib<'local>(
    env: &mut Env<'local>,
    _this: JObject<'local>,
    cred: JObject<'local>,
) -> Result<jint, jni::errors::Error> {
    let cred = Cred::from_jni(env, &cred).unwrap();
    unwrap_or_log!(libgit2::push(cred), "push");
    Ok(OK)
}

fn pull_lib<'local>(
    env: &mut Env<'local>,
    _this: JObject<'local>,
    cred: JObject<'local>,
    name: JString<'local>,
    email: JString<'local>,
) -> Result<jint, jni::errors::Error> {
    let cred = Cred::from_jni(env, &cred).unwrap();
    let name: String = name.try_to_string(env).unwrap();
    let email: String = email.try_to_string(env).unwrap();
    let author = GitAuthor { name, email };
    unwrap_or_log!(libgit2::pull(cred, &author), "pull");
    Ok(OK)
}

fn free_lib<'local>(
    _env: &mut Env<'local>,
    _this: JObject<'local>,
) -> Result<(), jni::errors::Error> {
    Ok(())
}

fn close_repo_lib<'local>(
    _env: &mut Env<'local>,
    _this: JObject<'local>,
) -> Result<(), jni::errors::Error> {
    libgit2::close();
    Ok(())
}
fn is_change_lib<'local>(
    _env: &mut Env<'local>,
    _this: JObject<'local>,
) -> Result<jint, jni::errors::Error> {
    let is_change = unwrap_or_log!(libgit2::is_change(), "is_change");

    Ok(is_change as jint)
}

fn get_timestamps_lib<'local>(
    env: &mut Env<'local>,
    _this: JObject<'local>,
    j_map: JObject<'local>,
) -> Result<jint, jni::errors::Error> {
    let timestamps = unwrap_or_log!(libgit2::get_timestamps(), "get_timestamps");

    if let Err(e) = get_timestamps_jni(env, &j_map, timestamps.iter()) {
        error!("get_timestamps_jni: {e}");
        return Ok(-1);
    }

    Ok(OK)
}

fn get_timestamps_jni<'local, 'a>(
    env: &mut Env<'local>,
    j_map: &JObject<'local>,
    timestamps: impl Iterator<Item = (&'a String, &'a i64)>,
) -> Result<(), Box<dyn std::error::Error>> {
    let map_class = env.get_object_class(j_map)?;
    let put_method = env.get_method_id(
        map_class,
        jni_str!("put"),
        jni_sig!((JObject, JObject) -> JObject),
    )?;

    let long_class = env.find_class(jni_str!("java/lang/Long"))?;
    let long_ctor = env.get_method_id(&long_class, jni_str!("<init>"), jni_sig!((jlong)))?;

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

fn generate_ssh_keys_lib<'local>(
    env: &mut Env<'local>,
    _this: JObject<'local>,
) -> Result<JObject<'local>, jni::errors::Error> {
    let keys = match gen_keys() {
        Ok(keys) => keys,
        Err(e) => {
            error!("can't gen keys: {e}");
            return Ok(JObject::null());
        }
    };

    let public_jstring = env.new_string(&keys.public).unwrap();
    let private_jstring = env.new_string(&keys.private).unwrap();

    let pair_class = env.find_class(jni_str!("kotlin/Pair")).unwrap();

    let pair_obj = env
        .new_object(
            &pair_class,
            jni_sig!((JObject, JObject)),
            &[(&public_jstring).into(), (&private_jstring).into()],
        )
        .unwrap();

    Ok(pair_obj)
}

fn extension_type_lib<'local>(
    env: &mut Env<'local>,
    _this: JObject<'local>,
    extension: JString<'local>,
) -> Result<jint, jni::errors::Error> {
    let extension = extension.try_to_string(env).unwrap();

    let res = match mime_types::extension_type(extension.as_str()) {
        Some(ext_type) => ext_type as jint,
        None => 0,
    };

    Ok(res)
}

fn is_extension_supported_lib<'local>(
    env: &mut Env<'local>,
    _this: JObject<'local>,
    extension: JString<'local>,
) -> Result<jboolean, jni::errors::Error> {
    let extension = extension.try_to_string(env).unwrap();

    let res = mime_types::is_extension_supported(extension.as_str());
    Ok(res)
}

fn get_url_info_lib<'local>(
    env: &mut Env<'local>,
    _this: JObject<'local>,
    url: JString<'local>,
) -> Result<JObject<'local>, jni::errors::Error> {
    let url = url.try_to_string(env).unwrap();

    let url_info = match url::parse_url(&url) {
        Ok(info) => info,
        Err(e) => {
            error!("{e}");
            return Ok(JObject::null());
        }
    };

    let is_ssh = url_info.kind == url::UrlKind::Ssh;

    let boolean_class = env.find_class(jni_str!("java/lang/Boolean")).unwrap();

    let obj = env
        .new_object(
            boolean_class,
            jni_sig!((jboolean)),
            &[JValue::Bool(is_ssh)],
        )
        .unwrap();

    Ok(obj)
}
