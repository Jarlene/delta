package org.byteam.delta;

import android.util.Log;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * <p>
 * Created by chenenyu on 16/8/16.
 */
class IOUtils {

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

    public static void closeQuietly(Closeable closeable) {
        if (closeable == null) return;
        try {
            closeable.close();
        } catch (IOException e) {
            Log.w("IOUtils", "Failed to close resource.", e);
        }
    }

}
