package org.byteam.delta.sample;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

/**
 * @Author: chenenyu
 * @Created: 16/8/23 16:36.
 */
public class App extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(base);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }
}
