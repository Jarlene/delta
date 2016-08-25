package org.byteam.tp.patch.util;
/**
 * @Author: chenenyu
 * @Created: 16/8/17 11:53.
 */
class TpFileUtils {

    static void mkdirs(String path) {
        mkdirs(new File(path))
    }

    static void mkdirs(File file) {
        if (file == null)
            return
        if (file.isDirectory()) {
            file.mkdirs()
        } else {
            if (!file.exists()) {
                file.mkdirs()
            }
        }

    }
}