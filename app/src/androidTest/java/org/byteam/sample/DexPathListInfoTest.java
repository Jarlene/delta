package org.byteam.sample;

import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Method;

/**
 * @Author: chenenyu
 * @Created: 16/9/5 14:31.
 */
@RunWith(AndroidJUnit4.class)
public class DexPathListInfoTest {
    static final String TAG = "DexPathListInfoTest";

    @Test
    public void showAllMethods() {
        Class<?> dexPathListClass = null;
        try {
            dexPathListClass = Class.forName("dalvik.system.DexPathList");
        } catch (ClassNotFoundException e) {
            Log.d(TAG, "Class 'dalvik.system.DexPathList' not found.", e);
        }
        Method[] methods = dexPathListClass.getDeclaredMethods();
        Log.d(TAG, "所有的方法:");
        for (Method method : methods) {
            Log.d(TAG, method.toGenericString());
        }
    }

}
