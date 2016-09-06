package org.byteam.delta.util

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.Project;

/**
 * @Author: chenenyu
 * @Created: 16/9/5 17:40.
 */
class BsDiffUtils {

    private static final def name = "bsdiff";

    static void copyBsDiff2BuildFolder(Project project) {
        def bsdiffDir = getBsDiffDir(project)
        if (!bsdiffDir.exists()) {
            bsdiffDir.mkdirs()
        }
        def bsdiff = new File(getBsDiffFilePath(project))
        if (!bsdiff.exists()) {
            new FileOutputStream(bsdiff).withStream {
                def inputStream = BsDiffUtils.class.getResourceAsStream("/$name/${getFilename()}")
                it.write(inputStream.getBytes())
            }
        }
        bsdiff.setExecutable(true, false)
    }

    /**
     * .../build/bsdiff
     * @return File (Directory)
     */
    private static File getBsDiffDir(Project project) {
        return new File(getBsDiffPath(project))
    }

    /**
     * .../build/bsdiff
     * @return String
     */
    private static String getBsDiffPath(Project project) {
        project.buildDir.absolutePath + File.separator + "$name"
    }

    /**
     * .../build/bsdiff/{bsdiff-linux/bsdiff-mac/bsdiff.exe}.
     *
     * @return String
     */
    static String getBsDiffFilePath(Project project) {
        getBsDiffPath(project) + File.separator + getFilename()
    }

    static String getFilename() {
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            "${name}.exe"
        } else if (Os.isFamily(Os.FAMILY_MAC)) {
            "${name}-mac"
        } else {
            "${name}-linux"
        }
    }

}