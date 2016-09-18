package org.byteam.delta;

import android.content.Context;
import android.support.annotation.NonNull;

import java.io.File;


/**
 * Paths constraint.
 */
final class Paths {
    /**
     * Name of the dex folder in the app's data directory
     */
    public static final String DEX_DIRECTORY_NAME = "dex";

    /**
     * Temp directory on the device
     */
    public static final String DEVICE_TEMP_DIR = "/data/local/tmp";

    /**
     * Name of folder to store the base.apk
     */
    private static final String BASE_APK_DIRECTORY_NAME = "base";

    /**
     * Name of folder to store the new apk
     */
    private static final String NEW_APK_DIRECTORY_NAME = "new";

    /**
     * @param context Context
     * @return /data/data/package/files/delta
     */
    private static File getDeltaBaseDirectory(@NonNull Context context) {
        return new File(context.getFilesDir(), "delta");
    }

    /**
     * @param context Context
     * @return /data/data/package/files/delta/base
     */
    public static File getBaseApkDirectory(@NonNull Context context) {
        return new File(getDeltaBaseDirectory(context), BASE_APK_DIRECTORY_NAME);
    }

    /**
     * @param context Context
     * @return /data/data/package/files/delta/new
     */
    public static File getNewApkDirectory(@NonNull Context context) {
        return new File(getDeltaBaseDirectory(context), NEW_APK_DIRECTORY_NAME);
    }

    /**
     * @param context Context
     * @return /data/data/package/files/delta/{@link #DEX_DIRECTORY_NAME}
     */
    public static File getDexDirectory(@NonNull Context context) {
        return new File(getDeltaBaseDirectory(context), DEX_DIRECTORY_NAME);
    }

    /**
     * @param context Context
     * @return /data/data/package/lib
     */
    public static File getNativeLibraryFolder(@NonNull Context context) {
        return new File(context.getFilesDir(), "lib");
    }

}
