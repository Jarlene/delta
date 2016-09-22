package org.byteam.delta.util

import org.gradle.internal.hash.HashUtil

/**
 * Hash function utils
 * <p>
 * Created by Cheney on 16/9/22.
 */
class HashUtils {

    /**
     * md5 algorithm
     */
    public static String md5(File file) {
        HashUtil.createHash(file, "MD5").asHexString()
    }
}