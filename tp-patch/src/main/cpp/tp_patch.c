#include <jni.h>
#include "tp_patch.h"
#include "bspatch.c"

JNIEXPORT jint JNICALL
Java_org_byteam_tp_patch_TpPatch_patch(JNIEnv *env, jclass type, jstring oldPath_,
                                       jstring newPath_, jstring patchPath_) {
    const char *oldPath = (*env)->GetStringUTFChars(env, oldPath_, 0);
    const char *newPath = (*env)->GetStringUTFChars(env, newPath_, 0);
    const char *patchPath = (*env)->GetStringUTFChars(env, patchPath_, 0);

    const char *argv[] = {"bspatch", oldPath, newPath, patchPath};
    int result = patch(argv);

    (*env)->ReleaseStringUTFChars(env, oldPath_, oldPath);
    (*env)->ReleaseStringUTFChars(env, newPath_, newPath);
    (*env)->ReleaseStringUTFChars(env, patchPath_, patchPath);

    return result;
}