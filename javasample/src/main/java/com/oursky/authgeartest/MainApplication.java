package com.oursky.authgeartest;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.oursky.authgear.Authgear;
import com.oursky.authgear.ConfigureOptions;
import com.oursky.authgear.OnConfigureListener;

public class MainApplication extends Application {
    private static final String TAG = MainApplication.class.getSimpleName();
    private Authgear mAuthgear;
    private MutableLiveData<Boolean> mIsConfigured = new MutableLiveData<>(false);
    @Override
    public void onCreate() {
        super.onCreate();
        mAuthgear = new Authgear(this);
        configureAuthgear();
    }

    public Authgear getAuthgear() {
        return mAuthgear;
    }

    public LiveData<Boolean> isConfigured() {
        return mIsConfigured;
    }

    public void configureAuthgear() {
        mAuthgear.configure(new ConfigureOptions(BuildConfig.LOCAL_AUTHGEAR_CLIENT_ID, BuildConfig.LOCAL_AUTHGEAR_ENDPOINT), new OnConfigureListener() {
            @Override
            public void onConfigured() {
                mIsConfigured.setValue(true);
            }

            @Override
            public void onConfigurationFailed(@NonNull Throwable throwable) {
                Log.d(TAG, throwable.toString());
                mIsConfigured.setValue(false);
            }
        });
    }
}
