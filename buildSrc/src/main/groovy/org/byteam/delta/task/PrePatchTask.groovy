package org.byteam.delta.task

import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.internal.pipeline.TransformTask
import com.android.build.gradle.internal.transforms.DexTransform
import com.android.build.gradle.internal.transforms.ProGuardTransform
import org.byteam.delta.DeltaPlugin
import org.byteam.delta.bean.Patch
import org.byteam.delta.util.ReflectionUtils
import org.byteam.delta.util.TaskUtils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/**
 * This task depends on clean task, and then apply mapping and maindexlist.
 *
 * @Author: chenenyu
 * @Created: 16/8/30 16:38.
 */
class PrePatchTask extends DefaultTask {

    @Input
    Patch mPatch

    @Input
    ApplicationVariant mVariant

    @TaskAction
    void prePatch() {
        applyMapping()
        applyMaindexlist()
    }

    private void applyMapping() {
        if (mVariant.buildType.minifyEnabled && mPatch.mappingFile.exists()) {
            String transformClassesAndResourcesWithProguardForVariant = TaskUtils.getTransformProguard(mVariant)
            def proguardTask = project.tasks.findByName(transformClassesAndResourcesWithProguardForVariant) as TransformTask
            if (proguardTask) {
                def proguard = proguardTask.transform as ProGuardTransform
                proguard.applyTestedMapping(mPatch.mappingFile)
                proguardTask.doLast {
                    proguard.applyTestedMapping(null)
                }
            } else {
                println("Task: ${transformClassesAndResourcesWithProguardForVariant} not found.")
            }
        }
    }

    private void applyMaindexlist() {
        File maindexlist = mPatch.mainDexListFile
        if (maindexlist.exists()) {
            String transformClassesWithDexForVariant = TaskUtils.getTransformDex(mVariant)
            def dexTask = project.tasks.findByName(transformClassesWithDexForVariant) as TransformTask
            if (dexTask) {
                if (DeltaPlugin.major2Minor2OrAboveVersion) {
                    DexTransform dexTransform = dexTask.transform as DexTransform
                    // DefaultDexOptions
                    def dexOptions = ReflectionUtils.getField(dexTransform, dexTransform.class, "dexOptions")
                    List<String> additionalParameters = dexOptions.additionalParameters
                    if (additionalParameters == null) {
                        additionalParameters = []
                    }
                    // additionalParameters << '--minimal-main-dex'  // 使主dex尽可能的小
                    String maindexlistArg = "--main-dex-list=${maindexlist.absolutePath}"
                    additionalParameters << maindexlistArg
                    dexOptions.additionalParameters = additionalParameters
                    ReflectionUtils.setField(dexTransform, dexTransform.class, "dexOptions", dexOptions)

                    dexTask.doLast { // dex task执行完后, 恢复配置
                        additionalParameters.remove(maindexlistArg)
                        dexOptions.additionalParameters = additionalParameters
                        ReflectionUtils.setField(dexTransform, dexTransform.class, "dexOptions", dexOptions)
                    }
                }
            } else {
                println("Task:${transformClassesWithDexForVariant} not found.")
            }
        }
    }
}