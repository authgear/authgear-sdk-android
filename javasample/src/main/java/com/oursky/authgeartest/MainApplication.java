package com.oursky.authgeartest;

import android.app.Application;

public class MainApplication extends Application {
    private static final String TAG = MainApplication.class.getSimpleName();
    public static final String AUTHGEAR_REDIRECT_URI = "com.authgear.example://host/path";
    public static final String AUTHGEAR_WECHAT_REDIRECT_URI = "com.authgear.example://host/open_wechat_app";
    public static final String AUTHGEAR_WECHAT_APP_ID = "wxa2f631873c63add1";
}
