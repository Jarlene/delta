package org.byteam.delta.task

import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.internal.pipeline.TransformTask
import com.android.build.gradle.internal.transforms.ProGuardTransform
import com.google.common.base.Joiner
import org.apache.commons.io.FileUtils
import org.byteam.delta.bean.Patch
import org.byteam.delta.util.TaskUtils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
/**
 * Backup original files.
 *
 * @Author: chenenyu
 * @Created: 16/8/24 18:50.
 */
class BackupTask extends DefaultTask {

    @Input
    Patch mPatch

    @Input
    ApplicationVariant mVariant

    @TaskAction
    void backupPatch() {
        File originalDir = new File(mPatch.backupPath)
        originalDir.mkdirs()
        FileUtils.cleanDirectory(originalDir)

        saveMapping()
        saveMaindexlist()
        copyApkToPatch()
    }

    /**
     * Save mapping.txt
     */
    private void saveMapping() {
        if (mVariant.buildType.minifyEnabled) {
            String transformClassesAndResourcesWithProguardForVariant = TaskUtils.getTransformProguard(mVariant)
            def proguardTask = project.tasks.findByName(transformClassesAndResourcesWithProguardForVariant) as TransformTask
            if (proguardTask) {
                def mappingFile = (proguardTask.transform as ProGuardTransform).mappingFile
                FileUtils.copyFile(mappingFile, mPatch.mappingFile)
            } else {
                println("Task:${transformClassesAndResourcesWithProguardForVariant} not found.")
            }
        }
    }

    /**
     * Save maindexlist.txt
     */
    private void saveMaindexlist() {
        // Q: mVariant.buildType.multiDexEnabled always returns null, why?
        String transformClassesWithMultidexlistForVariant = TaskUtils.getTransformMultidexlist(mVariant)
        def multidexlistTask = project.tasks.findByName(transformClassesWithMultidexlistForVariant)
        if (multidexlistTask) {
            File maindexlist = getMaindexlist()
            if (maindexlist.exists() && maindexlist.isFile()) {
                FileUtils.copyFile(maindexlist, mPatch.mainDexListFile)
            }
        } else {
            println("Task:${transformClassesWithMultidexlistForVariant} not found.")
        }
    }

    /**
     * 获取生成的maindexlist.txt
     */
    private File getMaindexlist() {
        List<String> dir = []
        dir.add(project.buildDir.absolutePath)
        dir.add('intermediates')
        dir.add('multi-dex')
        if (mPatch.flavorName)
            dir.add(mPatch.flavorName)
        dir.add(mPatch.buildTypeName)
        dir.add('maindexlist.txt')

        return new File(Joiner.on(File.separator).join(dir))
    }

    private void copyApkToPatch() {
        mVariant.outputs.each {
            File apk = it.getOutputFile()
            if (apk != null && apk.name.endsWith(".apk")) {
                File apkDir = new File(mPatch.backupApkPath)
                if (!apkDir.exists()) {
                    apkDir.mkdirs()
                } else {
                    FileUtils.cleanDirectory(apkDir)
                }
                FileUtils.copyFileToDirectory(apk, apkDir)
            }
        }
    }
}