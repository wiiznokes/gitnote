
#include "merge.h"
#include "logging.h"
#include <git2.h>

#include <errno.h>
#include <assert.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <fcntl.h>


int resolve_refish(git_annotated_commit **commit, git_repository *repo, const char *refish) {
    git_reference *ref;
    git_object *obj;
    int err;

    assert(commit != NULL);

    err = git_reference_dwim(&ref, repo, refish);
    if (err == GIT_OK) {
        git_annotated_commit_from_ref(commit, repo, ref);
        git_reference_free(ref);
        return 0;
    }

    err = git_revparse_single(&obj, repo, refish);
    if (err == GIT_OK) {
        err = git_annotated_commit_lookup(commit, repo, git_object_id(obj));
        git_object_free(obj);
    }

    return err;
}

struct merge_options {
    const char **heads;
    size_t heads_count;

    git_annotated_commit **annotated;
    size_t annotated_count;

    unsigned int no_commit: 1;
};


static void merge_options_init(struct merge_options *opts) {
    memset(opts, 0, sizeof(*opts));

    opts->heads = NULL;
    opts->heads_count = 0;
    opts->annotated = NULL;
    opts->annotated_count = 0;
}


void *xrealloc(void *oldp, size_t newsz) {
    void *p = realloc(oldp, newsz);
    if (p == NULL) {
        LOGE("Cannot allocate memory, exiting.\n");
        exit(1);
    }
    return p;
}

static void opts_add_refish(struct merge_options *opts, const char *refish) {
    size_t sz;

    assert(opts != NULL);

    sz = ++opts->heads_count * sizeof(opts->heads[0]);
    opts->heads = (const char **) xrealloc((void *) opts->heads, sz);
    opts->heads[opts->heads_count - 1] = refish;
}

static int resolve_heads(git_repository *repo, struct merge_options *opts) {
    git_annotated_commit **annotated = (git_annotated_commit **) calloc(opts->heads_count,
                                                                        sizeof(git_annotated_commit *));
    size_t annotated_count = 0, i;
    int err;

    for (i = 0; i < opts->heads_count; i++) {
        err = resolve_refish(&annotated[annotated_count++], repo, opts->heads[i]);
        if (err != 0) {
            LOGE("failed to resolve refish %s: %s\n", opts->heads[i], git_error_last()->message);
            annotated_count--;
            continue;
        }
    }

    if (annotated_count != opts->heads_count) {
        LOGE("unable to parse some refish\n");
        free(annotated);
        return -1;
    }

    opts->annotated = annotated;
    opts->annotated_count = annotated_count;
    return 0;
}

static int perform_fastforward(git_repository *repo, const git_oid *target_oid, int is_unborn) {
    git_checkout_options ff_checkout_options = GIT_CHECKOUT_OPTIONS_INIT;
    git_reference *target_ref;
    git_reference *new_target_ref;
    git_object *target = NULL;
    int err;

    if (is_unborn) {
        const char *symbolic_ref;
        git_reference *head_ref;

        /* HEAD reference is unborn, lookup manually so we don't try to resolve it */
        err = git_reference_lookup(&head_ref, repo, "HEAD");
        if (err != 0) {
            LOGD("failed to lookup HEAD ref");
            return -1;
        }

        /* Grab the reference HEAD should be pointing to */
        symbolic_ref = git_reference_symbolic_target(head_ref);

        /* Create our master reference on the target OID */
        err = git_reference_create(&target_ref, repo, symbolic_ref, target_oid, 0, NULL);
        if (err != 0) {
            LOGD("failed to create master reference");
            return -1;
        }

        git_reference_free(head_ref);
    } else {
        /* HEAD exists, just lookup and resolve */
        err = git_repository_head(&target_ref, repo);
        if (err != 0) {
            LOGE("failed to get HEAD reference");
            return -1;
        }
    }

    /* Lookup the target object */
    err = git_object_lookup(&target, repo, target_oid, GIT_OBJECT_COMMIT);
    if (err != 0) {
        LOGE("failed to lookup OID %s", git_oid_tostr_s(target_oid));
        return -1;
    }

    /* Checkout the result so the workdir is in the expected state */
    ff_checkout_options.checkout_strategy = GIT_CHECKOUT_SAFE;
    err = git_checkout_tree(repo, target, &ff_checkout_options);
    if (err != 0) {
        fprintf(stderr, "failed to checkout HEAD reference");
        return -1;
    }

    /* Move the target reference to the target OID */
    err = git_reference_set_target(&new_target_ref, target_ref, target_oid, NULL);
    if (err != 0) {
        LOGE("failed to move HEAD reference");
        return -1;
    }

    git_reference_free(target_ref);
    git_reference_free(new_target_ref);
    git_object_free(target);

    return 0;
}

static int output_conflicts(git_index *index) {
    git_index_conflict_iterator *conflicts;
    const git_index_entry *ancestor;
    const git_index_entry *our;
    const git_index_entry *their;

    int err = git_index_conflict_iterator_new(&conflicts, index);

    CHECK_LG2(err, "git_index_conflict_iterator_new");

    while ((err = git_index_conflict_next(&ancestor, &our, &their, conflicts)) == 0) {
        LOGE("conflict: a:%s o:%s t:%s",
             ancestor ? ancestor->path : "NULL",
             our->path ? our->path : "NULL",
             their->path ? their->path : "NULL");
    }

    if (err != GIT_ITEROVER) {
        LOGE("error iterating conflicts");
    }

    git_index_conflict_iterator_free(conflicts);
    return 0;
}

// todo: remove memory leak of this function
static int create_merge_commit(git_repository *repo, git_index *index, struct merge_options *opts) {
    git_oid tree_oid, commit_oid;
    git_tree *tree;
    git_signature *sign;
    git_reference *merge_ref = NULL;
    git_annotated_commit *merge_commit;
    git_reference *head_ref;
    git_commit **parents;
    const char *msg_target = NULL;
    size_t msglen;
    char *msg;
    size_t i;
    int err;

    /* Grab our needed references */
    err = git_repository_head(&head_ref, repo);
    CHECK_LG2(err, "git_repository_head");
    if (resolve_refish(&merge_commit, repo, opts->heads[0])) {
        LOGE("failed to resolve refish %s", opts->heads[0]);
        return -1;
    }

    /* Maybe that's a ref, so DWIM it */
    err = git_reference_dwim(&merge_ref, repo, opts->heads[0]);
    CHECK_LG2(err, "failed to DWIM reference");

    /* Grab a signature */
    // todo: take the signature here
    CHECK_LG2(git_signature_now(&sign, "Me", "me@example.com"), "failed to create signature");

#define MERGE_COMMIT_MSG "Merge %s '%s'"
    /* Prepare a standard merge commit message */
    if (merge_ref != NULL) {
        CHECK_LG2(git_branch_name(&msg_target, merge_ref),
                  "failed to get branch name of merged ref");
    } else {
        msg_target = git_oid_tostr_s(git_annotated_commit_id(merge_commit));
    }

    msglen = snprintf(NULL, 0, MERGE_COMMIT_MSG, (merge_ref ? "branch" : "commit"), msg_target);
    if (msglen > 0) msglen++;
    msg = (char *) malloc(msglen);
    err = snprintf(msg, msglen, MERGE_COMMIT_MSG, (merge_ref ? "branch" : "commit"), msg_target);

    if (err) return -1;

    /* Setup our parent commits */
    parents = (git_commit **) calloc(opts->annotated_count + 1, sizeof(git_commit *));
    err = git_reference_peel((git_object **) &parents[0], head_ref, GIT_OBJECT_COMMIT);
    CHECK_LG2(err, "failed to peel head reference");
    for (i = 0; i < opts->annotated_count; i++) {
        git_commit_lookup(&parents[i + 1], repo, git_annotated_commit_id(opts->annotated[i]));
    }

    /* Prepare our commit tree */
    CHECK_LG2(git_index_write_tree(&tree_oid, index), "failed to write merged tree");
    CHECK_LG2(git_tree_lookup(&tree, repo, &tree_oid), "failed to lookup tree");

    /* Commit time ! */
    err = git_commit_create(&commit_oid,
                            repo, git_reference_name(head_ref),
                            sign, sign,
                            NULL, msg,
                            tree,
                            opts->annotated_count + 1, (const git_commit **) parents);
    CHECK_LG2(err, "failed to create commit");

    /* We're done merging, cleanup the repository state */
    git_repository_state_cleanup(repo);

    free(parents);
    return err;
}

int merge(git_repository *repo) {
    struct merge_options opts;
    git_index *index;
    git_repository_state_t state;
    git_merge_analysis_t analysis;
    git_merge_preference_t preference;
    int err;


    state = (git_repository_state_t) git_repository_state(repo);
    if (state != GIT_REPOSITORY_STATE_NONE) {
        LOGE("repository is in unexpected state %d", state);
        goto cleanup;
    }

    merge_options_init(&opts);

    opts_add_refish(&opts, "refs/remotes/origin/HEAD");


    err = resolve_heads(repo, &opts);
    check_lg2(err, "resolve_heads");
    if (err != 0)
        goto cleanup;


    err = git_merge_analysis(&analysis, &preference,
                             repo,
                             (const git_annotated_commit **) opts.annotated,
                             opts.annotated_count);
    CHECK_LG2(err, "git_merge_analysis");

    if (analysis & GIT_MERGE_ANALYSIS_UP_TO_DATE) {
        printf("Already up-to-date\n");
        return 0;
    } else if (analysis & GIT_MERGE_ANALYSIS_UNBORN ||
               (analysis & GIT_MERGE_ANALYSIS_FASTFORWARD &&
                !(preference & GIT_MERGE_PREFERENCE_NO_FASTFORWARD))) {
        const git_oid *target_oid;
        if (analysis & GIT_MERGE_ANALYSIS_UNBORN) {
            LOGD("Unborn");
        } else {
            LOGD("Fast-forward");
        }

        /* Since this is a fast-forward, there can be only one merge head */
        target_oid = git_annotated_commit_id(opts.annotated[0]);
        assert(opts.annotated_count == 1);

        return perform_fastforward(repo, target_oid, (analysis & GIT_MERGE_ANALYSIS_UNBORN));
    } else if (analysis & GIT_MERGE_ANALYSIS_NORMAL) {
        git_merge_options merge_opts = GIT_MERGE_OPTIONS_INIT;
        git_checkout_options checkout_opts = GIT_CHECKOUT_OPTIONS_INIT;

        merge_opts.flags = 0;
        merge_opts.file_flags = GIT_MERGE_FILE_STYLE_DIFF3;

        checkout_opts.checkout_strategy = GIT_CHECKOUT_FORCE | GIT_CHECKOUT_ALLOW_CONFLICTS;

        if (preference & GIT_MERGE_PREFERENCE_FASTFORWARD_ONLY) {
            LOGI("Fast-forward is preferred, but only a merge is possible");
            return -1;
        }

        err = git_merge(repo,
                        (const git_annotated_commit **) opts.annotated, opts.annotated_count,
                        &merge_opts, &checkout_opts);
        CHECK_LG2(err, "merge failed");
    }

    /* If we get here, we actually performed the merge above */

    CHECK_LG2(git_repository_index(&index, repo), "failed to get repository index");

    if (git_index_has_conflicts(index)) {
        /* Handle conflicts */
        output_conflicts(index);
    } else if (!opts.no_commit) {
        create_merge_commit(repo, index, &opts);
        LOGD("Merge made");
    }

    cleanup:
    free((char **) opts.heads);
    free(opts.annotated);

    return 0;
}