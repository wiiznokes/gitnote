use std::{fs, io, path::Path};

use git2::{AnnotatedCommit, Oid, Repository, Signature};

use crate::{GitAuthor, libgit2::merge::do_merge};

fn clear_dir<P: AsRef<Path>>(path: P) -> io::Result<()> {
    for entry in fs::read_dir(path)? {
        let entry = entry?;
        let path = entry.path();

        if path.is_dir() {
            fs::remove_dir_all(path)?;
        } else {
            fs::remove_file(path)?;
        }
    }
    Ok(())
}

fn signature() -> Signature<'static> {
    Signature::now("Test", "test@example.com").unwrap()
}

fn create_initial_commit(repo: &Repository) -> Oid {
    let sig = signature();

    let tree_id = {
        let mut index = repo.index().unwrap();
        index.write_tree().unwrap()
    };

    let tree = repo.find_tree(tree_id).unwrap();

    repo.commit(Some("HEAD"), &sig, &sig, "initial commit", &tree, &[])
        .unwrap()
}

fn create_branch_commit(repo: &Repository, branch: &str, parent: Oid) -> Oid {
    let sig = signature();
    let parent = repo.find_commit(parent).unwrap();

    let tree_id = {
        let mut index = repo.index().unwrap();
        index.write_tree().unwrap()
    };
    let tree = repo.find_tree(tree_id).unwrap();

    let commit_id = repo
        .commit(None, &sig, &sig, "branch commit", &tree, &[&parent])
        .unwrap();

    repo.branch(branch, &repo.find_commit(commit_id).unwrap(), false)
        .unwrap();

    commit_id
}

fn annotated_commit(repo: &Repository, oid: Oid) -> AnnotatedCommit<'_> {
    repo.find_annotated_commit(oid).unwrap()
}

pub fn commit_file(
    repo: &Repository,
    path: &Path,
    contents: &str,
    message: &str,
    branch: &str,
) -> Oid {
    repo.set_head(&format!("refs/heads/{branch}")).unwrap();
    repo.checkout_head(None).unwrap();

    // Write file to working tree
    let full_path = repo.workdir().unwrap().join(path);
    fs::create_dir_all(full_path.parent().unwrap()).unwrap();
    fs::write(&full_path, contents).unwrap();

    // Stage file
    let mut index = repo.index().unwrap();
    index.add_path(path).unwrap();
    let tree_id = index.write_tree().unwrap();
    let tree = repo.find_tree(tree_id).unwrap();

    // Signature
    let sig = signature();

    // Parent commit (if any)
    let parents = match repo.head() {
        Ok(head) => vec![repo.find_commit(head.target().unwrap()).unwrap()],
        Err(_) => vec![], // initial commit
    };

    let parent_refs: Vec<&git2::Commit> = parents.iter().collect();

    // Commit
    let oid = repo
        .commit(Some("HEAD"), &sig, &sig, message, &tree, &parent_refs)
        .unwrap();

    oid
}

#[test]
fn do_test() {
    let path = "repo_test/repo1";

    let _ = clear_dir(path);

    let _ = fs::create_dir_all(path);

    let repo = Repository::init(path).unwrap();

    let base = create_initial_commit(&repo);

    let oid1 = commit_file(&repo, Path::new("file1.txt"), "hello", "file 1", "master");

    let branch_commit = create_branch_commit(&repo, "dev", base);

    let oid2 = commit_file(
        &repo,
        Path::new("file1.txt"),
        "hello world",
        "update file 1",
        "master",
    );

    let oid3 = commit_file(&repo, Path::new("file2.txt"), "hello", "file 2", "dev");

    let author = GitAuthor::from(signature());

    let annotated = annotated_commit(&repo, branch_commit);

    do_merge(&repo, "dev", annotated, &author).unwrap();
}
