package com.oursky.authgeartest;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
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

import com.oursky.authgear.PreAuthenticatedURLOptions;
import com.oursky.authgear.Authgear;
import com.oursky.authgear.AuthgearDelegate;
import com.oursky.authgear.AuthenticateOptions;
import com.oursky.authgear.AuthgearException;
import com.oursky.authgear.BiometricOptions;
import com.oursky.authgear.CancelException;
import com.oursky.authgear.ColorScheme;
import com.oursky.authgear.CustomTabsUIImplementation;
import com.oursky.authgear.OnAuthenticateAnonymouslyListener;
import com.oursky.authgear.OnAuthenticateBiometricListener;
import com.oursky.authgear.OnAuthenticateListener;
import com.oursky.authgear.OnConfigureListener;
import com.oursky.authgear.OnEnableBiometricListener;
import com.oursky.authgear.OnFetchUserInfoListener;
import com.oursky.authgear.OnHandleApp2AppAuthenticationRequestListener;
import com.oursky.authgear.OnLogoutListener;
import com.oursky.authgear.OnMakePreAuthenticatedURLListener;
import com.oursky.authgear.OnOpenAuthorizationURLListener;
import com.oursky.authgear.OnOpenSettingsActionListener;
import com.oursky.authgear.OnOpenURLListener;
import com.oursky.authgear.OnPromoteAnonymousUserListener;
import com.oursky.authgear.OnReauthenticateListener;
import com.oursky.authgear.OnRefreshIDTokenListener;
import com.oursky.authgear.OnWechatAuthCallbackListener;
import com.oursky.authgear.OpenAuthorizationURLOptions;
import com.oursky.authgear.Page;
import com.oursky.authgear.PersistentTokenStorage;
import com.oursky.authgear.PromoteOptions;
import com.oursky.authgear.ReauthenticateOptions;
import com.oursky.authgear.SessionState;
import com.oursky.authgear.SessionStateChangeReason;
import com.oursky.authgear.SettingOptions;
import com.oursky.authgear.TokenStorage;
import com.oursky.authgear.SettingsActionOptions;
import com.oursky.authgear.TransientTokenStorage;
import com.oursky.authgear.UIImplementation;
import com.oursky.authgear.UserInfo;
import com.oursky.authgear.WebKitWebViewUIImplementation;
import com.oursky.authgear.app2app.App2AppAuthenticateOptions;
import com.oursky.authgear.app2app.App2AppAuthenticateRequest;
import com.oursky.authgear.app2app.App2AppOptions;
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
    final private MutableLiveData<String> mApp2AppEndpoint = new MutableLiveData<>("");
    final private MutableLiveData<String> mAuthenticationiFlowGroup = new MutableLiveData<>("");
    final private MutableLiveData<String> mPreAuthenticatedURLClientID = new MutableLiveData<>("");
    final private MutableLiveData<String> mPreAuthenticatedURLRedirectURI = new MutableLiveData<>("");
    final private MutableLiveData<String> mPage = new MutableLiveData<>("");
    final private MutableLiveData<String> mTokenStorage = new MutableLiveData<>("");
    final private MutableLiveData<ColorScheme> mColorScheme = new MutableLiveData<>(null);

    final private MutableLiveData<Boolean> mUseWebKitWebView = new MutableLiveData<>(false);
    final private MutableLiveData<Boolean> mIsSsoEnabled = new MutableLiveData<>(false);
    final private MutableLiveData<Boolean> mIsPreAuthenticatedURLEnabled = new MutableLiveData<>(false);
    final private MutableLiveData<Boolean> mIsLoading = new MutableLiveData<>(false);
    final private MutableLiveData<Boolean> mBiometricEnable = new MutableLiveData<>(false);
    final private MutableLiveData<Boolean> mCanReauthenticate = new MutableLiveData<>(false);
    final private MutableLiveData<UserInfo> mUserInfo = new MutableLiveData<>(null);
    final private MutableLiveData<SessionState> mSessionState = new MutableLiveData<>(SessionState.UNKNOWN);
    final private MutableLiveData<Throwable> mError = new MutableLiveData<>(null);
    private Intent pendingApp2AppIntent = null;
    final private MutableLiveData<Boolean> mAuthgearConfigured = new MutableLiveData<>(false);
    final private MutableLiveData<ConfirmationViewModel> mApp2AppConfirmation = new MutableLiveData<>(null);

    public MainViewModel(Application application) {
        super(application);
        MainApplication app = getApplication();
        SharedPreferences preferences = app.getSharedPreferences("authgear.demo", Context.MODE_PRIVATE);
        if (preferences != null) {
            String storedClientID = preferences.getString("clientID", "");
            String storedEndpoint = preferences.getString("endpoint", "");
            String storedApp2AppEndpoint = preferences.getString("app2appendpoint", "");
            String storedAuthenticationFlowGroup = preferences.getString("authenticationFlowGroup", "");
            String storedPage = preferences.getString("page", "");
            String storedTokenStorage = preferences.getString("tokenStorage", PersistentTokenStorage.class.getSimpleName());
            String storedPreAuthenticatedURLClientID = preferences.getString("preAuthenticatedURLClientID", "");
            String storedPreAuthenticatedURLRedirectURI = preferences.getString("preAuthenticatedURLRedirectURI", "");
            Boolean storedIsSsoEnabled = preferences.getBoolean("isSsoEnabled", false);
            Boolean storedIsPreAuthenticatedURLEnabled= preferences.getBoolean("isPreAuthenticatedURLEnabled", false);
            Boolean storedUseWebKitWebView = preferences.getBoolean("useWebKitWebView", false);
            mClientID.setValue(storedClientID);
            mEndpoint.setValue(storedEndpoint);
            mApp2AppEndpoint.setValue(storedApp2AppEndpoint);
            mAuthenticationiFlowGroup.setValue(storedAuthenticationFlowGroup);
            mPage.setValue(storedPage);
            mTokenStorage.setValue(storedTokenStorage);
            mIsSsoEnabled.setValue(storedIsSsoEnabled);
            mIsPreAuthenticatedURLEnabled.setValue(storedIsPreAuthenticatedURLEnabled);
            mPreAuthenticatedURLClientID.setValue(storedPreAuthenticatedURLClientID);
            mPreAuthenticatedURLRedirectURI.setValue(storedPreAuthenticatedURLRedirectURI);
            mUseWebKitWebView.setValue(storedUseWebKitWebView);
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

    public void setAuthenticationiFlowGroup(String flowGroup) {
        mAuthenticationiFlowGroup.setValue(flowGroup);
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
    public LiveData<String> app2appEndpoint() {
        return mApp2AppEndpoint;
    }

    public LiveData<String> preAuthenticatedURLRedirectURI() {
        return mPreAuthenticatedURLRedirectURI;
    }
    public LiveData<String> preAuthenticatedURLClientID() {
        return mPreAuthenticatedURLClientID;
    }
    public LiveData<Boolean> isPreAuthenticatedURLEnabled() {
        return mIsPreAuthenticatedURLEnabled;
    }


    public LiveData<String> tokenStorage() { return mTokenStorage; }

    public LiveData<Boolean> useWebKitWebView() { return mUseWebKitWebView; }

    public LiveData<Boolean> isSsoEnabled() { return mIsSsoEnabled; }

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
    public LiveData<ConfirmationViewModel> app2appConfirmation() { return mApp2AppConfirmation; }

    public void configure(
            String clientID,
            String endpoint,
            Boolean isSsoEnabled,
            Boolean useWebKitWebView,
            String app2appEndpoint,
            Boolean isPreAuthenticatedURLEnabled,
            String preAuthenticatedURLClientID,
            String preAuthenticatedURLRedirectURI) {
        if (mIsLoading.getValue()) return;
        mIsLoading.setValue(true);
        MainApplication app = getApplication();
        mClientID.setValue(clientID);
        mEndpoint.setValue(endpoint);
        mApp2AppEndpoint.setValue(app2appEndpoint);
        mIsSsoEnabled.setValue(isSsoEnabled);
        mIsPreAuthenticatedURLEnabled.setValue(isPreAuthenticatedURLEnabled);
        mUseWebKitWebView.setValue(useWebKitWebView);
        app.getSharedPreferences("authgear.demo", Context.MODE_PRIVATE)
                .edit()
                .putString("clientID", clientID)
                .putString("endpoint", endpoint)
                .putString("app2appendpoint", app2appEndpoint)
                .putBoolean("isSsoEnabled", isSsoEnabled)
                .putBoolean("isPreAuthenticatedURLEnabled", isPreAuthenticatedURLEnabled)
                .putString("preAuthenticatedURLClientID", preAuthenticatedURLClientID)
                .putString("preAuthenticatedURLRedirectURI", preAuthenticatedURLRedirectURI)
                .putBoolean("useWebKitWebView", useWebKitWebView)
                .apply();
        Boolean isApp2AppEnabled = !app2appEndpoint.isEmpty();
        App2AppOptions app2appOptions = new App2AppOptions(isApp2AppEnabled);

        TokenStorage tokenStorage;
        if (mTokenStorage.getValue().equals(TransientTokenStorage.class.getSimpleName())) {
            tokenStorage = new TransientTokenStorage();
        } else {
            tokenStorage = new PersistentTokenStorage(getApplication());
        }

        UIImplementation uiImplementation;
        if (mUseWebKitWebView.getValue()) {
            uiImplementation = new WebKitWebViewUIImplementation();
        } else {
            uiImplementation = new CustomTabsUIImplementation();
        }

        mAuthgear = new Authgear(
                getApplication(),
                clientID,
                endpoint,
                tokenStorage,
                uiImplementation,
                isSsoEnabled,
                isPreAuthenticatedURLEnabled,
                null,
                app2appOptions
        );
        mAuthgear.configure(new OnConfigureListener() {
            @Override
            public void onConfigured() {
                mIsLoading.setValue(false);
                mAuthgearConfigured.setValue(true);
                updateBiometricState();
                handlePendingApp2AppRequest();
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

    public void authenticate() {
        mIsLoading.setValue(true);
        AuthenticateOptions options = new AuthenticateOptions(MainApplication.AUTHGEAR_REDIRECT_URI);
        options.setColorScheme(getColorScheme());
        options.setPage(mPage.getValue());
        options.setWechatRedirectURI(MainApplication.AUTHGEAR_WECHAT_REDIRECT_URI);
        options.setAuthenticationFlowGroup(mAuthenticationiFlowGroup.getValue());
        mAuthgear.authenticate(options, new OnAuthenticateListener() {
            @Override
            public void onAuthenticated(@Nullable UserInfo userInfo) {
                mUserInfo.setValue(userInfo);
                mCanReauthenticate.setValue(mAuthgear.getCanReauthenticate());
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

    public void appendApp2AppRequest(Intent intent) {
        this.pendingApp2AppIntent = intent;
        if (mAuthgearConfigured.getValue()) {
            handlePendingApp2AppRequest();
        }
    }

    private void handlePendingApp2AppRequest() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {
            throw new RuntimeException("app2app is not supported in current android version");
        }
        if (this.pendingApp2AppIntent == null) {
            return;
        }
        Uri intentUri = this.pendingApp2AppIntent.getData();
        this.pendingApp2AppIntent = null;
        if (intentUri == null) {
            return;
        }
        final App2AppAuthenticateRequest request = mAuthgear.parseApp2AppAuthenticationRequest(intentUri);
        if (request == null) {
            setError(new RuntimeException("Unexpected app2app uri"));
            return;
        }
        if (mAuthgear.getSessionState() != SessionState.AUTHENTICATED) {
            setError(new RuntimeException("must be in authenticated state to handle app2app request"));
            return;
        }
        mAuthgear.fetchUserInfo(new OnFetchUserInfoListener() {
            @Override
            public void onFetchedUserInfo(@NonNull UserInfo userInfo) {
                promptApp2AppConfirmation(userInfo, request);
            }

            @Override
            public void onFetchingUserInfoFailed(@NonNull Throwable throwable) {
                setError(throwable);
            }
        });
    }

    private void promptApp2AppConfirmation(UserInfo userInfo, App2AppAuthenticateRequest request) {
        String message = "Approve app2app request?";
        String userIdentity = "";

        if (userInfo.getPhoneNumber() != null) {
            userIdentity = userIdentity + "\n  phone: " + userInfo.getPhoneNumber();
        }
        if (userInfo.getEmail() != null) {
            userIdentity = userIdentity + "\n  email: " + userInfo.getEmail();
        }
        if (!userIdentity.isEmpty()) {
            message = message + "\ncurrent user:" + userIdentity;
        }
        if (request.getState() != null && !request.getState().isEmpty()) {
            message = message + "\nstate: " + request.getState();
        }
        mApp2AppConfirmation.setValue(new ConfirmationViewModel(message) {
            @Override
            public void onConfirm() {
                mAuthgear.approveApp2AppAuthenticationRequest(request, new OnHandleApp2AppAuthenticationRequestListener() {
                    @Override
                    public void onFinished() {
                        Log.d(TAG, "Handled app2app request successfully");
                    }

                    @Override
                    public void onFailed(@NonNull Throwable throwable) {
                        Log.d(TAG, throwable.toString());
                        setError(throwable);
                    }
                });
            }

            @Override
            public void onCancel() {
                mApp2AppConfirmation.postValue(null);
                mAuthgear.rejectApp2AppAuthenticationRequest(request, new AuthgearException("rejected"),new OnHandleApp2AppAuthenticationRequestListener() {
                    @Override
                    public void onFinished() {
                        Log.d(TAG, "Rejected app2app request successfully");
                    }

                    @Override
                    public void onFailed(@NonNull Throwable throwable) {
                        Log.d(TAG, throwable.toString());
                        setError(throwable);
                    }
                });
            }
        });
    }

    public void authenticateApp2App(String state) {
        mIsLoading.setValue(true);
        App2AppAuthenticateOptions options = new App2AppAuthenticateOptions(
                mApp2AppEndpoint.getValue(),
                MainApplication.AUTHGEAR_APP2APP_REDIRECT_URI,
                state);
        mAuthgear.startApp2AppAuthentication(options, new OnAuthenticateListener() {
            @Override
            public void onAuthenticated(@Nullable UserInfo userInfo) {
                mUserInfo.setValue(userInfo);
                mCanReauthenticate.setValue(mAuthgear.getCanReauthenticate());
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

    public void reauthenticate(FragmentActivity activity) {
        mIsLoading.setValue(true);

        mAuthgear.refreshIDToken(new OnRefreshIDTokenListener() {
            @Override
            public void onFinished() {
                if (mAuthgear.getCanReauthenticate()) {
                    ReauthenticateOptions options = new ReauthenticateOptions(MainApplication.AUTHGEAR_REDIRECT_URI);
                    options.setWechatRedirectURI(MainApplication.AUTHGEAR_WECHAT_REDIRECT_URI);
                    options.setColorScheme(getColorScheme());
                    options.setAuthenticationFlowGroup(mAuthenticationiFlowGroup.getValue());
                    mAuthgear.reauthenticate(options, makeBiometricOptions(activity), new OnReauthenticateListener() {
                        @Override
                        public void onFinished(@Nullable UserInfo userInfo) {
                            mUserInfo.setValue(userInfo);
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
                    ReauthenticateOptions options = new ReauthenticateOptions(MainApplication.AUTHGEAR_REDIRECT_URI);
                    options.setWechatRedirectURI(MainApplication.AUTHGEAR_WECHAT_REDIRECT_URI);
                    mAuthgear.reauthenticate(options, null, new OnReauthenticateListener() {
                        @Override
                        public void onFinished(@Nullable UserInfo userInfo) {
                            mUserInfo.setValue(userInfo);
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
            return ColorScheme.DARK;
        }
        return ColorScheme.LIGHT;
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
        mAuthgear.open(Page.SETTINGS, options, new OnOpenURLListener() {
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

    public void openChangePassword() {
        mIsLoading.setValue(true);

        mAuthgear.refreshIDToken(new OnRefreshIDTokenListener() {
            @Override
            public void onFinished() {
                SettingsActionOptions options = new SettingsActionOptions(
                        MainApplication.AUTHGEAR_REDIRECT_URI
                );
                options.setColorScheme(getColorScheme());
                options.setWechatRedirectURI(MainApplication.AUTHGEAR_WECHAT_REDIRECT_URI);
                mAuthgear.changePassword(options, new OnOpenSettingsActionListener() {
                    @Override
                    public void onFinished() {
                        mIsLoading.setValue(false);
                        Log.d(TAG, "changePassword finished");
                    }

                    @Override
                    public void onFailed(Throwable throwable) {
                        Log.d(TAG, throwable.toString());
                        mIsLoading.setValue(false);
                        setError(throwable);
                    }
                });
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
            public void onPromoted(@NonNull UserInfo userInfo) {
                mUserInfo.setValue(userInfo);
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

    public void preAuthenticatedURL(FragmentActivity activity) {
        boolean shouldUseAnotherBrowser = !mPreAuthenticatedURLRedirectURI.getValue().isEmpty();
        String targetRedirectURI = MainApplication.AUTHGEAR_REDIRECT_URI;
        String targetClientID = mClientID.getValue();
        if (!mPreAuthenticatedURLRedirectURI.getValue().isEmpty()) {
            targetRedirectURI = mPreAuthenticatedURLRedirectURI.getValue();
        }
        if (!mPreAuthenticatedURLClientID.getValue().isEmpty()) {
            targetClientID = mPreAuthenticatedURLClientID.getValue();
        }
        mIsLoading.setValue(true);
        PreAuthenticatedURLOptions options = new PreAuthenticatedURLOptions(
                targetClientID,
                targetRedirectURI,
                null
        );
        String finalTargetClientID = targetClientID;
        mAuthgear.makePreAuthenticatedURL(options, new OnMakePreAuthenticatedURLListener() {
            @Override
            public void onSuccess(@NonNull Uri uri) {
                mIsLoading.setValue(false);
                UIImplementation uiImpl = new CustomTabsUIImplementation();
                if (!shouldUseAnotherBrowser) {
                    uiImpl.openAuthorizationURL(
                            activity,
                            new OpenAuthorizationURLOptions(
                                    uri,
                                    Uri.parse(MainApplication.AUTHGEAR_REDIRECT_URI)
                            ),
                            new OnOpenAuthorizationURLListener() {
                                @Override
                                public void onSuccess(@NonNull Uri url) {
                                    Authgear newContainer = new Authgear(
                                            getApplication(),
                                            finalTargetClientID,
                                            mEndpoint.getValue(),
                                            new TransientTokenStorage(),
                                            uiImpl,
                                            true,
                                            false,
                                            "preAuthenticatedURL"
                                    );
                                    newContainer.configure(new OnConfigureListener() {
                                        @Override
                                        public void onConfigured() {
                                            newContainer.authenticate(new AuthenticateOptions(
                                                    MainApplication.AUTHGEAR_REDIRECT_URI
                                            ), new OnAuthenticateListener() {
                                                @Override
                                                public void onAuthenticated(@NonNull UserInfo userInfo) {
                                                    promptPreAuthenticatedURLSuccessAlert(activity, userInfo);
                                                }

                                                @Override
                                                public void onAuthenticationFailed(@NonNull Throwable throwable) {
                                                    // Ignore
                                                }
                                            });
                                        }

                                        @Override
                                        public void onConfigurationFailed(@NonNull Throwable throwable) {}
                                    });
                                }

                                @Override
                                public void onFailed(@NonNull Throwable e) {
                                    setError(e);
                                }
                            }
                    );
                } else {
                    uiImpl.openAuthorizationURL(
                            activity,
                            new OpenAuthorizationURLOptions(
                                    uri,
                                    Uri.parse(MainApplication.AUTHGEAR_REDIRECT_URI)
                            ),
                            new OnOpenAuthorizationURLListener() {
                                @Override
                                public void onSuccess(@NonNull Uri url) {}
                                @Override
                                public void onFailed(@NonNull Throwable throwable) {}
                            }
                    );
                }
            }

            @Override
            public void onFailed(@NonNull Throwable e) {
                setError(e);
                mIsLoading.setValue(false);
            }
        });
    }

    private void promptPreAuthenticatedURLSuccessAlert(Context context, UserInfo userInfo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("PreAuthenticatedURL");
        builder.setMessage("Successfully logged in");
        builder.setPositiveButton("OK", (dialogInterface, i) -> {
        });
        builder.create().show();
    }
}
