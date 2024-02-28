#ifndef GITNOTE_REMOTE_H
#define GITNOTE_REMOTE_H

#include <git2/types.h>
#include "CallbackHandler.h"


typedef void (*progress_callback)(int percent);

int clone(
        git_repository **repo,
        const char *repo_path,
        const char *remote_url,
        const char *username,
        const char *password,
        CallbackHandler callbackHandler
);

int push(
        git_repository *repo,
        const char *username,
        const char *password,
        CallbackHandler callbackHandler
);

int pull(
        git_repository *repo,
        const char *username,
        const char *password,
        CallbackHandler callbackHandler
);


#endif //GITNOTE_REMOTE_H
