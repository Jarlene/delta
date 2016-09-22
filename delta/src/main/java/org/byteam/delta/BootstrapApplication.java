package org.byteam.delta;

import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

/**
 * This class should use as few other classes as possible before the class loader is patched
 * because any class loaded before it cannot be incrementally deployed.
 */
public class BootstrapApplication extends Application {

    private String externalResourcePath;
    private Application realApplication;

    private void createResources(Context context) {
        externalResourcePath = FileManager.getNewApk(context);
    }

    private static void setupClassLoaders(Context context, String codeCacheDir) {
        List<String> dexList = FileManager.getDexList(context);

        if (!dexList.isEmpty()) {
            Log.i(Delta.TAG, "Bootstrapping class loader with dex list " + join('\n', dexList));

            ClassLoader classLoader = BootstrapApplication.class.getClassLoader();
            String nativeLibraryPath;
            try {
                // BaseDexClassLoader#getLdLibraryPath()
                nativeLibraryPath = (String) classLoader.getClass().getMethod("getLdLibraryPath")
                        .invoke(classLoader);
                Log.i(Delta.TAG, "Native library path: " + nativeLibraryPath);
            } catch (Throwable t) {
                Log.e(Delta.TAG, "Failed to determine native library path " + t.getMessage());
                nativeLibraryPath = Paths.getNativeLibraryFolder(context).getPath();
            }

            IncrementalClassLoader.inject(
                    classLoader,
                    nativeLibraryPath,
                    codeCacheDir,
                    dexList);
        }
    }

    public static String join(char on, List<String> list) {
        StringBuilder stringBuilder = new StringBuilder();
        for (String item : list) {
            stringBuilder.append(item).append(on);
        }
        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        return stringBuilder.toString();
    }

    private void createRealApplication() {
        String applicationClass;
        try {
            Class<?> appInfoClass = Class.forName("org.byteam.delta.AppInfo");
            applicationClass = (String) appInfoClass.getDeclaredField("applicationClass").get(null);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        if (applicationClass != null) {
            try {
                @SuppressWarnings("unchecked")
                Class<? extends Application> realClass =
                        (Class<? extends Application>) Class.forName(applicationClass);
                Log.i(Delta.TAG, "Created delegate app class successfully : " + realClass +
                        " with class loader " + realClass.getClassLoader());
                Constructor<? extends Application> constructor = realClass.getConstructor();
                realApplication = constructor.newInstance();
                Log.i(Delta.TAG, "Created real app instance successfully :" + realApplication);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        } else {
            realApplication = new Application();
        }
    }

    @Override
    protected void attachBaseContext(Context context) {
        createResources(context);
        // Create a new classloader and set it as original classloader's parent.
        setupClassLoaders(context, context.getCacheDir().getPath()); // TODO: 16/9/18 换cache目录

        createRealApplication();
        // This is called from ActivityThread#handleBindApplication() -> LoadedApk#makeApplication().
        // Application#mApplication is changed right after this call, so we cannot do the monkey
        // patching here. So just forward this method to the real Application instance.
        super.attachBaseContext(context);


        if (realApplication != null) {
            try {
                Method attachBaseContext =
                        ContextWrapper.class.getDeclaredMethod("attachBaseContext", Context.class);
                attachBaseContext.setAccessible(true);
                attachBaseContext.invoke(realApplication, context);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    @Override
    public void onCreate() {
        Patcher.patchApplication(
                BootstrapApplication.this, BootstrapApplication.this,
                realApplication, externalResourcePath);
        Patcher.patchExistingResources(BootstrapApplication.this,
                externalResourcePath, null);

        super.onCreate();

        if (realApplication != null) {
            realApplication.onCreate();
        }
    }

}
