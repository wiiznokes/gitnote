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
    Signature::now("Moi", "test@example.com").unwrap()
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

fn create_branch(repo: &Repository, branch: &str, parent: Oid) {
    let parent = repo.find_commit(parent).unwrap();
    repo.branch(branch, &parent, false).unwrap();
}

fn annotated_commit<'a>(repo: &'a Repository, branch: &str) -> AnnotatedCommit<'a> {
    let dev_ref = repo
        .find_reference(&format!("refs/heads/{branch}"))
        .unwrap();
    repo.reference_to_annotated_commit(&dev_ref).unwrap()
}

pub fn commit_file(
    repo: &Repository,
    path: &Path,
    contents: &str,
    message: &str,
    branch: &str,
) -> Oid {
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
    let parents = match repo.find_reference(&format!("refs/heads/{branch}")) {
        Ok(r) => vec![repo.find_commit(r.target().unwrap()).unwrap()],
        Err(_) => vec![],
    };

    let parent_refs: Vec<&git2::Commit> = parents.iter().collect();

    // Commit
    let oid = repo
        .commit(
            Some(&format!("refs/heads/{branch}")),
            &sig,
            &sig,
            message,
            &tree,
            &parent_refs,
        )
        .unwrap();

    oid
}

fn set_head(repo: &Repository, branch: &str) {
    repo.set_head(&format!("refs/heads/{branch}")).unwrap();
    repo.checkout_head(None).unwrap();
}

#[test]
fn do_test() {
    let path = "repo_test/repo1";

    let _ = clear_dir(path);

    let _ = fs::create_dir_all(path);

    let repo = Repository::init(path).unwrap();

    let oid1 = commit_file(&repo, Path::new("file1.txt"), "hello", "file 1", "master");

    create_branch(&repo, "dev", oid1);

    let _oid2 = commit_file(
        &repo,
        Path::new("file1.txt"),
        "hello world",
        "update file 1",
        "master",
    );

    let _oid3 = commit_file(&repo, Path::new("file2.txt"), "hello", "file 2", "dev");

    let author = GitAuthor::from(signature());

    let annotated = annotated_commit(&repo, "dev");

    set_head(&repo, "master");
    do_merge(&repo, "dev", annotated, &author).unwrap();
}
