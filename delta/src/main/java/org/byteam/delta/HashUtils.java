package org.byteam.delta;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Hash utils
 * <p>
 * Created by Cheney on 16/9/23.
 */
public class HashUtils {

    public static String md5(File file) {
        return getHash(file, "MD5");
    }

    public static String sha1(File file) {
        return getHash(file, "SHA-1");
    }

    public static String getHash(File file, String algorithm) {
        if (!file.exists() || !file.isFile()) {
            return null;
        }
        InputStream is = null;
        try {
            is = new FileInputStream(file);
            byte[] buffer = new byte[4096];
            MessageDigest messageDigest = MessageDigest.getInstance(algorithm);
            while (true) {
                int nread = is.read(buffer);
                if (nread < 0) {
                    BigInteger bi = new BigInteger(1, messageDigest.digest());
                    return bi.toString(16);
                }
                messageDigest.update(buffer, 0, nread);
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (is != null)
                try {
                    is.close();
                } catch (IOException e) {
                    // pass
                }
        }
        return null;
    }
}
