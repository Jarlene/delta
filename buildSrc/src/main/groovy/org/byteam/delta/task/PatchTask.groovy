package org.byteam.delta.task

import com.android.build.gradle.api.ApplicationVariant
import groovy.io.FileType
import org.apache.commons.io.Charsets
import org.apache.commons.io.FileUtils
import org.byteam.delta.DeltaPlugin
import org.byteam.delta.bean.Patch
import org.byteam.delta.util.BsDiffUtils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/**
 * Generates patch.
 *
 * @Author: chenenyu
 * @Created: 16/8/29 11:26.
 */
class PatchTask extends DefaultTask {

    @Input
    Patch mPatch

    @Input
    ApplicationVariant mVariant

    @Input
    boolean pushPatchToDevice

    File mPatchDir

    @TaskAction
    void patch() {
        if (checkFiles()) {
            mPatchDir = mPatch.patchDir
            mPatchDir.mkdirs()

            executeDiff()

            if (pushPatchToDevice) {
                copyPatchToDevice()
            }
        } else {
            println("There are no backup files for patch.")
        }
    }

    private boolean checkFiles() {
        File originalDexDir = new File(mPatch.backupDexPath)
        return originalDexDir.exists() && originalDexDir.directory && originalDexDir.listFiles() != null
    }

    private void executeDiff() {
        BsDiffUtils.copyBsDiff2BuildFolder(project)
        String bsdiff = BsDiffUtils.getBsDiffFilePath(project)

        File[] originalAllDex = new File(mPatch.backupDexPath).listFiles()
        File dexDir = DeltaPlugin.getDexFolder(project, mPatch)
        dexDir.listFiles().each { dex ->
            File originalDex = originalAllDex.find {
                it.name.equals(dex.name)
            }
            if (originalDex) {
                Process patch = new ProcessBuilder(bsdiff, originalDex.absolutePath, dex.absolutePath,
                        new File(mPatchDir, dex.name).absolutePath).redirectErrorStream(true).start()
                BufferedReader br = new BufferedReader(new InputStreamReader(patch.getInputStream()))
                StringBuilder consoleMsg = new StringBuilder()
                String line
                while (null != (line = br.readLine())) {
                    consoleMsg.append(line)
                }
                int result = patch.waitFor()
                if (result == 0) {
                    println("Patch successful")
                } else {
                    FileUtils.writeStringToFile(new File(mPatchDir, 'patch.log'),
                            consoleMsg.toString(), Charsets.UTF_8)
                    println("Generate patch file(s) failed, please see 'patch.log' for more info.")
                }
            } else {
                FileUtils.copyFileToDirectory(dex, mPatchDir)
            }
        }
    }

    private void copyPatchToDevice() {
        String adbPath = project.android.adbExe.absolutePath
        Runtime.runtime.exec(
                "${adbPath} shell;" +
                        "su;" +
                        "rm -rf /data/local/tmp/delta;" +
                        "mkdir /data/local/tmp/delta;" +
                        "exit;")

        mPatchDir.eachFileMatch(FileType.FILES, ~/.+\.dex/) { dex ->
            Runtime.runtime.exec("${adbPath} push ${dex.absolutePath} /data/local/tmp/delta/${dex.name}")
        }
    }

}