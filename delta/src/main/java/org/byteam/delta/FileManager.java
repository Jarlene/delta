package org.byteam.delta;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Class which handles locating existing code and resource files on the device,
 * as well as writing new versions of these.
 * <p>
 * Created by chenenyu on 16/9/14.
 */
class FileManager {

    /**
     * Returns the list of available .dex files to be loaded, possibly empty.
     */
    public static List<String> getDexList(Context context) {
        File dexDir = Paths.getDexDirectory(context);
        String[] dexes = null;
        if (dexDir.exists()) {
            dexes = dexDir.list(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".dex");
                }
            });
        } else {
            dexDir.mkdirs();
        }
        return dexes == null ? Collections.<String>emptyList() : Arrays.asList(dexes);
    }

    /**
     * Returns the new apk, possibly null.
     */
    public static String getNewApk(Context context) {
        File apkDir = Paths.getNewApkDirectory(context);
        if (apkDir.exists()) {
            File[] apks = apkDir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".apk");
                }
            });
            if (apks.length < 1) {
                Log.i(Delta.TAG, "There is no apk found in directory" + apkDir.getPath());
                return null;
            } else if (apks.length == 1) {
                return apks[0].getAbsolutePath();
            } else {
                Log.w(Delta.TAG, "Multiple apks was found in directory" + apkDir.getPath());
                return findLatestApk(apks);
            }
        } else {
            apkDir.mkdirs();
            return null;
        }
    }

    private static String findLatestApk(File[] apks) {
        long modified = 0L;
        String latestApk = null;
        for (File apk : apks) {
            if (apk.lastModified() > modified) {
                modified = apk.lastModified();
                latestApk = apk.getAbsolutePath();
            }
        }
        return latestApk;
    }

}
