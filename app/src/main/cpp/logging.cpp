#include "logging.h"

int check_lg2(int err, const char *msg) {
    if (err >= 0) {
        //LOGD("%s success", msg);
        return err;
    } else {
        const git_error *lg2err;
        if ((lg2err = git_error_last()) != NULL && lg2err->message != NULL) {
            LOGE("%s: error, %d %s", msg, lg2err->klass, lg2err->message);
        } else {
            LOGE("%s: error, no libgit2 message", msg);
        }
        return err;
    }
}