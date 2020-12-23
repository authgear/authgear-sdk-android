package com.oursky.authgeartest;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.oursky.authgear.Authgear;
import com.oursky.authgear.AuthorizeOptions;
import com.oursky.authgear.AuthorizeResult;
import com.oursky.authgear.OnAuthenticateAnonymouslyListener;
import com.oursky.authgear.OnAuthorizeListener;
import com.oursky.authgear.OnConfigureListener;
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
    private Authgear mAuthgear = null;
    final private MutableLiveData<Boolean> mIsConfigured = new MutableLiveData<>(false);
    final private MutableLiveData<String> mClientID = new MutableLiveData<>("");
    final private MutableLiveData<String> mEndpoint = new MutableLiveData<>("");
    final private MutableLiveData<Boolean> mIsThirdParty = new MutableLiveData<>(true);
    final private MutableLiveData<Boolean> mIsLoggedIn = new MutableLiveData<>(false);
    final private MutableLiveData<Boolean> mIsLoading = new MutableLiveData<>(false);
    final private MutableLiveData<UserInfo> mUserInfo = new MutableLiveData<>(null);
    final private MutableLiveData<String> mSuccessDialogMessage = new MutableLiveData<>(null);
    final private MutableLiveData<Throwable> mError = new MutableLiveData<>(null);

    public MainViewModel(Application application) {
        super(application);
        MainApplication app = getApplication();
        SharedPreferences preferences = app.getSharedPreferences("authgear.demo", Context.MODE_PRIVATE);
        if (preferences != null) {
            String storedClientID = preferences.getString("clientID", "");
            String storedEndpoint = preferences.getString("endpoint", "");
            Boolean storedIsThirdParty = preferences.getBoolean("isThirdParty", true);
            mClientID.setValue(storedClientID);
            mEndpoint.setValue(storedEndpoint);
            mIsThirdParty.setValue(storedIsThirdParty);
        }
    }

    private void updateSessionState() {
        if (mAuthgear == null) return;
        mIsLoggedIn.setValue(mAuthgear.getSessionState() == SessionState.AUTHENTICATED);
    }

    // clear screen state when user configure Authgear for more than once
    private void initializeScreenState() {
        mIsLoggedIn.setValue(false);
        mUserInfo.setValue(null);
        mSuccessDialogMessage.setValue(null);
        mError.setValue(null);
    }

    public LiveData<String> clientID() {
        return mClientID;
    }

    public LiveData<String> endpoint() {
        return mEndpoint;
    }

    public LiveData<Boolean> isThirdParty() {
        return mIsThirdParty;
    }

    public LiveData<Boolean> isConfigured() {
        return mIsConfigured;
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

    public LiveData<String> successDialogMessage() {
        return mSuccessDialogMessage;
    }

    public LiveData<Throwable> error() {
        return mError;
    }

    public void configure(String clientID, String endpoint, Boolean isThirdParty) {
        if (mIsLoading.getValue()) return;
        mIsLoading.setValue(true);
        MainApplication app = getApplication();
        mClientID.setValue(clientID);
        mEndpoint.setValue(endpoint);
        app.getSharedPreferences("authgear.demo", Context.MODE_PRIVATE)
                .edit()
                .putString("clientID", clientID)
                .putString("endpoint", endpoint)
                .putBoolean("isThirdParty", isThirdParty)
                .apply();
        mAuthgear = new Authgear(getApplication(), clientID, endpoint, null, isThirdParty);
        mAuthgear.configure(false, new OnConfigureListener() {
            @Override
            public void onConfigured() {
                mSuccessDialogMessage.setValue("Configured Authgear successfully");
                mIsLoading.setValue(false);
            }

            @Override
            public void onConfigurationFailed(@NonNull Throwable throwable) {
                Log.d(TAG, throwable.toString());
                mIsLoading.setValue(false);
                mError.setValue(throwable);
            }
        });

        mAuthgear.setDelegate((authgear, reason) -> {
            Log.d(TAG, "Session state=" + authgear.getSessionState() + " reason=" + reason);
            updateSessionState();
        });

        initializeScreenState();
        mIsConfigured.setValue(true);
    }

    public void authorize() {
        if (mAuthgear == null || mIsLoading.getValue()) return;
        mIsLoading.setValue(true);
        mAuthgear.authorize(new AuthorizeOptions(MainApplication.AUTHGEAR_REDIRECT_URI), new OnAuthorizeListener() {
            @Override
            public void onAuthorized(@Nullable AuthorizeResult result) {
                String state = result.getState();
                Log.d(TAG, state == null ? "No state" : state);
                mSuccessDialogMessage.setValue("Logged in successfully");
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

    public void authenticateAnonymously() {
        if (mAuthgear == null || mIsLoading.getValue()) return;
        mIsLoading.setValue(true);
        mAuthgear.authenticateAnonymously(new OnAuthenticateAnonymouslyListener() {
            @Override
            public void onAuthenticated(@NonNull UserInfo userInfo) {
                mUserInfo.setValue(userInfo);
                mSuccessDialogMessage.setValue("Logged in anonymously");
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

    public void logout() {
        if (mAuthgear == null || mIsLoading.getValue()) return;
        mIsLoading.setValue(true);
        mAuthgear.logout(new OnLogoutListener() {
            @Override
            public void onLogout() {
                mIsLoading.setValue(false);
                mUserInfo.setValue(null);
                mSuccessDialogMessage.setValue("Logged out successfully");
            }

            @Override
            public void onLogoutFailed(@NonNull Throwable throwable) {
                Log.d(TAG, throwable.toString());
                mIsLoading.setValue(false);
                mError.setValue(throwable);
            }
        });
    }

    public void openSettings() {
        if (mAuthgear == null) return;
        mAuthgear.open(Page.Settings);
    }

    public void promoteAnonymousUser() {
        if (mAuthgear == null || mIsLoading.getValue()) return;
        mIsLoading.setValue(true);
        mAuthgear.promoteAnonymousUser(new PromoteOptions(MainApplication.AUTHGEAR_REDIRECT_URI, null, null), new OnPromoteAnonymousUserListener() {
            @Override
            public void onPromoted(@NonNull AuthorizeResult result) {
                mUserInfo.setValue(result.getUserInfo());
                mSuccessDialogMessage.setValue("Successfully promoted anonymous user");
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
        if (mAuthgear == null) return;
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
