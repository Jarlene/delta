package org.byteam.tp.patch.task

import org.apache.commons.io.FileUtils
import org.byteam.tp.patch.TpPlugin
import org.byteam.tp.patch.bean.Patch
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

import java.nio.charset.Charset

/**
 * Generates patch.
 *
 * @Author: chenenyu
 * @Created: 16/8/29 11:26.
 */
class TpPatchTask extends DefaultTask {

    @Input
    Patch mPatch

    @TaskAction
    void patch() {
        if (checkFiles()) {
            executeDiff()
        } else {
            println("There are no backup files for patch.")
        }
    }

    private boolean checkFiles() {
        File originalDexDir = new File(mPatch.originalDexPath)
        return originalDexDir.exists() && originalDexDir.directory && originalDexDir.listFiles() != null
    }

    private void executeDiff() {
        File patchDir = mPatch.patchDir
        patchDir.mkdirs()

        File[] originalAllDex = new File(mPatch.originalDexPath).listFiles()
        File dexDir = TpPlugin.getDexFolder(project, mPatch)
        dexDir.listFiles().each { dex ->
            File originalDex = originalAllDex.find {
                it.name.equals(dex.name)
            }
            if (originalDex) {
                Process patch = new ProcessBuilder("bsdiff", originalDex.absolutePath, dex.absolutePath,
                        new File(patchDir, dex.name).absolutePath).redirectErrorStream(true).start()
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
                    FileUtils.writeStringToFile(new File(patchDir, 'patch.log'),
                            consoleMsg.toString(), Charset.forName("UTF-8"))
                    println("Generate patch file(s) failed, please see 'patch.log' for more info.")
                }
            } else {
                FileUtils.copyFileToDirectory(dex, patchDir)
            }
        }
    }

}