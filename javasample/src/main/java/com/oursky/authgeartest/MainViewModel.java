package com.oursky.authgeartest;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;

import com.oursky.authgear.Authgear;
import com.oursky.authgear.AuthorizeOptions;
import com.oursky.authgear.OnAuthorizeListener;

public class MainViewModel extends AndroidViewModel {
    private static final String TAG = MainViewModel.class.getSimpleName();
    private Authgear mAuthgear;

    public MainViewModel(Application application) {
        super(application);
        MainApplication app = getApplication();
        mAuthgear = app.getAuthgear();
        mAuthgear.setOnRefreshTokenExpiredListener(() -> Log.d(TAG, "Token expired"));
    }

    public void authenticateAnonymously() {
        mAuthgear.authenticateAnonymously();
    }

    public void authorize() {
        mAuthgear.authorize(new AuthorizeOptions(
                "com.myapp://host/path",
                null,
                null,
                null,
                null
        ), new OnAuthorizeListener() {
            @Override
            public void onAuthorized(@Nullable String state) {
                Log.d(TAG, state == null ? "No state" : state);
            }

            @Override
            public void onAuthorizationFailed(@NonNull Throwable throwable) {
                Log.d(TAG, throwable.toString());
            }
        });
    }

    public void logout() {
        mAuthgear.logout();
    }

    public void handleDeepLink() {
        mAuthgear.handleDeepLink();
    }

    public void promoteAnonymousUser() {
        mAuthgear.promoteAnonymousUser();
    }
}
