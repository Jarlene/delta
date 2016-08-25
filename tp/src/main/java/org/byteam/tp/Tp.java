package org.byteam.tp;

import android.app.Application;
import android.content.pm.ApplicationInfo;

import org.byteam.tp.patch.TpPatch;
import org.byteam.tp.util.DexUtils;
import org.byteam.tp.util.IOUtils;
import org.byteam.tp.util.Logger;
import org.byteam.tp.util.ReflectionUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @Author: chenenyu
 * @Created: 16/8/15 15:29.
 */
public class Tp {
    private static final String TAG = "tp";
    private static final String DEX_SUFFIX = ".dex";
    private static final String DEX_OPT_DIR = "opt_patch_dex";
    private static Application mApplication;
    // 存放apk中的dex
    private static String mOriginalDexPath;
    // 存放patch后的dex
    private static String mPatchedDexPath;

    public static void init(Application application) {
        mApplication = application;
        Logger.setDebug(isDebug(application));
        initPath();
        mountAllDex();
    }

    private static void initPath() {
        mOriginalDexPath = mApplication.getFilesDir() + File.separator + TAG + File.separator + "original";
        mPatchedDexPath = mApplication.getFilesDir() + File.separator + TAG + File.separator + "patched";
        File originalPath = new File(mOriginalDexPath);
        if (!originalPath.exists()) {
            originalPath.mkdirs();
        }
        File patchedPath = new File(mPatchedDexPath);
        if (!patchedPath.exists()) {
            patchedPath.mkdirs();
        }
    }

    public static boolean patch(File patchDex, PatchChecker patchChecker) {
        if (patchDex == null || !patchDex.exists()) {
            Logger.e("Invalid patch dex. Skipped.");
            return false;
        }
        if (!patchChecker.verifyPatch()) {
            Logger.e("Verify patch dex failed.");
            return false;
        }
        String patchName = patchDex.getName();
        copyDex(patchDex);
        File orignalDex = new File(mOriginalDexPath, ); // // TODO: 16/8/17 怎么找
        File patchedDex = new File(mPatchedDexPath, patchName); // Not exists now
        if (orignalDex.exists()) {
            int patchResult = TpPatch.patch(orignalDex.getPath(), patchedDex.getPath(), patchDex.getPath());
            if (patchResult == 0) {
                Logger.i("Patch dexes successfully");
                IOUtils.deleteFiles(new File(mOriginalDexPath));
                return true;
            }
        }
        Logger.i("Patch dexes failed");
        return false;
    }

    /**
     * 将base.apk中的dex拷贝到dex目录下
     *
     * @param patchDex
     */
    private static void copyDex(File patchDex) {
        // 如果dex目录下已存在当前patch,则跳过
//        if (new File(mOriginalDexPath, patchDex.getName()).exists()) {
//            return;
//        }
        InputStream is = null;
        OutputStream os = null;
        try {
            String baseApkPath = getBaseApkPath(mApplication);
            JarFile apk = new JarFile(baseApkPath);
            Enumeration<JarEntry> entries = apk.entries();
            // TODO: 16/8/17 假如apk中有多个dex?
            while (entries.hasMoreElements()) {
                JarEntry jarEntry = entries.nextElement();
                if (jarEntry != null && jarEntry.getName().endsWith(DEX_SUFFIX)) {
                    is = apk.getInputStream(jarEntry);
                    os = new FileOutputStream(new File(mOriginalDexPath, jarEntry.getName()));
                    IOUtils.copy(is, os);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * base.apk
     *
     * @param application Application
     * @return Full path to the base APK for this application.
     */
    private static String getBaseApkPath(Application application) {
        ApplicationInfo appInfo = application.getApplicationInfo();
        return appInfo.sourceDir;
    }

    private static void mountDex(File dex) {
        File dexOptDir = new File(mApplication.getFilesDir(), DEX_OPT_DIR);
        if (!dexOptDir.exists()) {
            dexOptDir.mkdirs();
        }
        try {
            DexUtils.injectDexAtFirst(dex.getPath(), dexOptDir.getAbsolutePath());
            Logger.i("Load patch succeed:" + dex.getAbsolutePath());
        } catch (Exception e) {
            Logger.w("Load patch failed:" + dex.getAbsolutePath());
            e.printStackTrace();
        }
    }

    private static void mountAllDex() {
        File[] files = new File(mPatchedDexPath).listFiles();
        if (files != null) {
            for (File file : files) {
                mountDex(file);
            }
        }
    }

    private static boolean isDebug(Application application) {
        try {
            Class<?> clazz = Class.forName(application.getPackageName() + ".BuildConfig");
            Object debug = ReflectionUtils.getField(null, clazz, "DEBUG");
            return (boolean) debug;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    public interface PatchChecker {
        boolean verifyPatch();
    }
}
