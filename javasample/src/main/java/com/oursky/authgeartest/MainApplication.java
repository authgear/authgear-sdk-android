package com.oursky.authgeartest;

import android.app.Application;

import com.oursky.authgear.Authgear;
import com.oursky.authgear.ConfigureOptions;

public class MainApplication extends Application {
    private Authgear mAuthgear;
    @Override
    public void onCreate() {
        super.onCreate();
        mAuthgear = new Authgear();
        mAuthgear.configure(new ConfigureOptions(BuildConfig.LOCAL_AUTHGEAR_CLIENT_ID, BuildConfig.LOCAL_AUTHGEAR_ENDPOINT));
    }

    public Authgear getAuthgear() {
        return mAuthgear;
    }
}
