package org.byteam.sample;

import android.content.Context;
import android.support.multidex.MultiDexApplication;

import org.byteam.tp.Tp;

/**
 * @Author: chenenyu
 * @Created: 16/8/23 16:36.
 */
public class App extends MultiDexApplication {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        Tp.install(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }
}
