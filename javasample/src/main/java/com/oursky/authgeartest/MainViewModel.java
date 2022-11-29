package com.oursky.authgeartest;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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
import com.oursky.authgear.CancelException;
import com.oursky.authgear.ColorScheme;
import com.oursky.authgear.OnAuthenticateAnonymouslyListener;
import com.oursky.authgear.OnAuthenticateBiometricListener;
import com.oursky.authgear.OnAuthorizeListener;
import com.oursky.authgear.OnConfigureListener;
import com.oursky.authgear.OnEnableBiometricListener;
import com.oursky.authgear.OnFetchUserInfoListener;
import com.oursky.authgear.OnLogoutListener;
import com.oursky.authgear.OnOpenURLListener;
import com.oursky.authgear.OnPromoteAnonymousUserListener;
import com.oursky.authgear.OnReauthenticateListener;
import com.oursky.authgear.OnRefreshIDTokenListener;
import com.oursky.authgear.OnWechatAuthCallbackListener;
import com.oursky.authgear.Page;
import com.oursky.authgear.PersistentTokenStorage;
import com.oursky.authgear.PromoteOptions;
import com.oursky.authgear.ReauthentcateOptions;
import com.oursky.authgear.ReauthenticateResult;
import com.oursky.authgear.SessionState;
import com.oursky.authgear.SessionStateChangeReason;
import com.oursky.authgear.SettingOptions;
import com.oursky.authgear.TransientTokenStorage;
import com.oursky.authgear.UserInfo;
import com.oursky.authgeartest.wxapi.WXEntryActivity;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import java.util.Date;

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
    final private MutableLiveData<String> mTokenStorage = new MutableLiveData<>("");
    final private MutableLiveData<ColorScheme> mColorScheme = new MutableLiveData<>(null);
    final private MutableLiveData<Boolean> mSSOEnabled = new MutableLiveData<>(false);
    final private MutableLiveData<Boolean> mIsLoading = new MutableLiveData<>(false);
    final private MutableLiveData<Boolean> mBiometricEnable = new MutableLiveData<>(false);
    final private MutableLiveData<Boolean> mCanReauthenticate = new MutableLiveData<>(false);
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
            String storedTokenStorage = preferences.getString("tokenStorage", PersistentTokenStorage.class.getSimpleName());
            Boolean storedSSOEnabled = preferences.getBoolean("ssoEnabled", false);
            mClientID.setValue(storedClientID);
            mEndpoint.setValue(storedEndpoint);
            mPage.setValue(storedPage);
            mTokenStorage.setValue(storedTokenStorage);
            mSSOEnabled.setValue(storedSSOEnabled);
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
        mCanReauthenticate.setValue(false);
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

    public void setPage(String page) {
        mPage.setValue(page);
    }

    public void setTokenStorage(String tokenStorage) {
        mTokenStorage.setValue(tokenStorage);
    }

    public void setColorScheme(ColorScheme colorScheme) {
        mColorScheme.setValue(colorScheme);
    }

    public LiveData<String> clientID() {
        return mClientID;
    }

    public LiveData<String> endpoint() {
        return mEndpoint;
    }

    public LiveData<String> tokenStorage() { return mTokenStorage; }

    public LiveData<Boolean> ssoEnabled() { return mSSOEnabled; }

    public LiveData<Boolean> isConfigured() {
        return mIsConfigured;
    }

    public LiveData<Boolean> isLoading() {
        return mIsLoading;
    }

    public LiveData<Boolean> isBiometricEnabled() { return mBiometricEnable; }

    public LiveData<Boolean> canReauthenticate() { return mCanReauthenticate; }

    public LiveData<UserInfo> userInfo() {
        return mUserInfo;
    }

    public LiveData<SessionState> sessionState() { return mSessionState; }

    public LiveData<Throwable> error() {
        return mError;
    }

    public void configure(String clientID, String endpoint, Boolean ssoEnabled) {
        if (mIsLoading.getValue()) return;
        mIsLoading.setValue(true);
        MainApplication app = getApplication();
        mClientID.setValue(clientID);
        mEndpoint.setValue(endpoint);
        mSSOEnabled.setValue(ssoEnabled);
        app.getSharedPreferences("authgear.demo", Context.MODE_PRIVATE)
                .edit()
                .putString("clientID", clientID)
                .putString("endpoint", endpoint)
                .putBoolean("ssoEnabled", ssoEnabled)
                .apply();
        if (mTokenStorage.getValue().equals(TransientTokenStorage.class.getSimpleName())) {
            mAuthgear = new Authgear(
                    getApplication(),
                    clientID,
                    endpoint,
                    new TransientTokenStorage(),
                    ssoEnabled
            );
        } else {
            mAuthgear = new Authgear(
                    getApplication(),
                    clientID,
                    endpoint,
                    new PersistentTokenStorage(getApplication()),
                    ssoEnabled
            );
        }
        mAuthgear.configure(new OnConfigureListener() {
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

    public void authorize() {
        mIsLoading.setValue(true);
        AuthorizeOptions options = new AuthorizeOptions(MainApplication.AUTHGEAR_REDIRECT_URI);
        options.setColorScheme(getColorScheme());
        options.setPage(mPage.getValue());
        options.setWechatRedirectURI(MainApplication.AUTHGEAR_WECHAT_REDIRECT_URI);
        mAuthgear.authorize(options, new OnAuthorizeListener() {
            @Override
            public void onAuthorized(@Nullable AuthorizeResult result) {
                String state = result.getState();
                Log.d(TAG, state == null ? "No state" : state);
                mUserInfo.setValue(result.getUserInfo());
                mCanReauthenticate.setValue(mAuthgear.getCanReauthenticate());
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

    public void reauthenticate(FragmentActivity activity) {
        mIsLoading.setValue(true);

        mAuthgear.refreshIDToken(new OnRefreshIDTokenListener() {
            @Override
            public void onFinished() {
                if (mAuthgear.getCanReauthenticate()) {
                    ReauthentcateOptions options = new ReauthentcateOptions(MainApplication.AUTHGEAR_REDIRECT_URI);
                    options.setWechatRedirectURI(MainApplication.AUTHGEAR_WECHAT_REDIRECT_URI);
                    options.setColorScheme(getColorScheme());
                    mAuthgear.reauthenticate(options, makeBiometricOptions(activity), new OnReauthenticateListener() {
                        @Override
                        public void onFinished(@Nullable ReauthenticateResult result) {
                            String state = result.getState();
                            Log.d(TAG, state == null ? "No state" : state);
                            mUserInfo.setValue(result.getUserInfo());
                            mIsLoading.setValue(false);
                        }

                        @Override
                        public void onFailed(@NonNull Throwable throwable) {
                            Log.d(TAG, throwable.toString());
                            mIsLoading.setValue(false);
                            setError(throwable);
                        }
                    });
                }
            }

            @Override
            public void onFailed(Throwable throwable) {
                Log.d(TAG, throwable.toString());
                mIsLoading.setValue(false);
                setError(throwable);
            }
        });
    }

    public void reauthenticateWebOnly() {
        mIsLoading.setValue(true);

        mAuthgear.refreshIDToken(new OnRefreshIDTokenListener() {
            @Override
            public void onFinished() {
                if (mAuthgear.getCanReauthenticate()) {
                    ReauthentcateOptions options = new ReauthentcateOptions(MainApplication.AUTHGEAR_REDIRECT_URI);
                    options.setWechatRedirectURI(MainApplication.AUTHGEAR_WECHAT_REDIRECT_URI);
                    mAuthgear.reauthenticate(options, null, new OnReauthenticateListener() {
                        @Override
                        public void onFinished(@Nullable ReauthenticateResult result) {
                            String state = result.getState();
                            Log.d(TAG, state == null ? "No state" : state);
                            mUserInfo.setValue(result.getUserInfo());
                            mIsLoading.setValue(false);
                        }

                        @Override
                        public void onFailed(@NonNull Throwable throwable) {
                            Log.d(TAG, throwable.toString());
                            mIsLoading.setValue(false);
                            setError(throwable);
                        }
                    });
                }
            }

            @Override
            public void onFailed(Throwable throwable) {
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
                mCanReauthenticate.setValue(false);
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

    private ColorScheme getColorScheme() {
        ColorScheme explicit = mColorScheme.getValue();
        if (explicit != null) {
            return explicit;
        }
        return getSystemColorScheme();
    }

    private ColorScheme getSystemColorScheme() {
        int currentNightMode = getApplication().getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
            return ColorScheme.Dark;
        }
        return ColorScheme.Light;
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
                        mCanReauthenticate.setValue(mAuthgear.getCanReauthenticate());
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
                mCanReauthenticate.setValue(false);
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
        mIsLoading.setValue(true);
        SettingOptions options = new SettingOptions();
        options.setColorScheme(getColorScheme());
        options.setWechatRedirectURI(MainApplication.AUTHGEAR_WECHAT_REDIRECT_URI);
        mAuthgear.open(Page.Settings, options, new OnOpenURLListener() {
            @Override
            public void onClosed() {
                mIsLoading.setValue(false);
                Log.d(TAG, "openSettings closed");
            }

            @Override
            public void onFailed(Throwable throwable) {
                Log.d(TAG, throwable.toString());
                mIsLoading.setValue(false);
                setError(throwable);
            }
        });
    }

    public void promoteAnonymousUser() {
        mIsLoading.setValue(true);
        PromoteOptions options = new PromoteOptions(MainApplication.AUTHGEAR_REDIRECT_URI);
        options.setWechatRedirectURI(MainApplication.AUTHGEAR_WECHAT_REDIRECT_URI);
        options.setColorScheme(getColorScheme());
        mAuthgear.promoteAnonymousUser(options, new OnPromoteAnonymousUserListener() {
            @Override
            public void onPromoted(@NonNull AuthorizeResult result) {
                mUserInfo.setValue(result.getUserInfo());
                mCanReauthenticate.setValue(mAuthgear.getCanReauthenticate());
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

    public void showAuthTime(FragmentActivity activity) {
        Date nullable = mAuthgear.getAuthTime();
        if (nullable != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle("auth_time");
            builder.setMessage(nullable.toString());
            builder.setPositiveButton("OK", (dialogInterface, i) -> {
            });
            builder.create().show();
        }
    }
}
