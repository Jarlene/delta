package org.byteam.tp;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.AssetManager;
import android.os.Build;
import android.util.Log;

import org.byteam.tp.patch.TpPatch;
import org.byteam.tp.util.IOUtils;
import org.byteam.tp.util.ReflectionUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.zip.ZipFile;

import dalvik.system.DexFile;

import static org.byteam.tp.util.ReflectionUtils.findField;
import static org.byteam.tp.util.ReflectionUtils.injectPatchDexAtFirst;


/**
 * @Author: chenenyu
 * @Created: 16/8/31 15:57.
 */
public class Tp {
    static final String TAG = "tp";

    private static final String CODE_CACHE_NAME = "code_cache";

    private static final String CODE_CACHE_SECONDARY_FOLDER_NAME = "tp-dexes";

    private static final String OLD_SECONDARY_FOLDER_NAME = "tp-dexes";

    private static String APK_DEX_DIR = "tp_original";

    private static File mApkDexDir; // 存放apk中的dex

    private static String PATCHED_DEX_DIR = "tp_patched";

    private static File mPatchedDexDir; // 存放打好补丁的dex

    private static final Set<String> installedApk = new HashSet<>();

    private Tp() {
    }

    /**
     * 从assets中合并patch,便于测试.
     *
     * @param context Context
     */
    public static void applyPatchFromAssets(Context context) {
        String patchDirInAssets = "patch";
        AssetManager assetManager = context.getAssets();
        InputStream is = null;
        OutputStream os = null;
        try {
            String[] dexes = assetManager.list(patchDirInAssets);
            if (dexes.length > 0) {
                File tmpPatchDir = new File(context.getCacheDir(), "tp_tmp_patch");
                mkdirChecked(tmpPatchDir);
                IOUtils.cleanDirectory(tmpPatchDir);
                for (String dex : dexes) {
                    File destFile = new File(tmpPatchDir, dex);
                    is = assetManager.open(patchDirInAssets + "/" + dex);
                    os = new FileOutputStream(destFile);
                    IOUtils.copy(is, os);

                    applyPatch(context, destFile);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Can not find patches from assets. ", e);
        } finally {
            IOUtils.closeQuietly(os);
            IOUtils.closeQuietly(is);
        }

    }

    /**
     * 合并指定的patch.
     *
     * @param context  Context
     * @param patchDex patch文件
     */
    public static void applyPatch(Context context, File patchDex) {
        if (patchDex == null || !patchDex.exists() || patchDex.isDirectory()) return;

        initDir(context);

        try {
            File apk = new File(getApplicationInfo(context).sourceDir);
            DexExtractor.extract(apk, mApkDexDir);
        } catch (IOException e) {
            Log.w(TAG, "Failure while trying to extract dex from base.apk.", e);
            return;
        }

        File[] apkDexes = mApkDexDir.listFiles();
        if (apkDexes == null) {
            Log.e(TAG, "The apk dex dir is invalid.");
            return;
        }
        if (apkDexes.length == 1) {
            int patchResult = TpPatch.patch(apkDexes[0].getAbsolutePath(),
                    new File(mPatchedDexDir, apkDexes[0].getName()).getAbsolutePath(),
                    patchDex.getAbsolutePath());
            if (patchResult == 0) {
                Log.i(TAG, "Patched dex successfully.");
            } else {
                Log.e(TAG, "Failed to patch dex: " + patchDex.getAbsolutePath());
            }
        } else if (apkDexes.length > 1) {
            for (File apkDex : apkDexes) {
                if (patchDex.getName().equals(apkDex.getName())) {
                    int patchResult = TpPatch.patch(apkDex.getAbsolutePath(),
                            new File(mPatchedDexDir, apkDex.getName()).getAbsolutePath(),
                            patchDex.getAbsolutePath());
                    if (patchResult == 0) {
                        Log.i(TAG, "Patched dex successfully.");
                    } else {
                        Log.e(TAG, "Failed to patch dex: " + patchDex.getAbsolutePath());
                    }
                } else {
                    Log.i(TAG, "Mismatch dex name.");
                }
            }
        }
    }

    /**
     * 初始化存放补丁的文件夹
     *
     * @param context Context
     */
    private static void initDir(Context context) {
        if (mApkDexDir == null)
            mApkDexDir = new File(context.getFilesDir(), APK_DEX_DIR);
        if (mPatchedDexDir == null)
            mPatchedDexDir = new File(context.getFilesDir(), PATCHED_DEX_DIR);
        try {
            mkdirChecked(mApkDexDir);
            IOUtils.cleanDirectory(mApkDexDir);
            mkdirChecked(mPatchedDexDir);
        } catch (IOException e) {
            Log.e(TAG, "Failed to create directory.", e);
        }
    }

    /**
     * Patches the application context class loader by appending extra dex files
     * loaded from the application apk. This method should be called in the
     * attachBaseContext of your {@link Application}.
     *
     * @param context application context.
     */
    public static void install(Context context) {
        Log.i(TAG, "install");

        initDir(context);
        File[] patchedDex = mPatchedDexDir.listFiles();
        if (patchedDex == null || patchedDex.length < 1) {
            Log.i(TAG, "Null patched dex");
            return;
        }

        try {
            ApplicationInfo applicationInfo = getApplicationInfo(context);
            if (applicationInfo == null) {
                // Looks like running on a test Context, so just return without patching.
                return;
            }

            synchronized (installedApk) {
                String apkPath = applicationInfo.sourceDir;
                if (installedApk.contains(apkPath)) {
                    return;
                }
                installedApk.add(apkPath);

                /* The patched class loader is expected to be a descendant of
                 * dalvik.system.BaseDexClassLoader. We modify its
                 * dalvik.system.DexPathList pathList field to append additional DEX
                 * file entries.
                 */
                ClassLoader loader;
                try {
                    loader = context.getClassLoader();
                } catch (RuntimeException e) {
                    /* Ignore those exceptions so that we don't break tests relying on Context like
                     * a android.test.mock.MockContext or a android.content.ContextWrapper with a
                     * null base Context.
                     */
                    Log.w(TAG, "Failure while trying to obtain Context class loader. " +
                            "Must be running in test mode. Skip patching.", e);
                    return;
                }
                if (loader == null) {
                    // Note, the context class loader is null when running Robolectric tests.
                    Log.e(TAG,
                            "Context class loader is null. Must be running in test mode. "
                                    + "Skip patching.");
                    return;
                }

                try {
                    clearOldDexDir(context);
                } catch (Throwable t) {
                    Log.w(TAG, "Something went wrong when trying to clear old dex extraction, "
                            + "continuing without cleaning.", t);
                }

                // optimizedDirectory
                File dexDir = getDexDir(context, applicationInfo);
                installAllPatchedDexes(loader, dexDir, Arrays.asList(patchedDex));
            }

        } catch (Exception e) {
            Log.e(TAG, "Tp installation failure", e);
            throw new RuntimeException("Tp installation failed (" + e.getMessage() + ").");
        }
        Log.i(TAG, "install done");
    }

    public static void clean(Context context) {
        initDir(context);
        IOUtils.cleanDirectory(mPatchedDexDir);
    }

    private static ApplicationInfo getApplicationInfo(Context context) {
        return context.getApplicationInfo();
    }

    private static void clearOldDexDir(Context context) throws Exception {
        File dexDir = new File(context.getFilesDir(), OLD_SECONDARY_FOLDER_NAME);
        if (dexDir.isDirectory()) {
            Log.i(TAG, "Clearing old secondary dex dir (" + dexDir.getPath() + ").");
            File[] files = dexDir.listFiles();
            if (files == null) {
                Log.w(TAG, "Failed to list secondary dex dir content (" + dexDir.getPath() + ").");
                return;
            }
            for (File oldFile : files) {
                Log.i(TAG, "Trying to delete old file " + oldFile.getPath() + " of size "
                        + oldFile.length());
                if (!oldFile.delete()) {
                    Log.w(TAG, "Failed to delete old file " + oldFile.getPath());
                } else {
                    Log.i(TAG, "Deleted old file " + oldFile.getPath());
                }
            }
            if (!dexDir.delete()) {
                Log.w(TAG, "Failed to delete secondary dex dir " + dexDir.getPath());
            } else {
                Log.i(TAG, "Deleted old secondary dex dir " + dexDir.getPath());
            }
        }
    }

    private static File getDexDir(Context context, ApplicationInfo applicationInfo)
            throws IOException {
        File cache = new File(applicationInfo.dataDir, CODE_CACHE_NAME);
        try {
            mkdirChecked(cache);
        } catch (IOException e) {
            /* If we can't emulate code_cache, then store to filesDir. This means abandoning useless
             * files on disk if the device ever updates to android 5+. But since this seems to
             * happen only on some devices running android 2, this should cause no pollution.
             */
            cache = new File(context.getFilesDir(), CODE_CACHE_NAME);
            mkdirChecked(cache);
        }
        File dexDir = new File(cache, CODE_CACHE_SECONDARY_FOLDER_NAME);
        mkdirChecked(dexDir);
        return dexDir;
    }

    private static void mkdirChecked(File dir) throws IOException {
        dir.mkdir();
        if (!dir.isDirectory()) {
            File parent = dir.getParentFile();
            if (parent == null) {
                Log.e(TAG, "Failed to create dir " + dir.getPath() + ". Parent file is null.");
            } else {
                Log.e(TAG, "Failed to create dir " + dir.getPath() +
                        ". parent file is a dir " + parent.isDirectory() +
                        ", a file " + parent.isFile() +
                        ", exists " + parent.exists() +
                        ", readable " + parent.canRead() +
                        ", writable " + parent.canWrite());
            }
            throw new IOException("Failed to create directory " + dir.getPath());
        }
    }

    private static void installAllPatchedDexes(ClassLoader loader, File dexDir, List<File> files)
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException,
            InvocationTargetException, NoSuchMethodException, IOException {
        if (!files.isEmpty()) {
            if (Build.VERSION.SDK_INT >= 19) {
                V19.install(loader, files, dexDir);
            } else if (Build.VERSION.SDK_INT >= 14) {
                V14.install(loader, files, dexDir);
            } else {
                V4.install(loader, files);
            }
        }
    }

    /**
     * Installer for platform versions 19.
     */
    private static final class V19 {

        private static void install(ClassLoader loader, List<File> additionalClassPathEntries,
                                    File optimizedDirectory)
                throws IllegalArgumentException, IllegalAccessException,
                NoSuchFieldException, InvocationTargetException, NoSuchMethodException {
            /* The patched class loader is expected to be a descendant of
             * dalvik.system.BaseDexClassLoader. We modify its
             * dalvik.system.DexPathList pathList field to append additional DEX
             * file entries.
             */
            Field pathListField = findField(loader, "pathList");
            Object dexPathList = pathListField.get(loader);
            ArrayList<IOException> suppressedExceptions = new ArrayList<>();
            injectPatchDexAtFirst(dexPathList, "dexElements", makeDexElements(dexPathList,
                    new ArrayList<>(additionalClassPathEntries), optimizedDirectory,
                    suppressedExceptions));
            if (suppressedExceptions.size() > 0) {
                for (IOException e : suppressedExceptions) {
                    Log.w(TAG, "Exception in makeDexElement", e);
                }
                Field suppressedExceptionsField =
                        findField(dexPathList, "dexElementsSuppressedExceptions");
                IOException[] dexElementsSuppressedExceptions =
                        (IOException[]) suppressedExceptionsField.get(dexPathList);

                if (dexElementsSuppressedExceptions == null) {
                    dexElementsSuppressedExceptions =
                            suppressedExceptions.toArray(
                                    new IOException[suppressedExceptions.size()]);
                } else {
                    IOException[] combined = new IOException[suppressedExceptions.size() +
                            dexElementsSuppressedExceptions.length];
                    suppressedExceptions.toArray(combined);
                    System.arraycopy(dexElementsSuppressedExceptions, 0, combined,
                            suppressedExceptions.size(), dexElementsSuppressedExceptions.length);
                    dexElementsSuppressedExceptions = combined;
                }

                suppressedExceptionsField.set(dexPathList, dexElementsSuppressedExceptions);
            }
        }

        /**
         * A wrapper around
         * {@code private static final dalvik.system.DexPathList#makeDexElements}.
         */
        private static Object[] makeDexElements(
                Object dexPathList, ArrayList<File> files, File optimizedDirectory,
                ArrayList<IOException> suppressedExceptions)
                throws IllegalAccessException, InvocationTargetException,
                NoSuchMethodException {
            Method makeDexElements = ReflectionUtils.findMethod(dexPathList, "makeDexElements",
                    ArrayList.class, File.class, ArrayList.class);

            return (Object[]) makeDexElements.invoke(dexPathList, files, optimizedDirectory,
                    suppressedExceptions);
        }
    }

    /**
     * Installer for platform versions 14, 15, 16, 17 and 18.
     */
    private static final class V14 {

        private static void install(ClassLoader loader, List<File> additionalClassPathEntries,
                                    File optimizedDirectory)
                throws IllegalArgumentException, IllegalAccessException,
                NoSuchFieldException, InvocationTargetException, NoSuchMethodException {
            /* The patched class loader is expected to be a descendant of
             * dalvik.system.BaseDexClassLoader. We modify its
             * dalvik.system.DexPathList pathList field to append additional DEX
             * file entries.
             */
            Field pathListField = findField(loader, "pathList");
            Object dexPathList = pathListField.get(loader);
            injectPatchDexAtFirst(dexPathList, "dexElements", makeDexElements(dexPathList,
                    new ArrayList<>(additionalClassPathEntries), optimizedDirectory));
        }

        /**
         * A wrapper around
         * {@code private static final dalvik.system.DexPathList#makeDexElements}.
         */
        private static Object[] makeDexElements(
                Object dexPathList, ArrayList<File> files, File optimizedDirectory)
                throws IllegalAccessException, InvocationTargetException,
                NoSuchMethodException {
            Method makeDexElements = ReflectionUtils.findMethod(dexPathList, "makeDexElements",
                    ArrayList.class, File.class);

            return (Object[]) makeDexElements.invoke(dexPathList, files, optimizedDirectory);
        }
    }

    /**
     * Installer for platform versions 4 to 13.
     */
    private static final class V4 {
        private static void install(ClassLoader loader, List<File> additionalClassPathEntries)
                throws IllegalArgumentException, IllegalAccessException,
                NoSuchFieldException, IOException {
            /* The patched class loader is expected to be a descendant of
             * dalvik.system.DexClassLoader. We modify its
             * fields mPaths, mFiles, mZips and mDexs to append additional DEX
             * file entries.
             */
            int extraSize = additionalClassPathEntries.size();

            Field pathField = findField(loader, "path");

            StringBuilder path = new StringBuilder((String) pathField.get(loader));
            String[] extraPaths = new String[extraSize];
            File[] extraFiles = new File[extraSize];
            ZipFile[] extraZips = new ZipFile[extraSize];
            DexFile[] extraDexs = new DexFile[extraSize];
            for (ListIterator<File> iterator = additionalClassPathEntries.listIterator();
                 iterator.hasNext(); ) {
                File additionalEntry = iterator.next();
                String entryPath = additionalEntry.getAbsolutePath();
                path.append(':').append(entryPath);
                int index = iterator.previousIndex();
                extraPaths[index] = entryPath;
                extraFiles[index] = additionalEntry;
                extraZips[index] = new ZipFile(additionalEntry);
                extraDexs[index] = DexFile.loadDex(entryPath, entryPath + ".dex", 0);
            }

            pathField.set(loader, path.toString());
            injectPatchDexAtFirst(loader, "mPaths", extraPaths);
            injectPatchDexAtFirst(loader, "mFiles", extraFiles);
            injectPatchDexAtFirst(loader, "mZips", extraZips);
            injectPatchDexAtFirst(loader, "mDexs", extraDexs);
        }
    }
}
