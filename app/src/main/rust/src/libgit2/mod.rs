use std::{
    collections::HashMap,
    fs,
    path::Path,
    str::FromStr,
    sync::{LazyLock, Mutex, OnceLock},
};

use git2::{
    CertificateCheckStatus, FetchOptions, IndexAddOption, Progress, PushOptions, RemoteCallbacks,
    Repository, Signature, StatusOptions, TreeWalkMode, TreeWalkResult,
};

use crate::{Cred, Error, ProgressCB, mime_types::is_extension_supported};

mod merge;

#[cfg(test)]
mod test;

const REMOTE: &str = "origin";
const BRANCH: &str = "main";

static REPO: LazyLock<Mutex<Option<Repository>>> = LazyLock::new(|| Mutex::new(None));

// https://github.com/libgit2/libgit2/pull/7056
static HOME_PATH: OnceLock<String> = OnceLock::new();

fn apply_ssh_workaround(clone: bool) {
    let home = HOME_PATH.get().unwrap();

    if clone {
        unsafe {
            std::env::set_var("HOME", home);
        }
    } else {
        let c_path = std::ffi::CString::from_str(home).expect("CString::new failed");

        unsafe {
            libgit2_sys::git_libgit2_opts(
                libgit2_sys::GIT_OPT_SET_HOMEDIR as std::ffi::c_int,
                c_path.as_ptr(),
            )
        };
    }

    if let Err(e) = std::fs::create_dir_all(format!("{home}/.ssh")) {
        error!("{e}");
    }
    if let Err(e) = std::fs::File::create(format!("{home}/.ssh/known_hosts")) {
        error!("{e}");
    }
}

pub fn init_lib(home_path: String) {
    info!("home_path: {home_path}");
    let _ = HOME_PATH.set(home_path.clone());

    unsafe {
        std::env::set_var("HOME", &home_path);
    }

    let git_config_path = Path::new(&home_path).join(".gitconfig");

    let git_config_content = "[safe]\n\tdirectory = *";

    match fs::exists(&git_config_path) {
        Ok(true) => {}
        Ok(false) => {
            if let Err(e) = fs::create_dir_all(git_config_path.parent().unwrap()) {
                error!("gitconfig: {e}");
            }

            if let Err(e) = fs::write(&git_config_path, git_config_content) {
                error!("gitconfig: {e}");
            } else {
                debug!("successfully written the gitconfig file")
            }
        }
        Err(e) => {
            error!("gitconfig: {e}");
        }
    }
}

pub fn create_repo(repo_path: &str) -> Result<(), Error> {
    let repo = Repository::init(repo_path).map_err(|e| Error::git2(e, "Repository::init"))?;

    REPO.lock().unwrap().replace(repo);

    Ok(())
}

pub fn open_repo(repo_path: &str) -> Result<(), Error> {
    let repo = Repository::open(repo_path).map_err(|e| Error::git2(e, "Repository::open"))?;

    REPO.lock().unwrap().replace(repo);

    Ok(())
}

fn credential_helper(cred: &Cred) -> Result<git2::Cred, git2::Error> {
    match cred {
        Cred::UserPassPlainText { username, password } => {
            git2::Cred::userpass_plaintext(username, password)
        }
        Cred::Ssh {
            username,
            private_key,
            public_key,
        } => git2::Cred::ssh_key_from_memory(username, Some(public_key), private_key, None),
    }
}

pub fn clone_repo(
    repo_path: &str,
    remote_url: &str,
    cred: Option<Cred>,
    mut cb: ProgressCB,
) -> Result<(), Error> {
    apply_ssh_workaround(true);
    let mut callbacks = RemoteCallbacks::new();

    callbacks.certificate_check(|_cert, _| Ok(CertificateCheckStatus::CertificateOk));

    if let Some(cred) = cred {
        callbacks
            .credentials(move |_url, _username_from_url, _allowed_types| credential_helper(&cred));
    }


    callbacks.transfer_progress(|stats: Progress| {
        let progress = stats.indexed_objects() as f32 / stats.total_objects() as f32 * 100.;

        cb.progress(progress as i32)
    });

    let mut fetch_options = FetchOptions::new();
    fetch_options.remote_callbacks(callbacks);

    let mut builder = git2::build::RepoBuilder::new();

    let repo = builder
        .fetch_options(fetch_options)
        .clone(remote_url, std::path::Path::new(&repo_path))
        .map_err(|e| Error::git2(e, "clone"))?;

    REPO.lock().unwrap().replace(repo);

    Ok(())
}

pub fn last_commit() -> Option<String> {
    let repo = REPO.lock().expect("repo lock");
    let repo = repo.as_ref().expect("repo");

    // new repo have no commit, so this function can fail
    let head = repo.refname_to_id("HEAD").ok()?;

    Some(head.to_string())
}

pub fn commit_all(username: &str, message: &str) -> Result<(), Error> {
    let repo = REPO.lock().expect("repo lock");
    let repo = repo.as_ref().expect("repo");

    let mut index = repo.index().map_err(|e| Error::git2(e, "index"))?;

    index
        .add_all(["*"].iter(), IndexAddOption::DEFAULT, None)
        .map_err(|e| Error::git2(e, "add_all"))?;

    // Write index to disk
    index.write().map_err(|e| Error::git2(e, "write"))?;

    // Write tree
    let tree_oid = index
        .write_tree()
        .map_err(|e| Error::git2(e, "write_tree"))?;

    let tree = repo
        .find_tree(tree_oid)
        .map_err(|e| Error::git2(e, "find_tree"))?;

    // Get HEAD commit as parent, and Allow initial commit
    let parent_commit = repo.head().and_then(|r| r.peel_to_commit()).ok();

    let sig = Signature::now(username, username).map_err(|e| Error::git2(e, "Signature::now"))?;

    // Create commit
    match parent_commit {
        Some(ref parent) => repo.commit(Some("HEAD"), &sig, &sig, message, &tree, &[parent]),
        None => repo.commit(Some("HEAD"), &sig, &sig, message, &tree, &[]),
    }
    .map(|_| ())
    .map_err(|e| Error::git2(e, "commit"))
}

pub fn push(cred: Option<Cred>) -> Result<(), Error> {
    apply_ssh_workaround(false);
    let repo = REPO.lock().expect("repo lock");
    let repo = repo.as_ref().expect("repo");

    let mut remote = repo
        .find_remote(REMOTE)
        .map_err(|e| Error::git2(e, "find_remote"))?;

    let refspecs = [format!("refs/heads/{BRANCH}:refs/heads/{BRANCH}")];

    let mut callbacks = RemoteCallbacks::new();

    callbacks.certificate_check(|_cert, _| Ok(CertificateCheckStatus::CertificateOk));

    if let Some(cred) = cred {
        callbacks
            .credentials(move |_url, _username_from_url, _allowed_types| credential_helper(&cred));
    }

    let mut push_opts = PushOptions::new();
    push_opts.remote_callbacks(callbacks);

    remote
        .push(&refspecs, Some(&mut push_opts))
        .map_err(|e| Error::git2(e, "push"))?;

    Ok(())
}

pub fn pull(cred: Option<Cred>) -> Result<(), Error> {
    apply_ssh_workaround(false);
    let repo = REPO.lock().expect("repo lock");
    let repo = repo.as_ref().expect("repo");

    let mut remote = repo
        .find_remote(REMOTE)
        .map_err(|e| Error::git2(e, "find_remote"))?;

    let mut callbacks = RemoteCallbacks::new();

    callbacks.certificate_check(|_cert, _| Ok(CertificateCheckStatus::CertificateOk));

    if let Some(cred) = cred {
        callbacks
            .credentials(move |_url, _username_from_url, _allowed_types| credential_helper(&cred));
    }

    let mut fetch_options = FetchOptions::new();
    fetch_options.remote_callbacks(callbacks);

    remote
        .fetch(&[] as &[&str], Some(&mut fetch_options), None)
        .map_err(|e| Error::git2(e, "fetch"))?;

    let fetch_head = repo
        .find_reference("FETCH_HEAD")
        .map_err(|e| Error::git2(e, "find_reference"))?;

    let commit = repo
        .reference_to_annotated_commit(&fetch_head)
        .map_err(|e| Error::git2(e, "reference_to_annotated_commit"))?;

    merge::do_merge(repo, BRANCH, commit).map_err(|e| Error::git2(e, "do_merge"))?;

    Ok(())
}

pub fn close() {
    let mut repo = REPO.lock().expect("repo lock");
    repo.take();
}

pub fn is_change() -> Result<bool, Error> {
    let repo = REPO.lock().expect("repo lock");
    let repo = repo.as_ref().expect("repo");

    let mut opts = StatusOptions::new();
    opts.include_untracked(true).recurse_untracked_dirs(true);

    let statuses = repo
        .statuses(Some(&mut opts))
        .map_err(|e| Error::git2(e, "statuses"))?;

    let count = statuses.len();

    Ok(count > 0)
}

fn find_timestamp(repo: &Repository, file_path: String) -> anyhow::Result<Option<(String, i64)>> {
    // Use revwalk to find the last commit that touched this path
    let mut revwalk = repo.revwalk()?;
    revwalk.push_head()?;
    revwalk.set_sorting(git2::Sort::TIME)?;

    for oid_result in revwalk {
        let oid = oid_result?;
        let commit = repo.find_commit(oid)?;

        // Check if this commit touches the file
        if commit
            .tree()?
            .get_path(std::path::Path::new(&file_path))
            .is_ok()
        {
            // We want to check if this commit modified the file_path compared to its parent(s)
            let parent = commit.parents().next();

            let is_modified = match parent {
                Some(parent) => {
                    // Compare trees between commit and its first parent
                    let parent_tree = parent.tree()?;
                    let current_tree = commit.tree()?;

                    let diff = repo.diff_tree_to_tree(
                        Some(&parent_tree),
                        Some(&current_tree),
                        Some(git2::DiffOptions::new().pathspec(&file_path)),
                    )?;

                    diff.deltas().len() > 0
                }
                // Initial commit, consider as modified
                None => true,
            };

            if is_modified {
                return Ok(Some((file_path, commit.time().seconds() * 1000)));
            }
        }
    }
    Ok(None)
}

pub fn get_timestamps() -> Result<HashMap<String, i64>, Error> {
    let repo = REPO.lock().expect("repo lock");
    let repo = repo.as_ref().expect("repo");

    // Get HEAD commit
    let head = repo.head()?.peel_to_commit()?;

    let mut file_timestamps = HashMap::new();

    // Get the list of files in the repo at HEAD
    let tree = head.tree()?;

    tree.walk(TreeWalkMode::PreOrder, |root, entry| {
        if entry.kind() == Some(git2::ObjectType::Blob)
            && let Some(name) = entry.name()
            && let Some(extension) = Path::new(name).extension()
            && let Some(extension) = extension.to_str()
            && is_extension_supported(extension)
        {
            let path = format!("{root}{name}");
            if let Ok(Some((path, time))) = find_timestamp(repo, path) {
                file_timestamps.insert(path, time);
            }
        }
        TreeWalkResult::Ok
    })?;

    Ok(file_timestamps)
}
