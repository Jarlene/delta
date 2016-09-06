package org.byteam.tp.patch

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.internal.pipeline.TransformTask
import com.android.build.gradle.internal.transforms.DexTransform
import com.android.build.gradle.internal.transforms.ProGuardTransform
import com.android.builder.Version
import com.google.common.base.Joiner
import org.byteam.tp.patch.bean.Patch
import org.byteam.tp.patch.extension.ExtConsts
import org.byteam.tp.patch.extension.TpExtension
import org.byteam.tp.patch.task.TaskConsts
import org.byteam.tp.patch.task.TpBackupTask
import org.byteam.tp.patch.task.TpPatchTask
import org.byteam.tp.patch.task.TpPrePatchTask
import org.byteam.tp.patch.util.ReflectionUtils
import org.byteam.tp.patch.util.TaskUtils
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

/**
 * @Author: chenenyu
 * @Created: 16/8/17 11:03.
 */
class TpPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        def isAndroidApp = project.plugins.hasPlugin(AppPlugin)
        if (!isAndroidApp) {
            throw new GradleException("'com.android.application' plugin required.")
        }

        def extension = project.extensions.create(ExtConsts.EXTENSION_NAME, TpExtension)

        project.afterEvaluate {

            if (!extension.enable) {
                return
            }

            project.android.applicationVariants.all { ApplicationVariant variant ->
                // Fetch version code.
                int versionCode = extension.versionCode > 0 ? extension.versionCode : variant.versionCode
                Patch patch = new Patch(project.projectDir.getAbsolutePath(), String.valueOf(versionCode),
                        variant.flavorName, variant.buildType.name)

                // dex task
                String transformClassesWithDexForVariant = TaskUtils.getTransformDex(variant)
                def dexTask = project.tasks.findByName(transformClassesWithDexForVariant) as TransformTask
                if (dexTask) {
                    setMaxNumberOfIdxPerDex(dexTask.transform as DexTransform, extension)
                } else {
                    println("Task:${transformClassesWithDexForVariant} not found.")
                }

                Task prePatchTask
                if (extension.mapping) { // If custom mapping exists.
                    File customMappingFile = new File(extension.mapping)
                    if (customMappingFile.exists() && customMappingFile.isFile()) {
                        String transformClassesAndResourcesWithProguardForVariant = TaskUtils.getTransformProguard(variant)
                        def proguardTask = project.tasks.findByName(transformClassesAndResourcesWithProguardForVariant) as TransformTask
                        if (proguardTask) {
                            def proguard = proguardTask.transform as ProGuardTransform
                            proguard.applyTestedMapping(customMappingFile)
                        } else {
                            println("Task: ${transformClassesAndResourcesWithProguardForVariant} not found.")
                        }
                    }
                } else { // Add pre-patch task to apply previous mapping.
                    Task cleanTask = project.tasks.findByName("clean")
                    if (cleanTask) {
                        prePatchTask = project.task(type: TpPrePatchTask, overwrite: true,
                                TaskConsts.TP_PRE_PATCH.concat(variant.name.capitalize())) { TpPrePatchTask task ->
                            task.group = TaskConsts.GROUP
                            task.description = "Clean project and apply mapping for ${variant.name}"
                            task.mPatch = patch
                            task.mVariant = variant
                            task.dependsOn cleanTask
                        }
                    } else {
                        println("Can not find task: clean")
                    }
                }

                String assembleVariant = TaskUtils.getAssemble(variant)
                Task assembleTask = project.tasks.findByName(assembleVariant)
                if (assembleTask) {
                    project.task(type: TpBackupTask, overwrite: true,
                            TaskConsts.TP_BACKUP.concat(variant.name.capitalize())) { TpBackupTask task ->
                        task.group = TaskConsts.GROUP
                        task.description = "Backup all required files for ${variant.name}"
                        task.mPatch = patch
                        task.mVariant = variant
                        task.dependsOn assembleTask
                    }

                    project.task(type: TpPatchTask, overwrite: true,
                            TaskConsts.TP_PATCH.concat(variant.name.capitalize())) { TpPatchTask task ->
                        task.group = TaskConsts.GROUP
                        task.description = "Generates patchs for ${variant.name}"
                        task.mPatch = patch
                        task.mVariant = variant
                        task.pushPatchToDevice = extension.autoPushPatchToDevice
                        task.dependsOn assembleTask
                        if (prePatchTask) {
                            task.dependsOn prePatchTask
                            assembleTask.mustRunAfter prePatchTask
                        }
                    }
                } else {
                    println("Can not find task: ${assembleVariant}")
                }

            }
        }
    }

    /**
     * 指定每个dex的最大方法数。
     */
    private void setMaxNumberOfIdxPerDex(DexTransform dexTransform, TpExtension extension) {
        if (isMajor2Minor2OrAboveVersion()) { // 2.2.x or above
            // DefaultDexOptions
            def dexOptions = ReflectionUtils.getField(dexTransform, dexTransform.class, "dexOptions")
            List<String> additionalParameters = dexOptions.additionalParameters
            if (additionalParameters == null) {
                additionalParameters = []
            }
            String arg = '--set-max-idx-number='
            if (extension.maxNumberOfIdxPerDex > 0 && extension.maxNumberOfIdxPerDex < 0xFFFF) {
                arg = arg.concat("${extension.maxNumberOfIdxPerDex}")
            } else {
                arg = arg.concat(String.valueOf(ExtConsts.DEFAULT_MAX_NUMBER_OF_IDX_PER_DEX))
            }
            additionalParameters << arg
            dexOptions.additionalParameters = additionalParameters
            ReflectionUtils.setField(dexTransform, dexTransform.class, "dexOptions", dexOptions)
        }
    }

    /**
     * @return True if AndroidGradlePluginVersion >= 2.2.x.
     */
    static boolean isMajor2Minor2OrAboveVersion() {
        String[] version = Version.ANDROID_GRADLE_PLUGIN_VERSION.split("\\.")
        if (version.length > 2) {
            if (Integer.valueOf(version[0]) > 1 && Integer.valueOf(version[1]) > 1) {
                true
            }
        }
        false
    }

    /**
     * 获取生成dex的目录。
     */
    static File getDexFolder(Project project, Patch patch) {
        List<String> dir = []
        dir.add(project.buildDir.absolutePath)
        dir.add('intermediates')
        dir.add('transforms')
        dir.add('dex')
        if (patch.flavorName)
            dir.add(patch.flavorName)
        dir.add(patch.buildTypeName)
        dir.add('folders')
        dir.add('1000')
        dir.add('1f')
        dir.add('main')

        return new File(Joiner.on(File.separator).join(dir))
    }

}