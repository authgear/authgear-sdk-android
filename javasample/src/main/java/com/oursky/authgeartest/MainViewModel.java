package com.oursky.authgeartest;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.oursky.authgear.Authgear;
import com.oursky.authgear.AuthorizeOptions;
import com.oursky.authgear.OnAuthorizeListener;
import com.oursky.authgear.SessionState;

public class MainViewModel extends AndroidViewModel {
    private static final String TAG = MainViewModel.class.getSimpleName();
    private Authgear mAuthgear;
    private MutableLiveData<Boolean> mIsLoggedIn;
    // TODO: Is configured can be false since configuration can fail, need to manually retry.
    // If is configured is false currently the session state returned would be wrong - the refresh
    // token is there but any error during access token refresh would pause the whole process.
    //
    // SDK can change to set session state to logged in once stored token is found, but this might surprise
    // user when configure failed (e.g. due to network error).
    private Observer<Boolean> mIsConfiguredObserver = it -> updateSessionState();

    public MainViewModel(Application application) {
        super(application);
        MainApplication app = getApplication();
        mAuthgear = app.getAuthgear();
        mIsLoggedIn = new MutableLiveData<>(mAuthgear.getSessionState() == SessionState.LoggedIn);
        mAuthgear.setOnRefreshTokenExpiredListener(() -> Log.d(TAG, "Token expired"));
        app.isConfigured().observeForever(mIsConfiguredObserver);
    }

    @Override
    public void onCleared() {
        super.onCleared();
        MainApplication application = getApplication();
        application.isConfigured().removeObserver(mIsConfiguredObserver);
    }

    private void updateSessionState() {
        mIsLoggedIn.setValue(mAuthgear.getSessionState() == SessionState.LoggedIn);
    }

    public LiveData<Boolean> isLoggedIn() {
        return mIsLoggedIn;
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
                updateSessionState();
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
