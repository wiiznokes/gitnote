use std::sync::{LazyLock, Mutex};

use git2::{
    CertificateCheckStatus, Cred, FetchOptions, IndexAddOption, Progress, PushOptions,
    RemoteCallbacks, Repository, Signature, StatusOptions,
};

use crate::{Creeds, Error};

mod merge;

const REMOTE: &str = "origin";
const BRANCH: &str = "main";

static REPO: LazyLock<Mutex<Option<Repository>>> = LazyLock::new(|| Mutex::new(None));

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

pub fn clone_repo(repo_path: &str, remote_url: &str, creeds: Option<Creeds>) -> Result<(), Error> {
    let mut callbacks = RemoteCallbacks::new();

    callbacks.certificate_check(|_cert, _| Ok(CertificateCheckStatus::CertificateOk));

    if let Some(creeds) = creeds {
        callbacks.credentials(move |_url, _username_from_url, _allowed_types| {
            Cred::userpass_plaintext(&creeds.username, &creeds.password)
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

pub fn push(creeds: Option<Creeds>) -> Result<(), Error> {
    let repo = REPO.lock().expect("repo lock");
    let repo = repo.as_ref().expect("repo");

    let mut remote = repo
        .find_remote(REMOTE)
        .map_err(|e| Error::git2(e, "find_remote"))?;

    let refspecs = [format!("refs/heads/{BRANCH}:refs/heads/{BRANCH}")];

    let mut callbacks = RemoteCallbacks::new();

    callbacks.certificate_check(|_cert, _| Ok(CertificateCheckStatus::CertificateOk));

    if let Some(creeds) = creeds {
        callbacks.credentials(move |_url, _username_from_url, _allowed_types| {
            Cred::userpass_plaintext(&creeds.username, &creeds.password)
        });
    }

    let mut push_opts = PushOptions::new();
    push_opts.remote_callbacks(callbacks);

    remote
        .push(&refspecs, Some(&mut push_opts))
        .map_err(|e| Error::git2(e, "push"))?;

    Ok(())
}

pub fn pull(creeds: Option<Creeds>) -> Result<(), Error> {
    let repo = REPO.lock().expect("repo lock");
    let repo = repo.as_ref().expect("repo");

    let mut remote = repo
        .find_remote(REMOTE)
        .map_err(|e| Error::git2(e, "find_remote"))?;

    let mut callbacks = RemoteCallbacks::new();

    callbacks.certificate_check(|_cert, _| Ok(CertificateCheckStatus::CertificateOk));

    if let Some(creeds) = creeds {
        callbacks.credentials(move |_url, _username_from_url, _allowed_types| {
            Cred::userpass_plaintext(&creeds.username, &creeds.password)
        });
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
