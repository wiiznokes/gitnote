#ifndef GITNOTE_LOGGING_H
#define GITNOTE_LOGGING_H

#include <android/log.h>
#include <git2/errors.h>

#define LOG_TAG "GitnoteCPP"

#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

// todo: maybe store the last result of this macros to grab it from kotlin

/// Return err in case of error
#define CHECK_LG2(err, msg) if (check_lg2(err, msg) < 0) { return err; }

/// Return returnValue in case of error
#define CHECK_LG2_RETURN(err, msg, returnValue) if (check_lg2(err, msg) < 0) { return returnValue; }


/// Return err
#define CHECK_LG2_RET(err, msg) check_lg2(err, msg); return err;

int check_lg2(int err, const char *msg);

#endif
