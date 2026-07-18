use std::fmt::{Debug, Display};

use anyhow::anyhow;
use git2::Signature;
use jni::objects::{JClass, JObject, JString, JValue};
use jni::sys::{JNIEnv, jboolean, jint, jobject, jstring};
use jni::{Env, EnvUnowned, jni_sig, jni_str};
use tap::{Pipe, Tap};

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

#[unsafe(no_mangle)]
pub extern "C" fn Java_io_github_wiiznokes_gitnote_manager_GitManagerKt_initLib<'caller>(
    mut env: EnvUnowned<'caller>,
    _class: JClass<'caller>,
    home_path: JString<'caller>,
) -> jint {

    env.with_env(|env| {
        Ok(home_path.mutf8_chars(env)?.to_string())
    }).into_outcome();

    let home_path = home_path.to_string();
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

    OK
}

#[unsafe(no_mangle)]
pub extern "C" fn Java_io_github_wiiznokes_gitnote_manager_GitManagerKt_createRepoLib<'caller>(
    _env: EnvUnowned<'caller>,
    _class: JClass<'caller>,
    repo_path: JString<'caller>,
) -> jint {
    let repo_path: String = repo_path.to_string();

    unwrap_or_log!(libgit2::create_repo(&repo_path), "create_repo");

    OK
}
#[unsafe(no_mangle)]
pub extern "C" fn Java_io_github_wiiznokes_gitnote_manager_GitManagerKt_openRepoLib<'caller>(
    _env: EnvUnowned<'caller>,
    _class: JClass<'caller>,
    repo_path: JString<'caller>,
) -> jint {
    let repo_path: String = repo_path.to_string();

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
    use jni::{EnvUnowned, jni_sig, jni_str, objects::JObject};

    pub struct ProgressCB<'a, 'b> {
        env: &'b mut EnvUnowned<'a>,
        callback_class: JObject<'a>,
    }

    impl<'a, 'b> ProgressCB<'a, 'b> {
        pub fn new(env: &'b mut EnvUnowned<'a>, callback_class: JObject<'a>) -> Self {
            Self {
                env,
                callback_class,
            }
        }
        pub fn progress(&mut self, progress: i32) -> bool {
            let res: jni::EnvOutcome<'_, _, anyhow::Error> = self.env.with_env(|env| {
                let res = env.call_method(
                    &self.callback_class,
                    jni_str!("progressCb"),
                    jni_sig!((jint) -> jboolean),
                    &[progress.into()],
                )?;

                let res = res.z()?;

                Ok(res)
            });

            match res.into_outcome() {
                jni::Outcome::Ok(bool) => bool,
                jni::Outcome::Err(e) => {
                    error!("{e}");
                    true
                }
                jni::Outcome::Panic(_any) => true,
            }
        }
    }
}
#[unsafe(no_mangle)]
pub extern "C" fn Java_io_github_wiiznokes_gitnote_manager_GitManagerKt_cloneRepoLib<'caller>(
    mut env: EnvUnowned<'caller>,
    _class: JClass<'caller>,
    repo_path: JString<'caller>,
    remote_url: JString<'caller>,
    cred: JString<'caller>,
    progress_callback: JObject<'caller>,
) -> jint {
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
    env: EnvUnowned,
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
pub extern "C" fn Java_io_github_wiiznokes_gitnote_manager_GitManagerKt_commitAllLib<'caller>(
    mut env: EnvUnowned<'caller>,
    _class: JClass<'caller>,
    name: JString<'caller>,
    email: JString<'caller>,
    message: JString<'caller>,
) -> jint {
    let name: String = env.get_string(&name).unwrap().into();
    let email: String = env.get_string(&email).unwrap().into();
    let message: String = env.get_string(&message).unwrap().into();

    unwrap_or_log!(libgit2::commit_all(&name, &email, &message), "commit_all");

    OK
}

#[unsafe(no_mangle)]
pub extern "C" fn Java_io_github_wiiznokes_gitnote_manager_GitManagerKt_currentSignatureLib<
    'caller,
>(
    mut env: EnvUnowned<'caller>,
    _class: JClass<'caller>,
) -> jobject {
    let signature = match libgit2::signature() {
        Some(signature) => signature,
        None => return std::ptr::null_mut(),
    };

    let name_jstring = env.new_string(&signature.0).unwrap();
    let email_jstring = env.new_string(&signature.1).unwrap();

    let pair_class = env.find_class("kotlin/Pair").unwrap();

    let pair_obj = env
        .new_object(
            &pair_class,
            "(Ljava/lang/Object;Ljava/lang/Object;)V",
            &[(&name_jstring).into(), (&email_jstring).into()],
        )
        .unwrap();

    pair_obj.into_raw()
}
#[unsafe(no_mangle)]
pub extern "C" fn Java_io_github_wiiznokes_gitnote_manager_GitManagerKt_pushLib<'caller>(
    mut env: EnvUnowned<'caller>,
    _class: JClass<'caller>,
    cred: JString<'caller>,
) -> jint {
    let cred = Cred::from_jni(&mut env, &cred).unwrap();
    unwrap_or_log!(libgit2::push(cred), "push");
    OK
}

#[unsafe(no_mangle)]
pub extern "C" fn Java_io_github_wiiznokes_gitnote_manager_GitManagerKt_pullLib<'caller>(
    mut env: EnvUnowned<'caller>,
    _class: JClass<'caller>,
    cred: JString<'caller>,
    name: JString<'caller>,
    email: JString<'caller>,
) -> jint {
    let cred = Cred::from_jni(&mut env, &cred).unwrap();
    let name: String = env.get_string(&name).unwrap().into();
    let email: String = env.get_string(&email).unwrap().into();
    let author = GitAuthor { name, email };
    unwrap_or_log!(libgit2::pull(cred, &author), "pull");
    OK
}

#[unsafe(no_mangle)]
pub extern "C" fn Java_io_github_wiiznokes_gitnote_manager_GitManagerKt_freeLib(
    _env: EnvUnowned,
    _class: JClass,
) {
}

#[unsafe(no_mangle)]
pub extern "C" fn Java_io_github_wiiznokes_gitnote_manager_GitManagerKt_closeRepoLib(
    _env: EnvUnowned,
    _class: JClass,
) {
    libgit2::close();
}
#[unsafe(no_mangle)]
pub extern "C" fn Java_io_github_wiiznokes_gitnote_manager_GitManagerKt_isChangeLib(
    _env: EnvUnowned,
    _class: JClass,
) -> jint {
    let is_change = unwrap_or_log!(libgit2::is_change(), "is_change");

    is_change as jint
}

#[unsafe(no_mangle)]
pub extern "C" fn Java_io_github_wiiznokes_gitnote_manager_GitManagerKt_getTimestampsLib<
    'caller,
>(
    mut env: EnvUnowned<'caller>,
    _class: JClass<'caller>,
    j_map: JObject<'caller>,
) -> jint {
    let timestamps = unwrap_or_log!(libgit2::get_timestamps(), "get_timestamps");

    if let Err(e) = get_timestamps_jni(&mut env, &j_map, timestamps.iter()) {
        error!("get_timestamps_jni: {e}");
        return -1;
    }

    OK
}

fn get_timestamps_jni<'caller, 'a>(
    env: &mut EnvUnowned<'caller>,
    j_map: &JObject<'caller>,
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
    'caller,
>(
    mut env: EnvUnowned<'caller>,
    _class: JClass<'caller>,
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

#[unsafe(no_mangle)]
pub extern "C" fn Java_io_github_wiiznokes_gitnote_manager_MimeTypeManagerKt_extensionTypeLib<
    'caller,
>(
    mut env: EnvUnowned<'caller>,
    _class: JClass<'caller>,
    extension: JString<'caller>,
) -> jint {
    let extension: String = env.get_string(&extension).unwrap().into();

    match mime_types::extension_type(extension.as_str()) {
        Some(ext_type) => ext_type as jint,
        None => 0,
    }
}

#[unsafe(no_mangle)]
pub extern "C" fn Java_io_github_wiiznokes_gitnote_manager_MimeTypeManagerKt_isExtensionSupported<
    'caller,
>(
    mut env: EnvUnowned<'caller>,
    _class: JClass<'caller>,
    extension: JString<'caller>,
) -> jboolean {
    let extension: String = env.get_string(&extension).unwrap().into();

    mime_types::is_extension_supported(extension.as_str()).into()
}

#[unsafe(no_mangle)]
pub extern "C" fn Java_io_github_wiiznokes_gitnote_manager_GitManagerKt_getUrlInfoLib<'caller>(
    mut env: EnvUnowned<'caller>,
    _class: JClass<'caller>,
    url: JString<'caller>,
) -> jobject {
    let url: String = env.get_string(&url).unwrap().into();

    let url_info = match url::parse_url(&url) {
        Ok(info) => info,
        Err(e) => {
            error!("{e}");
            return std::ptr::null_mut();
        }
    };

    let is_ssh = url_info.kind == url::UrlKind::Ssh;

    let boolean_class = env.find_class("java/lang/Boolean").unwrap();

    let obj = env
        .new_object(
            boolean_class,
            "(Z)V",
            &[JValue::Bool(if is_ssh { true } else { false })],
        )
        .unwrap();

    obj.into_raw()
}
