package org.byteam.tp.patch

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.internal.pipeline.TransformTask
import com.android.build.gradle.internal.transforms.DexTransform
import com.android.builder.core.DefaultDexOptions
import com.google.common.base.Joiner
import org.byteam.tp.patch.bean.Patch
import org.byteam.tp.patch.constant.ExtConsts
import org.byteam.tp.patch.constant.TaskConsts
import org.byteam.tp.patch.task.TpBackupTask
import org.byteam.tp.patch.util.ReflectionUtils
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

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
            project.tasks.matching {
                it.name.contains('dex') || it.name.contains('Dex')
            }.each {
                println("------${it.name}")
            }

            if (!extension.enable) {
                return
            }

            project.android.applicationVariants.all { ApplicationVariant variant ->
                // 获取版本号
                int versionCode = extension.versionCode > 0 ? extension.versionCode : variant.versionCode
                Patch patch = new Patch(project.projectDir.getAbsolutePath(), versionCode + "",
                        variant.flavorName, variant.buildType.name)

                boolean minifyEnabled = variant.buildType.minifyEnabled

                // task: tpBackupDebug
                project.task(type: TpBackupTask, overwrite: true, TaskConsts.TP_BACKUP.concat(variant.name.capitalize())) { TpBackupTask task ->
                    task.group = 'tp'
                    task.description = "Backup all dex for ${variant.name}"
                    task.dependsOn project.tasks.findByName("assemble${variant.name.capitalize()}")
                    task.mPatch = patch
                    task.minifyEnabled = minifyEnabled
                    task.mVariant = variant
                }

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
                String transformClassesWithDexForVariant = "transformClassesWithDexFor${variant.name.capitalize()}"
                def dexTask = project.tasks.findByName(transformClassesWithDexForVariant) as TransformTask
                if (dexTask) {
                    setMaxNumberOfIdxPerDex(dexTask.transform as DexTransform, extension)
                } else {
                    println("Task:${transformClassesWithDexForVariant} not found.")
                }

                // todo
            }
        }
    }

    /**
     * 指定每个dex的最大方法数。
     */
    static void setMaxNumberOfIdxPerDex(DexTransform dexTransform, TpExtension extension) {
        DefaultDexOptions dexOptions = ReflectionUtils.getField(dexTransform, dexTransform.class, "dexOptions")
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

    /**
     * 获取生成dex的目录。
     */
    static File getDexFolder(Project project, Patch patch, DexTransform dexTransform) {
        List<String> dirs = []
        dirs.add(project.buildDir)
        dirs.add('intermediates')
        dirs.add('transforms')
        dirs.add(dexTransform.name) // dex
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