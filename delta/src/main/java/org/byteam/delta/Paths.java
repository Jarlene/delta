package org.byteam.delta;

import android.content.Context;
import android.support.annotation.NonNull;

import java.io.File;


/**
 * Paths constraint.
 */
final class Paths {

    private static final String BASE_NAME = "delta";

    /**
     * Temp directory on the device
     */
    private static final String DEVICE_TEMP_DIR = "/data/local/tmp";

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
     * @return /data/data/package/files/delta
     */
    private static File getBaseDirectory(@NonNull Context context) {
        return new File(context.getFilesDir(), BASE_NAME);
    }

    /**
     * @return /data/data/package/files/delta/base
     */
    public static File getBaseApkDirectory(@NonNull Context context) {
        return new File(getBaseDirectory(context), BASE_APK_DIRECTORY_NAME);
    }

    /**
     * @return /data/data/package/files/delta/new
     */
    public static File getNewApkDirectory(@NonNull Context context) {
        return new File(getBaseDirectory(context), NEW_APK_DIRECTORY_NAME);
    }

    /**
     * @return /data/data/package/files/delta/dex
     */
    public static File getDexDirectory(@NonNull Context context) {
        return new File(getBaseDirectory(context), DEX_DIRECTORY_NAME);
    }

    /**
     * @return /data/data/package/lib
     */
    public static File getNativeLibraryFolder(@NonNull Context context) {
        return new File(context.getFilesDir(), LIB_DIRECTORY_NAME);
    }

    /**
     * @return /data/local/tmp/package_delta
     */
    public static File getTmpDir(@NonNull Context context) {
        return new File(DEVICE_TEMP_DIR, context.getPackageName() + "_" + BASE_NAME);
    }

}
