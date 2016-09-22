package org.byteam.delta.sample;

import android.content.Context;
import android.support.multidex.MultiDexApplication;

/**
 * @Author: chenenyu
 * @Created: 16/8/23 16:36.
 */
public class App extends MultiDexApplication {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }
}
