package com.oursky.authgeartest;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricManager;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.oursky.authgear.Authgear;
import com.oursky.authgear.AuthgearDelegate;
import com.oursky.authgear.AuthorizeOptions;
import com.oursky.authgear.AuthorizeResult;
import com.oursky.authgear.BiometricOptions;
import com.oursky.authgear.BiometricPrivateKeyNotFoundException;
import com.oursky.authgear.CancelException;
import com.oursky.authgear.ConfigureOptions;
import com.oursky.authgear.OnAuthenticateAnonymouslyListener;
import com.oursky.authgear.OnAuthenticateBiometricListener;
import com.oursky.authgear.OnAuthorizeListener;
import com.oursky.authgear.OnConfigureListener;
import com.oursky.authgear.OnEnableBiometricListener;
import com.oursky.authgear.OnFetchUserInfoListener;
import com.oursky.authgear.OnLogoutListener;
import com.oursky.authgear.OnPromoteAnonymousUserListener;
import com.oursky.authgear.OnWechatAuthCallbackListener;
import com.oursky.authgear.Page;
import com.oursky.authgear.PromoteOptions;
import com.oursky.authgear.SessionState;
import com.oursky.authgear.SessionStateChangeReason;
import com.oursky.authgear.SettingOptions;
import com.oursky.authgear.UserInfo;
import com.oursky.authgeartest.wxapi.WXEntryActivity;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

@SuppressWarnings("ConstantConditions")
public class MainViewModel extends AndroidViewModel {
    private static final int ALLOWED = BiometricManager.Authenticators.BIOMETRIC_STRONG;
    private static final String TAG = MainViewModel.class.getSimpleName();
    private Authgear mAuthgear = null;
    private IWXAPI wechatAPI;
    final private MutableLiveData<Boolean> mIsConfigured = new MutableLiveData<>(false);
    final private MutableLiveData<String> mClientID = new MutableLiveData<>("");
    final private MutableLiveData<String> mEndpoint = new MutableLiveData<>("");
    final private MutableLiveData<String> mPage = new MutableLiveData<>("");
    final private MutableLiveData<Boolean> mTransientSession = new MutableLiveData<>(false);
    final private MutableLiveData<Boolean> mIsLoading = new MutableLiveData<>(false);
    final private MutableLiveData<Boolean> mBiometricEnable = new MutableLiveData<>(false);
    final private MutableLiveData<UserInfo> mUserInfo = new MutableLiveData<>(null);
    final private MutableLiveData<SessionState> mSessionState = new MutableLiveData<>(SessionState.UNKNOWN);
    final private MutableLiveData<Throwable> mError = new MutableLiveData<>(null);

    public MainViewModel(Application application) {
        super(application);
        MainApplication app = getApplication();
        SharedPreferences preferences = app.getSharedPreferences("authgear.demo", Context.MODE_PRIVATE);
        if (preferences != null) {
            String storedClientID = preferences.getString("clientID", "");
            String storedEndpoint = preferences.getString("endpoint", "");
            String storedPage = preferences.getString("page", "");
            Boolean storedTransientSession = preferences.getBoolean("transientSession", false);
            mClientID.setValue(storedClientID);
            mEndpoint.setValue(storedEndpoint);
            mPage.setValue(storedPage);
            mTransientSession.setValue(storedTransientSession);
        }
    }

    private void updateBiometricState() {
        boolean supported = false;
        try {
            if (Build.VERSION.SDK_INT >= 23) {
                mAuthgear.checkBiometricSupported(
                        this.getApplication(),
                        ALLOWED
                );
                supported = true;
            }
        } catch (Exception e) {}
        boolean enabled = false;
        if (supported) {
            try {
                enabled = mAuthgear.isBiometricEnabled();
            } catch (Exception e) {}
        }
        mBiometricEnable.setValue(enabled);
    }

    private void resetState() {
        mIsConfigured.setValue(true);
        mUserInfo.setValue(null);
        mBiometricEnable.setValue(false);
        mSessionState.setValue(SessionState.UNKNOWN);
        mError.setValue(null);
    }

    private void setError(Throwable e) {
        if (e instanceof CancelException) {
            mError.setValue(null);
        } else {
            mError.setValue(e);
        }
    }

    public LiveData<String> clientID() {
        return mClientID;
    }

    public LiveData<String> endpoint() {
        return mEndpoint;
    }

    public LiveData<String> page() { return mPage; }

    public LiveData<Boolean> transientSession() { return mTransientSession; }

    public LiveData<Boolean> isConfigured() {
        return mIsConfigured;
    }

    public LiveData<Boolean> isLoading() {
        return mIsLoading;
    }

    public LiveData<Boolean> isBiometricEnabled() { return mBiometricEnable; }

    public LiveData<UserInfo> userInfo() {
        return mUserInfo;
    }

    public LiveData<SessionState> sessionState() { return mSessionState; }

    public LiveData<Throwable> error() {
        return mError;
    }

    public void configure(String clientID, String endpoint, Boolean transientSession) {
        if (mIsLoading.getValue()) return;
        mIsLoading.setValue(true);
        MainApplication app = getApplication();
        mClientID.setValue(clientID);
        mEndpoint.setValue(endpoint);
        mTransientSession.setValue(transientSession);
        app.getSharedPreferences("authgear.demo", Context.MODE_PRIVATE)
                .edit()
                .putString("clientID", clientID)
                .putString("endpoint", endpoint)
                .putBoolean("transientSession", transientSession)
                .apply();
        ConfigureOptions configureOptions = new ConfigureOptions();
        configureOptions.setTransientSession(transientSession);
        mAuthgear = new Authgear(getApplication(), clientID, endpoint, null);
        mAuthgear.configure(configureOptions, new OnConfigureListener() {
            @Override
            public void onConfigured() {
                mIsLoading.setValue(false);
                updateBiometricState();
            }

            @Override
            public void onConfigurationFailed(@NonNull Throwable throwable) {
                Log.d(TAG, throwable.toString());
                mIsLoading.setValue(false);
                setError(throwable);
            }
        });

        mAuthgear.setDelegate(new AuthgearDelegate() {
            @Override
            public void onSessionStateChanged(Authgear container, SessionStateChangeReason reason) {
                Log.d(TAG, "Session state=" + container.getSessionState() + " reason=" + reason);
                mSessionState.setValue(container.getSessionState());
            }

            @Override
            public void sendWechatAuthRequest(String state) {
                Log.d(TAG, "Open wechat sdk state=" + state);
                if (!wechatAPI.isWXAppInstalled()) {
                    setError(new RuntimeException("You have not installed the WeChat client app"));
                    return;
                }
                if (wechatAPI == null) {
                    setError(new RuntimeException("WeChat app id is not configured"));
                    return;
                }
                SendAuth.Req req = new SendAuth.Req();
                req.scope = "snsapi_userinfo";
                req.state = state;
                wechatAPI.sendReq(req);
            }
        });

        resetState();

        wechatAPI = WXAPIFactory.createWXAPI(app, MainApplication.AUTHGEAR_WECHAT_APP_ID, true);
        wechatAPI.registerApp(MainApplication.AUTHGEAR_WECHAT_APP_ID);

        WXEntryActivity.setOnWeChatSendAuthResultListener((code, state) -> {
            Log.d(TAG, "Sending WeChat Callback");
            mAuthgear.wechatAuthCallback(code, state, new OnWechatAuthCallbackListener() {
                @Override
                public void onWechatAuthCallback() {
                    Log.d(TAG, "onWeChatAuthCallback");
                }

                @Override
                public void onWechatAuthCallbackFailed(Throwable throwable) {
                    Log.e(TAG, "onWeChatAuthCallbackFailed", throwable);
                }
            });
        });
    }

    public void authorize(String page) {
        mPage.setValue(page);
        MainApplication app = getApplication();
        app.getSharedPreferences("authgear.demo", Context.MODE_PRIVATE)
                .edit()
                .putString("page", page)
                .apply();
        mIsLoading.setValue(true);
        AuthorizeOptions options = new AuthorizeOptions(MainApplication.AUTHGEAR_REDIRECT_URI);
        options.setPrompt("login");
        options.setPage(page);
        options.setWechatRedirectURI(MainApplication.AUTHGEAR_WECHAT_REDIRECT_URI);
        mAuthgear.authorize(options, new OnAuthorizeListener() {
            @Override
            public void onAuthorized(@Nullable AuthorizeResult result) {
                String state = result.getState();
                Log.d(TAG, state == null ? "No state" : state);
                mUserInfo.setValue(result.getUserInfo());
                mIsLoading.setValue(false);
                updateBiometricState();
            }

            @Override
            public void onAuthorizationFailed(@NonNull Throwable throwable) {
                Log.d(TAG, throwable.toString());
                mIsLoading.setValue(false);
                setError(throwable);
            }
        });
    }

    public void authenticateAnonymously() {
        mIsLoading.setValue(true);
        mAuthgear.authenticateAnonymously(new OnAuthenticateAnonymouslyListener() {
            @Override
            public void onAuthenticated(@NonNull UserInfo userInfo) {
                mUserInfo.setValue(userInfo);
                mIsLoading.setValue(false);
                updateBiometricState();
            }

            @Override
            public void onAuthenticationFailed(@NonNull Throwable throwable) {
                Log.d(TAG, throwable.toString());
                mIsLoading.setValue(false);
                setError(throwable);
            }
        });
    }

    private BiometricOptions makeBiometricOptions(FragmentActivity activity) {
        return new BiometricOptions(
                activity,
                "Biometric authentication",
                "Biometric authentication",
                "Use biometric to authenticate",
                "Cancel",
                ALLOWED,
                true
        );
    }

    public void enableBiometric(FragmentActivity activity) {
        mIsLoading.setValue(true);

        try {
            mAuthgear.checkBiometricSupported(activity, ALLOWED);
        } catch (Exception e) {
            mIsLoading.setValue(false);
            setError(e);
            return;
        }

        mAuthgear.enableBiometric(
                makeBiometricOptions(activity),
                new OnEnableBiometricListener() {
                    @Override
                    public void onEnabled() {
                        mIsLoading.setValue(false);
                        updateBiometricState();
                    }

                    @Override
                    public void onFailed(Throwable throwable) {
                        mIsLoading.setValue(false);
                        setError(throwable);
                    }
                }
        );
    }

    public void disableBiometric() {
        try {
            mAuthgear.disableBiometric();
            updateBiometricState();
        } catch (Exception e) {
            setError(e);
        }
    }

    public void authenticateBiometric(FragmentActivity activity) {
        mIsLoading.setValue(true);
        mAuthgear.authenticateBiometric(
                makeBiometricOptions(activity),
                new OnAuthenticateBiometricListener() {
                    @Override
                    public void onAuthenticated(UserInfo userInfo) {
                        mIsLoading.setValue(false);
                        mUserInfo.setValue(userInfo);
                        updateBiometricState();
                    }

                    @Override
                    public void onAuthenticationFailed(Throwable throwable) {
                        Log.d(TAG, throwable.toString());
                        mIsLoading.setValue(false);
                        setError(throwable);
                        updateBiometricState();
                    }
                }
        );
    }

    public void logout() {
        mIsLoading.setValue(true);
        mAuthgear.logout(new OnLogoutListener() {
            @Override
            public void onLogout() {
                mIsLoading.setValue(false);
                mUserInfo.setValue(null);
                updateBiometricState();
            }

            @Override
            public void onLogoutFailed(@NonNull Throwable throwable) {
                Log.d(TAG, throwable.toString());
                mIsLoading.setValue(false);
                setError(throwable);
            }
        });
    }

    public void openSettings() {
        SettingOptions options = new SettingOptions();
        options.setWechatRedirectURI(MainApplication.AUTHGEAR_WECHAT_REDIRECT_URI);
        mAuthgear.open(Page.Settings, options);
    }

    public void promoteAnonymousUser() {
        mIsLoading.setValue(true);
        PromoteOptions options = new PromoteOptions(MainApplication.AUTHGEAR_REDIRECT_URI);
        options.setWechatRedirectURI(MainApplication.AUTHGEAR_WECHAT_REDIRECT_URI);
        mAuthgear.promoteAnonymousUser(options, new OnPromoteAnonymousUserListener() {
            @Override
            public void onPromoted(@NonNull AuthorizeResult result) {
                mUserInfo.setValue(result.getUserInfo());
                mIsLoading.setValue(false);
            }

            @Override
            public void onPromotionFailed(@NonNull Throwable throwable) {
                Log.d(TAG, throwable.toString());
                mIsLoading.setValue(false);
                setError(throwable);
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
                setError(throwable);
            }
        });
    }
}
