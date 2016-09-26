package org.byteam.delta;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;


/**
 * <a href="https://github.com/byteam/delta">Home page</a>
 */
public class Delta {
    static final String TAG = "delta";

    private static PatchListener mPatchListener;

    private Delta() {
    }

    public static void applyPatchForTest(Context context, PatchListener patchListener) {
        if (!isDebug(context)) {
            Log.w(TAG, "Running test function in non-debug build type!");
        }

        File testPatchDir = Paths.getCacheDirectory(context);
        if (!testPatchDir.exists()) {
            testPatchDir.mkdirs();
        }
        if (!testPatchDir.canRead()) {
            patchListener.patchResult(false, testPatchDir.getPath() + " can't read.");
            return;
        }
        if (!testPatchDir.isDirectory()) {
            patchListener.patchResult(false, testPatchDir.getPath() + " is not a directory.");
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) !=
                    PackageManager.PERMISSION_GRANTED) {
                patchListener.patchResult(false,
                        "Permission: " + Manifest.permission.READ_EXTERNAL_STORAGE + " required.");
                return;
            }
        }

        File[] patches = testPatchDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".apk");
            }
        });
        if (patches == null) {
            patchListener.patchResult(false, testPatchDir.getPath() + " does not denote a directory.");
            return;
        }
        if (patches.length < 1) {
            patchListener.patchResult(false, "Can not find patch file in cache dir.");
            return;
        }
        applyPatch(context, patches[0], patchListener);
    }

    public static void applyPatch(Context context, File apkFile, PatchListener patchListener) {
        applyPatch(context, apkFile, true, patchListener);
    }

    public static void applyPatch(Context context, File apkFile, boolean isPatch, PatchListener patchListener) {
        if (context == null) {
            throw new NullPointerException("context can not be null.");
        }
        if (apkFile == null) {
            throw new NullPointerException("apkFile can not be null.");
        }
        if (!apkFile.exists()) {
            throw new IllegalArgumentException("Invalid apk: " + apkFile.getPath() + " not exists.");
        }
        if (apkFile.isDirectory()) {
            throw new IllegalArgumentException("Invalid apk: " + apkFile.getPath() + "is a directory.");
        }
        if (!apkFile.canRead()) {
            throw new IllegalArgumentException("Invalid apk: " + apkFile.getPath() + " can't read.");
        }
        if (patchListener == null) {
            throw new NullPointerException("You must offer a 'PatchListener'.");
        }

        mPatchListener = patchListener;

        if (isPatch) {
            combineApk(context, apkFile);
        } else {
            try {
                FileUtils.copyFileToDirectory(apkFile, Paths.getNewApkDirectory(context));
            } catch (IOException e) {
                Log.w(TAG, "Failed to copy " + apkFile.getName() + " to " + Paths.getNewApkDirectory(context), e);
            }
            extractDex(context, apkFile);
        }
    }

    private static void combineApk(Context context, File patchApk) {
        String apk = context.getApplicationInfo().publicSourceDir;
        File baseApkDir = Paths.getBaseApkDirectory(context);
        if (!baseApkDir.exists()) {
            if (!baseApkDir.mkdirs()) {
                Log.e(TAG, "Failed to mkdirs: " + baseApkDir.getPath());
                if (mPatchListener != null) {
                    mPatchListener.patchResult(false, "Failed to mkdirs: " + baseApkDir.getPath());
                }
                return;
            }
        } else {
            try {
                FileUtils.cleanDirectory(baseApkDir);
            } catch (IOException e) {
                Log.e(TAG, "Can not clean directory " + baseApkDir.getPath(), e);
                if (mPatchListener != null) {
                    mPatchListener.patchResult(false, "Can not clean directory " + baseApkDir.getPath());
                }
                return;
            }
        }
        try {
            FileUtils.copyFileToDirectory(new File(apk), baseApkDir);
        } catch (IOException e) {
            Log.e(TAG, "Failure while extracting base.apk.", e);
            if (mPatchListener != null) {
                mPatchListener.patchResult(false, "Failure while extracting base.apk.");
            }
            return;
        }
        File[] apks = baseApkDir.listFiles();
        File baseApk;
        if (apks != null && apks.length > 0) {
            baseApk = apks[0];
            // Check md5
            if (!HashUtils.md5(new File(apk)).equals(HashUtils.md5(baseApk))) {
                if (mPatchListener != null) {
                    mPatchListener.patchResult(false,
                            "MD5: (" + apk + ") != MD5: (" + baseApk.getPath() + ")");
                }
                try {
                    FileUtils.cleanDirectory(baseApkDir);
                } catch (IOException e) {
                    // pass
                }
                return;
            }
        } else {
            Log.e(TAG, "Empty base apk dir.");
            if (mPatchListener != null) {
                mPatchListener.patchResult(false, "Empty base apk dir: " + baseApkDir.getPath());
            }
            return;
        }
        File newApk = new File(Paths.getNewApkDirectory(context), baseApk.getName());
        Patch patch = new Patch(baseApk.getAbsolutePath(), newApk.getAbsolutePath(),
                patchApk.getAbsolutePath());

        // Start a cross-processing service to execute patch.
        Intent patchIntent = new Intent(context, PatchService.class);
        patchIntent.putExtra(PatchService.PATCH_FLAG, patch);
        context.startService(patchIntent);
    }

    /**
     * Called by {@link PatchReceiver}.
     */
    static void patchResult(Context context, Patch patch) {
        if (patch == null) {
            Log.e(TAG, "Null patch result");
            if (mPatchListener != null) {
                mPatchListener.patchResult(false, "Null patch result");
            }
        } else {
            Log.i(TAG, "old:   " + HashUtils.md5(new File(patch.getOldPath())));
            Log.i(TAG, "new:   " + HashUtils.md5(new File(patch.getNewPath())));
            Log.i(TAG, "patch: " + HashUtils.md5(new File(patch.getPatchPath())));
            extractDex(context, new File(patch.getNewPath()));
        }
    }

    private static void extractDex(Context context, File target) {
        try {
            Extractor.extractDex(target, Paths.getDexDirectory(context));
        } catch (IOException e) {
            Log.e(TAG, "Failure while extracting dex from " + target.getPath(), e);
            if (mPatchListener != null) {
                mPatchListener.patchResult(false, "Failure while extracting dex from " + target.getPath());
            }
            return;
        }
        // TODO: 16/9/18  提取dex之后是否odex?
        if (mPatchListener != null) {
            mPatchListener.patchResult(true, null);
        }
    }

    /**
     * Clear all the patch file.
     */
    public static void clear(Context context) {
        try {
            if (Paths.getDexDirectory(context).exists()) {
                FileUtils.cleanDirectory(Paths.getDexDirectory(context));
            }
            if (Paths.getBaseApkDirectory(context).exists()) {
                FileUtils.cleanDirectory(Paths.getBaseApkDirectory(context));
            }
            if (Paths.getNewApkDirectory(context).exists()) {
                FileUtils.cleanDirectory(Paths.getNewApkDirectory(context));
            }
            if (Paths.getCacheDirectory(context).exists()) {
                FileUtils.cleanDirectory(Paths.getCacheDirectory(context));
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to clean patch file.", e);
        }
    }

    private static boolean isDebug(Context context) {
        try {
            Class<?> clazz = Class.forName(context.getPackageName() + ".BuildConfig");
            Object debug = clazz.getDeclaredField("DEBUG").get(null);
            return (boolean) debug;
        } catch (Exception e) {
            // ClassNotFoundException: Maybe caused by proguard.
        }
        return false;
    }

}
