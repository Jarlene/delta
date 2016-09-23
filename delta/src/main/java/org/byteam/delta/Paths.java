package org.byteam.delta;

import android.content.Context;

import java.io.File;


/**
 * Paths constraint.
 */
final class Paths {

    private static final String DELTA = "delta";

    /**
     * Name of folder to store the base.apk
     */
    private static final String BASE_APK_DIRECTORY_NAME = "base";

    /**
     * Name of folder to store the new apk
     */
    private static final String NEW_APK_DIRECTORY_NAME = "new";

    /**
     * Name of folder to store the dexes
     */
    private static final String DEX_DIRECTORY_NAME = "dex";

    /**
     * Name of folder to store .so
     */
    private static final String LIB_DIRECTORY_NAME = "lib";

    /**
     * Name of folder to store odex
     */
    private static final String CODE_CACHE_NAME = "code_cache";

    /**
     * /data/data/package/files/delta
     */
    private static File getBaseDirectory(Context context) {
        return new File(context.getFilesDir(), DELTA);
    }

    /**
     * /data/data/package/files/delta/base
     */
    public static File getBaseApkDirectory(Context context) {
        return new File(getBaseDirectory(context), BASE_APK_DIRECTORY_NAME);
    }

    /**
     * /data/data/package/files/delta/new
     */
    public static File getNewApkDirectory(Context context) {
        return new File(getBaseDirectory(context), NEW_APK_DIRECTORY_NAME);
    }

    /**
     * /data/data/package/files/delta/dex
     */
    public static File getDexDirectory(Context context) {
        return new File(getBaseDirectory(context), DEX_DIRECTORY_NAME);
    }

    /**
     * /data/data/package/lib
     */
    public static File getNativeLibraryDirectory(Context context) {
        return new File(context.getFilesDir(), LIB_DIRECTORY_NAME);
    }

    /**
     * /data/data/package/cache/delta
     */
    public static File getCacheDirectory(Context context) {
        return new File(context.getCacheDir(), DELTA);
    }

    /**
     * /data/data/package/code_cache
     * or
     * /data/data/package/files/code_cache
     */
    public static File getCodeCacheDirectory(Context context) {
        File cache = new File(context.getApplicationInfo().dataDir, CODE_CACHE_NAME);
        cache.mkdirs();
        if (!cache.isDirectory()) {
            cache = new File(context.getFilesDir(), CODE_CACHE_NAME);
            cache.mkdirs();
        }
        return cache;
    }

}
