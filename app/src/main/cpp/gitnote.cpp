#include <jni.h>
#include "logging.h"
#include <git2.h>
#include "merge.h"
#include "remote.h"
#include "CallbackHandler.h"

git_repository *repo = NULL;


extern "C"
JNIEXPORT jint JNICALL
Java_com_example_gitnote_manager_GitManagerKt_initLib(JNIEnv *, jclass) {
    int err = git_libgit2_init();
    CHECK_LG2_RET(err, "initLibGit2");
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_example_gitnote_manager_GitManagerKt_createRepoLib(JNIEnv *env, jclass,
                                                            jstring repoPathObj) {
    const char *repo_path = env->GetStringUTFChars(repoPathObj, nullptr);
    int err = git_repository_init(&repo, repo_path, false);
    env->ReleaseStringUTFChars(repoPathObj, repo_path);
    CHECK_LG2_RET(err, "createRepo");
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_example_gitnote_manager_GitManagerKt_openRepoLib(JNIEnv *env, jclass,
                                                          jstring repoPathObj) {
    const char *repo_path = env->GetStringUTFChars(repoPathObj, nullptr);
    int err = git_repository_open(&repo, repo_path);
    env->ReleaseStringUTFChars(repoPathObj, repo_path);
    CHECK_LG2_RET(err, "openRepo");
}



extern "C"
JNIEXPORT jint JNICALL
Java_com_example_gitnote_manager_GitManagerKt_cloneRepoLib(JNIEnv *env, jclass,
                                                           jstring repoPathObj,
                                                           jstring remoteUrlObj,
                                                           jstring usernameObj,
                                                           jstring passwordObj,
                                                           jobject progressCallback) {

    const char *repo_path = env->GetStringUTFChars(repoPathObj, nullptr);
    const char *remote_url = env->GetStringUTFChars(remoteUrlObj, nullptr);
    const char *username;
    const char *password;

    if (usernameObj != nullptr && passwordObj != nullptr) {
        username = env->GetStringUTFChars(usernameObj, nullptr);
        password = env->GetStringUTFChars(passwordObj, nullptr);
    }

    CallbackHandler callbackHandler = CallbackHandler(env, progressCallback);

    int err = clone(&repo, repo_path, remote_url, username, password, callbackHandler);

    env->ReleaseStringUTFChars(repoPathObj, repo_path);
    env->ReleaseStringUTFChars(remoteUrlObj, remote_url);
    if (usernameObj != nullptr && passwordObj != nullptr) {
        env->ReleaseStringUTFChars(usernameObj, username);
        env->ReleaseStringUTFChars(passwordObj, password);
    }

    return err;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_gitnote_manager_GitManagerKt_lastCommitLib(JNIEnv *env, jclass clazz) {

    // https://stackoverflow.com/questions/15717625/how-to-get-the-last-commit-from-head-in-a-git-repository-using-libgit2
    int err;
    git_oid sha_last_commit;

    err = git_reference_name_to_id(&sha_last_commit, repo, "HEAD");

    CHECK_LG2_RETURN(err, "git_reference_name_to_id", NULL);

    // Convert git_oid to string
    char commit_str[GIT_OID_HEXSZ + 1];
    git_oid_tostr(commit_str, sizeof(commit_str), &sha_last_commit);

    jstring j_commit_str = env->NewStringUTF(commit_str);

    return j_commit_str;
}




extern "C"
JNIEXPORT jint JNICALL
Java_com_example_gitnote_manager_GitManagerKt_commitAllLib(JNIEnv *env, jclass,
                                                           jstring usernameObj) {

    int err;

    git_index *index = NULL;


    err = git_repository_index(&index, repo);
    CHECK_LG2(err, "git_repository_index");


    err = git_index_add_all(index, NULL, 0, NULL, NULL);
    if (check_lg2(err, "git_repository_index") < 0) {
        git_index_free(index);
        return err;
    }

    // Écrire les changements de l'index dans le référentiel
    err = git_index_write(index);
    if (check_lg2(err, "git_index_write_tree") < 0) {
        git_index_free(index);
        return err;
    }


    git_oid commit_id;
    // Créer un nouvel arbre avec les modifications
    err = git_index_write_tree(&commit_id, index);
    if (check_lg2(err, "git_index_write_tree") < 0) {
        git_index_free(index);
        return err;
    }


    // Récupérer le dernier commit HEAD
    git_oid parent_commit_id;
    git_reference_name_to_id(&parent_commit_id, repo, "HEAD");
    git_commit *parent_commit = NULL;
    git_commit_lookup(&parent_commit, repo, &parent_commit_id);


    git_tree *tree;
    err = git_tree_lookup(&tree, repo, &commit_id);
    if (check_lg2(err, "git_tree_lookup") < 0) {
        git_index_free(index);
        return err;
    }

    const char *username = env->GetStringUTFChars(usernameObj, nullptr);

    git_signature *signature = NULL;
    err = git_signature_now(&signature, username, username);
    if (check_lg2(err, "git_signature_now") < 0) {
        git_index_free(index);
        git_tree_free(tree);
        env->ReleaseStringUTFChars(usernameObj, username);
        return err;
    }


    // todo: upgrade the message commit (maybe add the time and the name of the file being updated
    //  (take this from from Kotlin or check what file are committed from git)
    err = git_commit_create_v(
            &commit_id,
            repo,
            "HEAD",
            signature,
            signature,
            NULL,
            "Commit from gitnote",
            tree,
            1,
            parent_commit
    );

    check_lg2(err, "git_commit_create_v");

    git_index_free(index);
    git_tree_free(tree);
    git_signature_free(signature);
    env->ReleaseStringUTFChars(usernameObj, username);

    return err;
}




extern "C"
JNIEXPORT jint JNICALL
Java_com_example_gitnote_manager_GitManagerKt_pushLib(JNIEnv *env, jclass clazz,
                                                      jstring usernameObj,
                                                      jstring passwordObj,
                                                      jobject progressCallback) {

    const char *username;
    const char *password;

    if (usernameObj != nullptr && passwordObj != nullptr) {
        username = env->GetStringUTFChars(usernameObj, nullptr);
        password = env->GetStringUTFChars(passwordObj, nullptr);
    }


    CallbackHandler callbackHandler = CallbackHandler(env, progressCallback);
    int err = push(repo, username, password, callbackHandler);

    if (usernameObj != nullptr && passwordObj != nullptr) {
        env->ReleaseStringUTFChars(usernameObj, username);
        env->ReleaseStringUTFChars(passwordObj, password);
    }

    return err;
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_example_gitnote_manager_GitManagerKt_pullLib(JNIEnv *env, jclass clazz,
                                                      jstring usernameObj,
                                                      jstring passwordObj,
                                                      jobject progressCallback) {

    const char *username;
    const char *password;

    if (usernameObj != nullptr && passwordObj != nullptr) {
        username = env->GetStringUTFChars(usernameObj, nullptr);
        password = env->GetStringUTFChars(passwordObj, nullptr);
    }

    CallbackHandler callbackHandler = CallbackHandler(env, progressCallback);
    int err = pull(repo, username, password, callbackHandler);

    if (usernameObj != nullptr && passwordObj != nullptr) {
        env->ReleaseStringUTFChars(usernameObj, username);
        env->ReleaseStringUTFChars(passwordObj, password);
    }

    return err;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_gitnote_manager_GitManagerKt_freeLib(JNIEnv *env, jclass clazz) {
    git_libgit2_shutdown();
}
extern "C"
JNIEXPORT void JNICALL
Java_com_example_gitnote_manager_GitManagerKt_closeRepoLib(JNIEnv *env, jclass clazz) {
    git_repository_free(repo);
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_example_gitnote_manager_GitManagerKt_isChangeLib(JNIEnv *env, jclass clazz) {

    git_status_list *status_list = NULL;
    git_status_options options = GIT_STATUS_OPTIONS_INIT;
    options.flags |= GIT_STATUS_OPT_INCLUDE_UNTRACKED;

    int res = git_status_list_new(&status_list, repo, &options);
    CHECK_LG2(res, "git_status_list_new");

    size_t entry_count = git_status_list_entrycount(status_list);

    // LOGD cause seg fault here
    /*
    for (size_t i = 0; i < entry_count; i++) {
        const git_status_entry *entry = git_status_byindex(status_list, i);
        LOGD("Fichier: %s, Statut: %u\n", entry->head_to_index->old_file.path, entry->status);
    }
    */

    git_status_list_free(status_list);

    return entry_count > 0;
}