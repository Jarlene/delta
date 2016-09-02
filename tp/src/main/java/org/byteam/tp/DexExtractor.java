package org.byteam.tp;

import org.byteam.tp.util.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


/**
 * @Author: chenenyu
 * @Created: 16/8/31 15:22.
 */
class DexExtractor {

    public static void extract(File sourceApk, File destDir) throws IOException {
        InputStream is = null;
        OutputStream os = null;
        try {
            ZipFile apk = new ZipFile(sourceApk);
            Enumeration<ZipEntry> entries = (Enumeration<ZipEntry>) apk.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.getName().matches("classes([1-9]?)\\d*\\.dex$")) {
                    is = apk.getInputStream(entry);
                    os = new FileOutputStream(new File(destDir, entry.getName()));
                    IOUtils.copy(is, os);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(os);
            IOUtils.closeQuietly(is);
        }
    }

}
