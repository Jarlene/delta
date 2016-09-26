package org.byteam.delta.task

import com.android.build.gradle.api.ApplicationVariant
import groovy.io.FileType
import org.apache.commons.io.Charsets
import org.apache.commons.io.FileUtils
import org.byteam.delta.bean.Patch
import org.byteam.delta.util.BsDiffUtils
import org.byteam.delta.util.HashUtils
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
            if (!mPatchDir.exists())
                mPatchDir.mkdirs()

            executeDiff()

            if (pushPatchToDevice) {
                copyPatchToDevice()
            }
        } else {
            println("There are no backup files for patching.")
        }
    }

    private boolean checkFiles() {
        File apkDir = new File(mPatch.backupApkPath)
        return apkDir.exists() && apkDir.directory && apkDir.listFiles() != null
    }

    private void executeDiff() {
        BsDiffUtils.copyBsDiff2BuildFolder(project)
        String bsdiff = BsDiffUtils.getBsDiffFilePath(project)

        File backupApk = new File(mPatch.backupApkPath).listFiles(new FileFilter() {
            @Override
            boolean accept(File file) {
                return file.isFile() && file.getName().endsWith(".apk")
            }
        })[0]

        mVariant.outputs.each {
            File apk = it.getOutputFile()
            if (HashUtils.md5(backupApk).equalsIgnoreCase(HashUtils.md5(apk))) {
                println("Aborted. The hash value between ${backupApk.absolutePath} and " +
                        "${apk.absolutePath} are the same.")
                return
            }
            if (apk != null && apk.name.endsWith(".apk")) {
                Process patch = new ProcessBuilder(bsdiff, backupApk.absolutePath, apk.absolutePath,
                        new File(mPatchDir, apk.name).absolutePath).redirectErrorStream(true).start()
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
                    println("Generate patch files failed, please see 'patch.log' for more info.")
                    FileUtils.writeStringToFile(new File(mPatchDir, 'patch.log'),
                            consoleMsg.toString(), Charsets.UTF_8)
                }
            }
        }
    }

    private void copyPatchToDevice() {
        String destDir = "/data/data/${mVariant.applicationId}/cache/delta"
        String adbPath = project.android.adbExe.absolutePath
        try {
            Runtime.runtime.exec(
                    "${adbPath} shell;" +
                            "su;" +
                            "rm -r ${destDir};" +
                            "mkdir -p ${destDir};" +
                            "exit;")
            mPatchDir.eachFileMatch(FileType.FILES, ~/.+\.apk/) { apk ->
                Runtime.runtime.exec("${adbPath} push ${apk.absolutePath} ${destDir}/${apk.name}")
            }
        } catch (IOException e) {
            e.printStackTrace()
        }
    }

}