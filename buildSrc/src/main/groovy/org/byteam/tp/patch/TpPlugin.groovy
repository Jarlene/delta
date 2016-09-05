package org.byteam.tp.patch

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.internal.pipeline.TransformTask
import com.android.build.gradle.internal.transforms.DexTransform
import com.google.common.base.Joiner
import org.byteam.tp.patch.bean.Patch
import org.byteam.tp.patch.extension.ExtConsts
import org.byteam.tp.patch.extension.TpExtension
import org.byteam.tp.patch.task.TaskConsts
import org.byteam.tp.patch.task.TpBackupTask
import org.byteam.tp.patch.task.TpPatchTask
import org.byteam.tp.patch.task.TpPrePatchTask
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
            // Test code. 查看dex相关的tasks
//            project.tasks.matching {
//                it.name.contains('dex') || it.name.contains('Dex')
//            }.each {
//                println("------${it.name}")
//            }

            if (!extension.enable) {
                return
            }

            project.android.applicationVariants.all { ApplicationVariant variant ->
                // 获取版本号
                int versionCode = extension.versionCode > 0 ? extension.versionCode : variant.versionCode
                Patch patch = new Patch(project.projectDir.getAbsolutePath(), versionCode + "",
                        variant.flavorName, variant.buildType.name)


                String transformClassesWithMultidexlistForVariant = "transformClassesWithMultidexlistFor${variant.name.capitalize()}"
                def multidexlistTask = project.tasks.findByName(transformClassesWithMultidexlistForVariant)
                if (multidexlistTask) {
                    multidexlistTask.doLast {
                        println("${multidexlistTask.name} 执行了。")
                    }
                } else {
                    println("Task:${transformClassesWithMultidexlistForVariant} not found.")
                }

                // dex task
                String transformClassesWithDexForVariant = TaskUtils.getTransformDex(variant)
                def dexTask = project.tasks.findByName(transformClassesWithDexForVariant) as TransformTask
                if (dexTask) {
                    setMaxNumberOfIdxPerDex(dexTask.transform as DexTransform, extension)
                } else {
                    println("Task:${transformClassesWithDexForVariant} not found.")
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

                    Task cleanTask = project.tasks.findByName("clean")
                    if (cleanTask) {
                        Task prePatchTask = project.task(type: TpPrePatchTask, overwrite: true,
                                TaskConsts.TP_PRE_PATCH.concat(variant.name.capitalize())) { TpPrePatchTask task ->
                            task.group = TaskConsts.GROUP
                            task.description = "Clean project and apply mapping for ${variant.name}"
                            task.mPatch = patch
                            task.mVariant = variant
                            task.dependsOn cleanTask
                        }

                        project.task(type: TpPatchTask, overwrite: true,
                                TaskConsts.TP_PATCH.concat(variant.name.capitalize())) { TpPatchTask task ->
                            task.group = TaskConsts.GROUP
                            task.description = "Generates patchs for ${variant.name}"
                            task.mPatch = patch
                            task.mVariant = variant
                            task.pushPatchToDevice = extension.autoPushPatchToDevice
                            task.dependsOn prePatchTask
                            task.dependsOn assembleTask
                            assembleTask.mustRunAfter prePatchTask
                        }
                    } else {
                        println("Can not find task: clean")
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
        // fixme 2.2.0以前的gradle-plugin没有DefaultDexOptions。
//        DefaultDexOptions dexOptions = ReflectionUtils.getField(dexTransform, dexTransform.class, "dexOptions")
//        List<String> additionalParameters = dexOptions.additionalParameters
//        if (additionalParameters == null) {
//            additionalParameters = []
//        }
//        String arg = '--set-max-idx-number='
//        if (extension.maxNumberOfIdxPerDex > 0 && extension.maxNumberOfIdxPerDex < 0xFFFF) {
//            arg = arg.concat("${extension.maxNumberOfIdxPerDex}")
//        } else {
//            arg = arg.concat(String.valueOf(ExtConsts.DEFAULT_MAX_NUMBER_OF_IDX_PER_DEX))
//        }
//        additionalParameters << arg
//        dexOptions.additionalParameters = additionalParameters
//        ReflectionUtils.setField(dexTransform, dexTransform.class, "dexOptions", dexOptions)
    }

    /**
     * 获取生成dex的目录。
     */
    static File getDexFolder(Project project, Patch patch) {
        List<String> dirs = []
        dirs.add(project.buildDir)
        dirs.add('intermediates')
        dirs.add('transforms')
        dirs.add('dex')
        if (patch.flavorName)
            dirs.add(patch.flavorName)
        dirs.add(patch.buildTypeName)
        dirs.add('folders')
        dirs.add('1000')
        dirs.add('1f')
        dirs.add('main')

        new File(Joiner.on(File.separator).join(dirs))
    }
}