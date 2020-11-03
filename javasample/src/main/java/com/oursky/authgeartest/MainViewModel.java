package com.oursky.authgeartest;

import android.app.Activity;
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
import com.oursky.authgear.AuthorizeResult;
import com.oursky.authgear.OnAuthenticateAnonymouslyListener;
import com.oursky.authgear.OnAuthorizeListener;
import com.oursky.authgear.OnFetchUserInfoListener;
import com.oursky.authgear.OnLogoutListener;
import com.oursky.authgear.OnPromoteAnonymousUserListener;
import com.oursky.authgear.Page;
import com.oursky.authgear.PromoteOptions;
import com.oursky.authgear.SessionState;
import com.oursky.authgear.UserInfo;

@SuppressWarnings("ConstantConditions")
public class MainViewModel extends AndroidViewModel {
    private static final String TAG = MainViewModel.class.getSimpleName();
    private Authgear mAuthgear;
    private MutableLiveData<Boolean> mIsLoggedIn;
    private MutableLiveData<Boolean> mIsLoading = new MutableLiveData<>(false);
    private MutableLiveData<UserInfo> mUserInfo = new MutableLiveData<>(null);
    private MutableLiveData<String> mAccessToken = new MutableLiveData<>(null);
    private MutableLiveData<Throwable> mError = new MutableLiveData<>(null);
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
        mAuthgear.addOnSessionStateChangedListener((authgear, reason) -> {
            Log.d(TAG, "Session state=" + authgear.getSessionState() + " reason=" + reason);
            updateSessionState();
        });
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
        mAccessToken.setValue(mAuthgear.getAccessToken());
    }

    public LiveData<Boolean> isLoggedIn() {
        return mIsLoggedIn;
    }

    public LiveData<Boolean> isLoading() {
        return mIsLoading;
    }

    public LiveData<UserInfo> userInfo() {
        return mUserInfo;
    }

    public LiveData<String> accessToken() {
        return mAccessToken;
    }

    public LiveData<Throwable> error() {
        return mError;
    }

    public void authenticateAnonymously() {
        if (mIsLoading.getValue()) return;
        mIsLoading.setValue(true);
        mAuthgear.authenticateAnonymously(new OnAuthenticateAnonymouslyListener() {
            @Override
            public void onAuthenticated(@NonNull UserInfo userInfo) {
                mIsLoading.setValue(false);
            }

            @Override
            public void onAuthenticationFailed(@NonNull Throwable throwable) {
                Log.d(TAG, throwable.toString());
                mIsLoading.setValue(false);
                mError.setValue(throwable);
            }
        });
    }

    public void authorize() {
        if (mIsLoading.getValue()) return;
        mIsLoading.setValue(true);
        mAuthgear.authorize(new AuthorizeOptions("com.myapp://host/path"), new OnAuthorizeListener() {
            @Override
            public void onAuthorized(@Nullable AuthorizeResult result) {
                String state = result.getState();
                Log.d(TAG, state == null ? "No state" : state);
                mIsLoading.setValue(false);
            }

            @Override
            public void onAuthorizationFailed(@NonNull Throwable throwable) {
                Log.d(TAG, throwable.toString());
                mIsLoading.setValue(false);
                mError.setValue(throwable);
            }
        });
    }

    public void logout() {
        if (mIsLoading.getValue()) return;
        mIsLoading.setValue(true);
        mAuthgear.logout(new OnLogoutListener() {
            @Override
            public void onLogout() {
                mIsLoading.setValue(false);
                mUserInfo.setValue(null);
            }

            @Override
            public void onLogoutFailed(@NonNull Throwable throwable) {
                Log.d(TAG, throwable.toString());
                mIsLoading.setValue(false);
                mError.setValue(throwable);
            }
        });
    }

    public void openSettings(Activity activity) {
        mAuthgear.open(activity, Page.Settings);
    }

    public void promoteAnonymousUser() {
        if (mIsLoading.getValue()) return;
        mIsLoading.setValue(true);
        mAuthgear.promoteAnonymousUser(new PromoteOptions("com.myapp://host/path", null, null), new OnPromoteAnonymousUserListener() {
            @Override
            public void onPromoted(@NonNull AuthorizeResult result) {
                mUserInfo.setValue(result.getUserInfo());
                mIsLoading.setValue(false);
            }

            @Override
            public void onPromotionFailed(@NonNull Throwable throwable) {
                Log.d(TAG, throwable.toString());
                mIsLoading.setValue(false);
                mError.setValue(throwable);
            }
        });
    }

    public void fetchUserInfo() {
        mAuthgear.fetchUserInfo(new OnFetchUserInfoListener() {
            @Override
            public void onFetchedUserInfo(@NonNull UserInfo userInfo) {
                mUserInfo.setValue(userInfo);
            }

            @Override
            public void onFetchingUserInfoFailed(@NonNull Throwable throwable) {
                mUserInfo.setValue(null);
                mError.setValue(throwable);
            }
        });
    }
}
