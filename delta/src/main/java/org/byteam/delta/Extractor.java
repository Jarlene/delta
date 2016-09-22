package org.byteam.delta;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


/**
 * Extractor for apk and dex.
 * <p>
 * Created by chenenyu on 16/8/31.
 */
class Extractor {

    /**
     * Extract dex(es) from specify apk.
     *
     * @param sourceApk apk
     * @param destDir   destination dir
     */
    static void extractDex(File sourceApk, File destDir) throws IOException {
        if (sourceApk == null) {
            throw new NullPointerException("Source must not be null");
        }
        if (destDir == null) {
            throw new NullPointerException("Destination must not be null");
        }
        if (destDir.exists() && !destDir.isDirectory()) {
            throw new IllegalArgumentException("Destination '" + destDir + "' is not a directory");
        }
        if (!destDir.exists()) {
            destDir.mkdirs();
        } else {
            FileUtils.cleanDirectory(destDir);
        }
        InputStream is = null;
        OutputStream os = null;
        try {
            ZipFile apk = new ZipFile(sourceApk);
            Enumeration<? extends ZipEntry> entries = apk.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.getName().startsWith("classes") && entry.getName().endsWith(".dex")) {
                    is = apk.getInputStream(entry);
                    os = new FileOutputStream(new File(destDir, entry.getName()));
                    IOUtils.copy(is, os);
                }
            }
        } finally {
            IOUtils.closeQuietly(os);
            IOUtils.closeQuietly(is);
        }
    }

}
