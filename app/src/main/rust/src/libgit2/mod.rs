use std::{
    collections::HashMap,
    sync::{LazyLock, Mutex},
};

use git2::{
    CertificateCheckStatus, FetchOptions, IndexAddOption, Progress, PushOptions,
    RemoteCallbacks, Repository, Signature, StatusOptions, TreeWalkMode, TreeWalkResult,
};

use crate::{Cred, Error, ProgressCB};

mod merge;

#[cfg(test)]
mod test;

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

fn credential_helper(cred: &Cred) -> Result<git2::Cred, git2::Error> {
    match cred {
        Cred::UserPassPlainText { username, password } => {
            git2::Cred::userpass_plaintext(&username, &password)
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
    let mut callbacks = RemoteCallbacks::new();

    callbacks.certificate_check(|_cert, _| Ok(CertificateCheckStatus::CertificateOk));

    if let Some(cred) = cred {
        callbacks.credentials(move |_url, _username_from_url, _allowed_types| {
            credential_helper(&cred)
        });
    }

    callbacks.transfer_progress(|stats: Progress| {
        let progress = stats.indexed_objects() as f32 / stats.total_objects() as f32 * 100.;

        cb.progress(progress as i32);

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

pub fn push(cred: Option<Cred>) -> Result<(), Error> {
    let repo = REPO.lock().expect("repo lock");
    let repo = repo.as_ref().expect("repo");

    let mut remote = repo
        .find_remote(REMOTE)
        .map_err(|e| Error::git2(e, "find_remote"))?;

    let refspecs = [format!("refs/heads/{BRANCH}:refs/heads/{BRANCH}")];

    let mut callbacks = RemoteCallbacks::new();

    callbacks.certificate_check(|_cert, _| Ok(CertificateCheckStatus::CertificateOk));

    if let Some(cred) = cred {
        callbacks.credentials(move |_url, _username_from_url, _allowed_types| {
            credential_helper(&cred)
        });
    }

    let mut push_opts = PushOptions::new();
    push_opts.remote_callbacks(callbacks);

    remote
        .push(&refspecs, Some(&mut push_opts))
        .map_err(|e| Error::git2(e, "push"))?;

    Ok(())
}

pub fn pull(cred: Option<Cred>) -> Result<(), Error> {
    let repo = REPO.lock().expect("repo lock");
    let repo = repo.as_ref().expect("repo");

    let mut remote = repo
        .find_remote(REMOTE)
        .map_err(|e| Error::git2(e, "find_remote"))?;

    let mut callbacks = RemoteCallbacks::new();

    callbacks.certificate_check(|_cert, _| Ok(CertificateCheckStatus::CertificateOk));

    if let Some(cred) = cred {
        callbacks.credentials(move |_url, _username_from_url, _allowed_types| {
            credential_helper(&cred)
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

pub fn get_timestamps() -> Result<HashMap<String, i64>, Error> {
    let repo = REPO.lock().expect("repo lock");
    let repo = repo.as_ref().expect("repo");

    // Get HEAD commit
    let head = repo.head()?.peel_to_commit()?;

    // We'll build a map from file path -> last commit time (u64)
    let mut file_timestamps = HashMap::new();

    // Get the list of files in the repo at HEAD
    let tree = head.tree()?;

    // Collect all file paths
    let mut file_paths = Vec::new();
    tree.walk(TreeWalkMode::PreOrder, |root, entry| {
        if let Some(name) = entry.name() {
            let full_path = format!("{}{}", root, name);
            if entry.kind() == Some(git2::ObjectType::Blob) {
                file_paths.push(full_path);
            }
        }
        TreeWalkResult::Ok
    })?;

    // For each file, find last commit that modified it
    for file_path in file_paths {
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
                let parents = commit.parents().collect::<Vec<_>>();
                let is_modified = if parents.is_empty() {
                    // Initial commit, consider as modified
                    true
                } else {
                    // Compare trees between commit and its first parent
                    let parent_tree = parents[0].tree()?;
                    let current_tree = commit.tree()?;

                    let diff = repo.diff_tree_to_tree(
                        Some(&parent_tree),
                        Some(&current_tree),
                        Some(git2::DiffOptions::new().pathspec(&file_path)),
                    )?;

                    diff.deltas().len() > 0
                };

                if is_modified {
                    // Store commit time
                    file_timestamps.insert(file_path.clone(), commit.time().seconds());
                    break;
                }
            }
        }
    }

    Ok(file_timestamps)
}
