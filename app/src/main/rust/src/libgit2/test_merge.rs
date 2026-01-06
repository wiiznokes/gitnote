use git2::{Repository, Signature, build::CheckoutBuilder};
use std::path::Path;
use std::{fs, io};

use crate::GitAuthor;
use crate::libgit2::merge::do_merge;

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

fn switch_to_branch(repo: &Repository, branch_name: &str) {
    let ref_name = format!("refs/heads/{}", branch_name);
    let obj = repo
        .revparse_single(&ref_name)
        .unwrap()
        .peel_to_commit()
        .unwrap();

    let mut opts = CheckoutBuilder::new();
    opts.force();

    repo.checkout_tree(obj.as_object(), Some(&mut opts))
        .unwrap();
    repo.set_head(&ref_name).unwrap();
}

pub fn commit_current_state(repo: &Repository, message: &str) -> git2::Oid {
    let sig = signature();
    let mut index = repo.index().unwrap();
    let tree_id = index.write_tree().unwrap();
    let tree = repo.find_tree(tree_id).unwrap();

    // Récupère le parent actuel (HEAD)
    let parent = repo
        .head()
        .ok()
        .and_then(|h| h.target())
        .and_then(|id| repo.find_commit(id).ok());

    let parents = match &parent {
        Some(c) => vec![c],
        None => vec![],
    };

    repo.commit(Some("HEAD"), &sig, &sig, message, &tree, &parents)
        .unwrap()
}

fn add_file(repo: &Repository, filename: &str, content: &str) {
    let path = repo.workdir().unwrap().join(filename);
    fs::write(path, content).unwrap();

    let mut index = repo.index().unwrap();
    index.add_path(Path::new(filename)).unwrap();
    index.write().unwrap();
}

fn assert_content(repo: &Repository, path: &str, content: &str) {
    let path = repo.workdir().unwrap().join(path);

    let real_content = fs::read_to_string(&path).unwrap();

    assert_eq!(real_content, content);
}

#[test]
fn test_clean_merge_flow() {
    let path = "repo_test/clean_repo";
    let _ = clear_dir(path);
    let repo = Repository::init(path).unwrap();

    // 1. Premier commit sur Master
    add_file(&repo, "file1.txt", "hello");
    let oid1 = commit_current_state(&repo, "Initial commit on master");

    // 2. Créer et passer sur la branche 'dev'
    let commit1 = repo.find_commit(oid1).unwrap();
    repo.branch("dev", &commit1, false).unwrap();
    switch_to_branch(&repo, "dev");

    // 3. Commit sur 'dev' (file2.txt)
    add_file(&repo, "file2.txt", "hello");
    commit_current_state(&repo, "Add file2 on dev");

    // 4. Retour sur 'master' et commit (file3.txt)
    switch_to_branch(&repo, "master");
    add_file(&repo, "file1.txt", "hello world");
    commit_current_state(&repo, "Modif file1 on master");

    // 5. Merge 'dev' dans 'master'
    let annotated_dev = {
        let dev_ref = repo.find_reference("refs/heads/dev").unwrap();
        repo.reference_to_annotated_commit(&dev_ref).unwrap()
    };

    let author = GitAuthor::from(signature());
    do_merge(&repo, "dev", annotated_dev, &author).expect("Merge failed");

    assert_content(&repo, "file1.txt", "hello world");
    assert_content(&repo, "file2.txt", "hello");
}

#[test]
fn test_clean_merge_flow2() {
    let path = "repo_test/clean_repo2";
    let _ = clear_dir(path);
    let repo = Repository::init(path).unwrap();

    // 1. Premier commit sur Master
    add_file(&repo, "file1.txt", "Contenu Initial");
    let oid1 = commit_current_state(&repo, "Initial commit on master");

    // 2. Créer et passer sur la branche 'dev'
    let commit1 = repo.find_commit(oid1).unwrap();
    repo.branch("dev", &commit1, false).unwrap();
    switch_to_branch(&repo, "dev");

    // 3. Commit sur 'dev' (file2.txt)
    add_file(&repo, "file2.txt", "Contenu Dev");
    commit_current_state(&repo, "Add file2 on dev");

    // 4. Retour sur 'master' et commit (file3.txt)
    switch_to_branch(&repo, "master");
    add_file(&repo, "file3.txt", "Contenu Master");
    commit_current_state(&repo, "Add file3 on master");

    // 5. Merge 'dev' dans 'master'
    let annotated_dev = {
        let dev_ref = repo.find_reference("refs/heads/dev").unwrap();
        repo.reference_to_annotated_commit(&dev_ref).unwrap()
    };

    let author = GitAuthor::from(signature());
    do_merge(&repo, "dev", annotated_dev, &author).expect("Merge failed");

    assert_content(&repo, "file1.txt", "Contenu Initial");
    assert_content(&repo, "file2.txt", "Contenu Dev");
    assert_content(&repo, "file3.txt", "Contenu Master");
}
