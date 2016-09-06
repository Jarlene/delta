package org.byteam.delta.sample;

import android.content.Context;
import android.support.multidex.MultiDexApplication;

import org.byteam.delta.Delta;

/**
 * @Author: chenenyu
 * @Created: 16/8/23 16:36.
 */
public class App extends MultiDexApplication {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        Delta.install(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }
}
