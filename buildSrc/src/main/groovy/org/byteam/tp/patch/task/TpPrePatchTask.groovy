package org.byteam.tp.patch.task

import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.internal.pipeline.TransformTask
import com.android.build.gradle.internal.transforms.ProGuardTransform
import org.byteam.tp.patch.bean.Patch
import org.byteam.tp.patch.util.TaskUtils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/**
 * @Author: chenenyu
 * @Created: 16/8/30 16:38.
 */
class TpPrePatchTask extends DefaultTask {

    @Input
    Patch mPatch

    @Input
    ApplicationVariant mVariant

    @TaskAction
    void prePatch() {
        applyMapping()
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
}