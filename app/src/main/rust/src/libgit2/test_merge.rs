use git2::{Commit, ObjectType, Repository, Signature, build::CheckoutBuilder};
use std::fs;
use std::path::Path;

// --- Helpers de base ---

fn signature() -> Signature<'static> {
    Signature::now("Moi", "test@example.com").unwrap()
}

/// Bascule sur une branche en mettant à jour physiquement les fichiers (Checkout)
fn switch_to_branch(repo: &Repository, branch_name: &str) {
    let ref_name = format!("refs/heads/{}", branch_name);
    let obj = repo.revparse_single(&ref_name).unwrap()
        .peel_to_commit().unwrap();
    
    let mut opts = CheckoutBuilder::new();
    opts.force(); // Écrase les reliquats pour garantir un état propre
    
    repo.checkout_tree(obj.as_object(), Some(&mut opts)).unwrap();
    repo.set_head(&ref_name).unwrap();
}

/// Crée un commit proprement en partant de l'état actuel de l'index
pub fn commit_current_state(repo: &Repository, message: &str) -> git2::Oid {
    let sig = signature();
    let mut index = repo.index().unwrap();
    let tree_id = index.write_tree().unwrap();
    let tree = repo.find_tree(tree_id).unwrap();

    // Récupère le parent actuel (HEAD)
    let parent = repo.head().ok()
        .and_then(|h| h.target())
        .and_then(|id| repo.find_commit(id).ok());

    let parents = match &parent {
        Some(c) => vec![c],
        None => vec![],
    };

    repo.commit(Some("HEAD"), &sig, &sig, message, &tree, &parents).unwrap()
}

/// Ajoute un fichier physiquement ET dans l'index
fn add_file(repo: &Repository, filename: &str, content: &str) {
    let path = repo.workdir().unwrap().join(filename);
    fs::write(path, content).unwrap();
    
    let mut index = repo.index().unwrap();
    index.add_path(Path::new(filename)).unwrap();
    index.write().unwrap();
}

// --- Le Test ---

#[test]
fn test_clean_merge_flow() {
    let path = "repo_test/clean_repo";
    let _ = fs::remove_dir_all(path); // Nettoyage initial
    let repo = Repository::init(path).unwrap();

    // 1. Premier commit sur Master
    add_file(&repo, "file1.txt", "Contenu Initial\n");
    let oid1 = commit_current_state(&repo, "Initial commit on master");

    // 2. Créer et passer sur la branche 'dev'
    let commit1 = repo.find_commit(oid1).unwrap();
    repo.branch("dev", &commit1, false).unwrap();
    switch_to_branch(&repo, "dev");

    // 3. Commit sur 'dev' (file2.txt)
    add_file(&repo, "file2.txt", "Contenu Dev\n");
    commit_current_state(&repo, "Add file2 on dev");

    // 4. Retour sur 'master' et commit (file3.txt)
    switch_to_branch(&repo, "master");
    add_file(&repo, "file3.txt", "Contenu Master\n");
    commit_current_state(&repo, "Add file3 on master");

    // 5. Merge 'dev' dans 'master'
    let annotated_dev = {
        let dev_ref = repo.find_reference("refs/heads/dev").unwrap();
        repo.reference_to_annotated_commit(&dev_ref).unwrap()
    };

    // Ici on appelle votre fonction do_merge
    // Note: Le merge devrait être "Fast-forward" ou "Clean" car les fichiers sont différents
    let author = crate::GitAuthor::from(signature());
    crate::libgit2::merge::do_merge(&repo, "dev", annotated_dev, &author).expect("Merge failed");

    // Vérification finale
    assert!(repo.workdir().unwrap().join("file1.txt").exists());
    assert!(repo.workdir().unwrap().join("file2.txt").exists());
    assert!(repo.workdir().unwrap().join("file3.txt").exists());
}