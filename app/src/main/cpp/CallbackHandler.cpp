#include "CallbackHandler.h"
#include "logging.h"


bool isInit = false;

CallbackHandler::CallbackHandler(JNIEnv *env, jobject progressCallback) {
    if (progressCallback == nullptr) {
        return;
    }

    this->env = env;
    this->progressCallbackObj = progressCallback;
    jclass callbackClass = env->GetObjectClass(progressCallbackObj);
    this->callbackMethodId = env->GetMethodID(callbackClass, "invoke", "(I)V");
    isInit = true;
}


void CallbackHandler::progressCallback(int percent) {
    if (isInit) {
        env->CallVoidMethod(progressCallbackObj, callbackMethodId, percent);
    }
}
