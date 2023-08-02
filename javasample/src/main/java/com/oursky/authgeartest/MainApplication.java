package com.oursky.authgeartest;

import android.app.Application;

public class MainApplication extends Application {
    private static final String TAG = MainApplication.class.getSimpleName();
    public static final String AUTHGEAR_REDIRECT_URI = "com.authgear.exampleapp.android://host/path";
    public static final String AUTHGEAR_APP2APP_REDIRECT_URI = "https://authgear-demo-rn.pandawork.com/app2app/redirect";
    public static final String AUTHGEAR_WECHAT_REDIRECT_URI = "com.authgear.exampleapp.android://host/open_wechat_app";
    public static final String AUTHGEAR_WECHAT_APP_ID = "wxa2f631873c63add1";
}
