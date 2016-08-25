package org.byteam.tp.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @Author: chenenyu
 * @Created: 16/8/16 17:11.
 */
public class IOUtils {

    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    public static long copy(final InputStream input, final OutputStream output) throws IOException {
        final long count = copy(input, output, DEFAULT_BUFFER_SIZE);
        if (count > Integer.MAX_VALUE) {
            return -1;
        }
        return count;
    }

    public static long copy(final InputStream input, final OutputStream output, final int bufferSize) throws IOException {
        long count = 0;
        int n;
        byte[] buffer = new byte[bufferSize];
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    /**
     * 递归删除指定file.
     *
     * @param file file or directory
     */
    public static void deleteFiles(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isFile()) {
            file.delete();
        } else {
            File[] files = file.listFiles();
            for (File f : files) {
                deleteFiles(f);
            }
        }
    }

}
