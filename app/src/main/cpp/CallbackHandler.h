#ifndef GITNOTE_CALLBACKHANDLER_H
#define GITNOTE_CALLBACKHANDLER_H


#include <jni.h>

class CallbackHandler {
private:
    JNIEnv *env;
    jobject progressCallbackObj;
    jmethodID callbackMethodId;

public:
    CallbackHandler(JNIEnv *env, jobject progressCallback);

    void progressCallback(int percent);
};


#endif //GITNOTE_CALLBACKHANDLER_H
