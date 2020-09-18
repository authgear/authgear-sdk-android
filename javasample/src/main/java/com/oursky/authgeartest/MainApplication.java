package com.oursky.authgeartest;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;

import com.oursky.authgear.Authgear;
import com.oursky.authgear.ConfigureOptions;
import com.oursky.authgear.OnConfigureListener;

public class MainApplication extends Application {
    private static final String TAG = MainApplication.class.getSimpleName();
    private Authgear mAuthgear;
    @Override
    public void onCreate() {
        super.onCreate();
        mAuthgear = new Authgear(this);
        mAuthgear.configure(new ConfigureOptions(BuildConfig.LOCAL_AUTHGEAR_CLIENT_ID, BuildConfig.LOCAL_AUTHGEAR_ENDPOINT), new OnConfigureListener() {
            @Override
            public void onConfigured() {
            }

            @Override
            public void onConfigurationFailed(@NonNull Throwable throwable) {
                Log.d(TAG, throwable.toString());
            }
        });
    }

    public Authgear getAuthgear() {
        return mAuthgear;
    }
}
