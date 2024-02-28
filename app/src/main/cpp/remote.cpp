#include "remote.h"
#include "logging.h"
#include "merge.h"

#include <git2.h>


int always_allow_certificate_check(git_cert *cert, int valid, const char *host, void *payload) {
    return 0;
}


typedef struct remote_payload {
    size_t indexed_objects;
    CallbackHandler callbackHandler;

    const char *username;
    const char *password;
} remote_payload;


int credentials_https_callback(git_cred **out, const char *url, const char *username_from_url,
                               unsigned int allowed_types, void *payload) {
    remote_payload *pd = (remote_payload *) payload;

    return git_cred_userpass_plaintext_new(out, pd->username, pd->password);
}





// todo: maybe add this, for giant repo
/*
static void checkout_progress(const char *path, size_t cur, size_t tot, void *payload)
{

}
 */

static int fetch_progress(const git_indexer_progress *stats, void *payload) {

    remote_payload *pd = (remote_payload *) payload;

    if (pd->indexed_objects != stats->indexed_objects) {
        float progress_float = (float) stats->indexed_objects / stats->total_objects * 100;
        int progress = (int) progress_float;

        pd->callbackHandler.progressCallback(progress);
        pd->indexed_objects = stats->indexed_objects;
    }

    return 0;
}


int clone(
        git_repository **repo,
        const char *repo_path,
        const char *remote_url,
        const char *username,
        const char *password,
        CallbackHandler callbackHandler
) {

    git_clone_options clone_opts = GIT_CLONE_OPTIONS_INIT;
    git_checkout_options checkout_opts = GIT_CHECKOUT_OPTIONS_INIT;

    checkout_opts.checkout_strategy = GIT_CHECKOUT_SAFE;
    /*
    checkout_opts.progress_payload = &pd;
    checkout_opts.progress_cb = &checkout_progress;
     */

    clone_opts.checkout_opts = checkout_opts;

    remote_payload pd = remote_payload{
            .indexed_objects = 0,
            .callbackHandler = callbackHandler,
            .username =  username,
            .password =  password
    };
    clone_opts.fetch_opts.callbacks.payload = &pd;
    clone_opts.fetch_opts.callbacks.transfer_progress = &fetch_progress;

    clone_opts.bare = false;
    clone_opts.fetch_opts.callbacks.certificate_check = always_allow_certificate_check;

    if (username && password) {
        clone_opts.fetch_opts.callbacks.credentials = credentials_https_callback;
    }

    int err = git_clone(repo, remote_url, repo_path, &clone_opts);
    CHECK_LG2_RET(err, "cloneRepo");
}

int push(
        git_repository *repo,
        const char *username,
        const char *password,
        CallbackHandler callbackHandler
) {
    git_remote *remote = NULL;
    int err = git_remote_lookup(&remote, repo, "origin");
    CHECK_LG2(err, "git_remote_lookup");


    const char *ref_specs[] = {"refs/heads/main:refs/heads/main"};
    git_strarray ref_specs_array = {
            .strings = (char **) ref_specs,
            .count = 1
    };


    git_push_options push_opts = GIT_PUSH_OPTIONS_INIT;

    push_opts.callbacks.certificate_check = always_allow_certificate_check;
    remote_payload pd = remote_payload{
            .indexed_objects = 0,
            .callbackHandler = callbackHandler,
            .username =  username,
            .password =  password,
    };
    push_opts.callbacks.payload = &pd;

    if (username && password) {
        push_opts.callbacks.credentials = credentials_https_callback;
    }

    err = git_remote_push(remote, &ref_specs_array, &push_opts);

    git_remote_free(remote);

    CHECK_LG2_RET(err, "git_remote_push");
}

int pull(
        git_repository *repo,
        const char *username,
        const char *password,
        CallbackHandler callbackHandler
) {
    git_remote *remote = NULL;
    int err = git_remote_lookup(&remote, repo, "origin");
    CHECK_LG2(err, "git_remote_lookup");

    git_fetch_options fetch_opts = GIT_FETCH_OPTIONS_INIT;
    fetch_opts.callbacks.certificate_check = always_allow_certificate_check;

    remote_payload pd = remote_payload{
            .indexed_objects = 0,
            .callbackHandler = callbackHandler,
            .username =  username,
            .password =  password
    };
    fetch_opts.callbacks.payload = &pd;
    if (username && password) {
        fetch_opts.callbacks.credentials = credentials_https_callback;
    }

    err = git_remote_fetch(remote, NULL, &fetch_opts, NULL);

    git_remote_free(remote);

    CHECK_LG2(err, "git_remote_fetch");

    return merge(repo);
}

