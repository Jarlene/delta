//
// Created by 陈恩裕 on 16/8/16.
//
#include <jni.h>

#ifndef COURGETTE_ORG_BTEAM_TP_BSDIFF_H
#define COURGETTE_ORG_BTEAM_TP_BSDIFF_H

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jint JNICALL
        Java_org_byteam_tp_patch_TpPatch_patch(JNIEnv *env, jclass type, jstring oldPath_,
                                               jstring newPath_, jstring patchPath_);

#ifdef __cplusplus
}
#endif

#endif //COURGETTE_ORG_BTEAM_TP_BSDIFF_H
