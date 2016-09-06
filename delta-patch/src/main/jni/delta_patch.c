#include <jni.h>
//#include "bsdiff.c"
#include "bspatch.c"

/**
JNIEXPORT jint JNICALL
Java_org_byteam_delta_patch_DeltaPatch_diff(JNIEnv *env, jclass type, jstring oldPath_,
                                            jstring newPath_, jstring patchPath_) {
    const char *oldPath = (*env)->GetStringUTFChars(env, oldPath_, 0);
    const char *newPath = (*env)->GetStringUTFChars(env, newPath_, 0);
    const char *patchPath = (*env)->GetStringUTFChars(env, patchPath_, 0);

    const char *argv[] = {"bsdiff", oldPath, newPath, patchPath};
    int result = diff(4, argv);

    (*env)->ReleaseStringUTFChars(env, oldPath_, oldPath);
    (*env)->ReleaseStringUTFChars(env, newPath_, newPath);
    (*env)->ReleaseStringUTFChars(env, patchPath_, patchPath);

    return result;
}
**/

JNIEXPORT jint JNICALL
Java_org_byteam_delta_patch_DeltaPatch_patch(JNIEnv *env, jclass type, jstring oldPath_,
                                             jstring newPath_, jstring patchPath_) {
    const char *oldPath = (*env)->GetStringUTFChars(env, oldPath_, 0);
    const char *newPath = (*env)->GetStringUTFChars(env, newPath_, 0);
    const char *patchPath = (*env)->GetStringUTFChars(env, patchPath_, 0);

    const char *argv[] = {"bspatch", oldPath, newPath, patchPath};
    int result = patch(4, argv);

    (*env)->ReleaseStringUTFChars(env, oldPath_, oldPath);
    (*env)->ReleaseStringUTFChars(env, newPath_, newPath);
    (*env)->ReleaseStringUTFChars(env, patchPath_, patchPath);

    return result;
}

