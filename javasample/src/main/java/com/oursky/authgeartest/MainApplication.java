package com.oursky.authgeartest;

import android.app.Application;

public class MainApplication extends Application {
    private static final String TAG = MainApplication.class.getSimpleName();
    // Each product flavor (see app1/app2 in javasample/build.gradle.kts) sets its own
    // AUTHGEAR_REDIRECT_URI_SCHEME/AUTHGEAR_DEMO_HOST BuildConfig fields, so the same
    // source can build either app and pick up that app's own custom URL scheme and
    // App Links host at runtime.
    public static final String AUTHGEAR_REDIRECT_URI = BuildConfig.AUTHGEAR_REDIRECT_URI_SCHEME + "://host/path";
    public static final String AUTHGEAR_APP2APP_REDIRECT_URI = "https://" + BuildConfig.AUTHGEAR_DEMO_HOST + "/app2app/redirect";
    public static final String AUTHGEAR_WECHAT_REDIRECT_URI = BuildConfig.AUTHGEAR_REDIRECT_URI_SCHEME + "://host/open_wechat_app";
    public static final String AUTHGEAR_WECHAT_APP_ID = "wxa2f631873c63add1";
}
