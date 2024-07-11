package com.oursky.authgeartest;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.oursky.authgear.BiometricLockoutException;
import com.oursky.authgear.BiometricNoEnrollmentException;
import com.oursky.authgear.BiometricNoPasscodeException;
import com.oursky.authgear.BiometricNotSupportedOrPermissionDeniedException;
import com.oursky.authgear.BiometricPrivateKeyNotFoundException;
import com.oursky.authgear.ColorScheme;
import com.oursky.authgear.PersistentTokenStorage;
import com.oursky.authgear.SessionState;
import com.oursky.authgear.TransientTokenStorage;
import com.oursky.authgear.UserInfo;

@SuppressWarnings("ConstantConditions")
public class MainActivity extends AppCompatActivity {
    private static final String COLOR_SCHEME_USE_SYSTEM = "Use System";
    private static final String PAGE_UNSET = "Unset";

    private EditText mClientId;
    private EditText mEndpoint;
    private EditText mApp2AppEndpoint;
    private EditText mApp2AppState;
    private EditText mPreAuthenticatedURLClientID;
    private EditText mPreAuthenticatedURLRedirectURI;
    private View mApp2AppstateField;
    private EditText mAuthenticationFlowGroup;
    private Spinner mPage;
    private Spinner mTokenStorage;
    private Spinner mColorScheme;
    private CheckBox mUseWebKitWebView;
    private CheckBox mIsSsoEnabled;
    private CheckBox mIsPreAuthenticatedURLEnabled;
    private TextView mSessionState;
    private TextView mLoading;
    private View mConfigure;
    private View mAuthenticate;
    private View mAuthenticateApp2App;
    private View mAuthenticateAnonymously;
    private View mPromoteAnonymousUser;
    private View mReauthenticate;
    private View mReauthenticateWebOnly;
    private View mEnableBiometric;
    private View mDisableBiometric;
    private View mAuthenticateBiometric;
    private View mPreAuthenticatedURL;
    private View mOpenSettings;
    private View mChangePassword;
    private View mFetchUserInfo;
    private View mShowAuthTime;
    private View mLogout;
    private MainViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final MainViewModel viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        this.viewModel = viewModel;

        mClientId = findViewById(R.id.clientIdInput);
        mEndpoint = findViewById(R.id.endpointInput);
        mApp2AppEndpoint = findViewById(R.id.app2appendpointInput);
        mApp2AppState = findViewById(R.id.app2appstateInput);
        mApp2AppstateField = findViewById(R.id.app2appstateField);
        mAuthenticationFlowGroup = findViewById(R.id.authenticationFlowGroupInput);
        mPreAuthenticatedURLClientID = findViewById(R.id.preAuthenticatedURLClientIDInput);
        mPreAuthenticatedURLRedirectURI = findViewById(R.id.preAuthenticatedURLRedirectURIInput);
        mLoading = findViewById(R.id.loading);
        mConfigure = findViewById(R.id.configure);
        mAuthenticate = findViewById(R.id.authenticate);
        mAuthenticateApp2App = findViewById(R.id.authenticateapp2app);
        mAuthenticateAnonymously = findViewById(R.id.authenticateAnonymously);
        mPromoteAnonymousUser = findViewById(R.id.promoteAnonymousUser);
        mReauthenticate = findViewById(R.id.reauthenticate);
        mReauthenticateWebOnly = findViewById(R.id.reauthenticateWebOnly);
        mEnableBiometric = findViewById(R.id.enableBiometric);
        mDisableBiometric = findViewById(R.id.disableBiometric);
        mAuthenticateBiometric = findViewById(R.id.authenticateBiometric);
        mPreAuthenticatedURL = findViewById(R.id.preAuthenticatedURL);
        mOpenSettings = findViewById(R.id.openSettings);
        mChangePassword = findViewById(R.id.changePassword);
        mFetchUserInfo = findViewById(R.id.fetchUserInfo);
        mShowAuthTime = findViewById(R.id.showAuthTime);
        mLogout = findViewById(R.id.logout);
        mUseWebKitWebView = findViewById(R.id.useWebKitWebView);
        mIsSsoEnabled = findViewById(R.id.isSsoEnabled);
        mIsPreAuthenticatedURLEnabled = findViewById(R.id.isPreAuthenticatedURLEnabled);
        mSessionState = findViewById(R.id.sessionStateInput);

        String[] pages = {
            PAGE_UNSET,
            "login",
            "signup",
        };
        mPage = findViewById(R.id.pageSpinner);
        ArrayAdapter<String> pageAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, pages);
        pageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mPage.setAdapter(pageAdapter);

        // Setup session type spinner
        String[] tokenStorages = {
                TransientTokenStorage.class.getSimpleName(),
                PersistentTokenStorage.class.getSimpleName(),
        };
        mTokenStorage = findViewById(R.id.tokenStorageSpinner);
        ArrayAdapter<String> tokenStorageAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, tokenStorages);
        tokenStorageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mTokenStorage.setAdapter(tokenStorageAdapter);

        String[] colorSchemes = {
                COLOR_SCHEME_USE_SYSTEM,
                ColorScheme.LIGHT.name(),
                ColorScheme.DARK.name(),
        };
        mColorScheme = findViewById(R.id.colorSchemeSpinner);
        ArrayAdapter<String> colorSchemeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, colorSchemes);
        colorSchemeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mColorScheme.setAdapter(colorSchemeAdapter);

        mConfigure.setOnClickListener(
                view -> viewModel.configure(
                        mClientId.getText().toString(),
                        mEndpoint.getText().toString(),
                        mIsSsoEnabled.isChecked(),
                        mUseWebKitWebView.isChecked(),
                        mApp2AppEndpoint.getText().toString(),
                        mIsPreAuthenticatedURLEnabled.isChecked(),
                        mPreAuthenticatedURLClientID.getText().toString(),
                        mPreAuthenticatedURLRedirectURI.getText().toString()
                )
        );
        mAuthenticate.setOnClickListener(view -> viewModel.authenticate());
        mAuthenticateApp2App.setOnClickListener(view -> viewModel.authenticateApp2App(mApp2AppState.getText().toString()));
        mAuthenticateAnonymously.setOnClickListener(view -> viewModel.authenticateAnonymously());
        mPromoteAnonymousUser.setOnClickListener(view -> viewModel.promoteAnonymousUser());
        mReauthenticate.setOnClickListener(view -> viewModel.reauthenticate(this));
        mReauthenticateWebOnly.setOnClickListener(view -> viewModel.reauthenticateWebOnly());
        mEnableBiometric.setOnClickListener(view -> viewModel.enableBiometric(this));
        mDisableBiometric.setOnClickListener(view -> viewModel.disableBiometric());
        mAuthenticateBiometric.setOnClickListener(view -> viewModel.authenticateBiometric(this));
        mPreAuthenticatedURL.setOnClickListener(view -> viewModel.preAuthenticatedURL(this));
        mOpenSettings.setOnClickListener(view -> viewModel.openSettings());
        mChangePassword.setOnClickListener(view -> viewModel.openChangePassword());
        mFetchUserInfo.setOnClickListener(view -> viewModel.fetchUserInfo());
        mShowAuthTime.setOnClickListener(view -> viewModel.showAuthTime(this));
        mLogout.setOnClickListener(view -> viewModel.logout());

        mAuthenticationFlowGroup.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                viewModel.setAuthenticationiFlowGroup(editable.toString());
            }
        });

        mPage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String value = (String) parent.getItemAtPosition(position);
                if (PAGE_UNSET.equals(value)) {
                    viewModel.setPage("");
                } else {
                    viewModel.setPage(value);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        mTokenStorage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String value = (String) parent.getItemAtPosition(position);
                viewModel.setTokenStorage(value);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        mColorScheme.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String value = (String) parent.getItemAtPosition(position);
                if (COLOR_SCHEME_USE_SYSTEM.equals(value)) {
                    viewModel.setColorScheme(null);
                } else if (ColorScheme.LIGHT.name().equals(value)) {
                    viewModel.setColorScheme(ColorScheme.LIGHT);
                } else if (ColorScheme.DARK.name().equals(value)) {
                    viewModel.setColorScheme(ColorScheme.DARK);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        mClientId.setText(viewModel.clientID().getValue());
        mEndpoint.setText(viewModel.endpoint().getValue());
        mApp2AppEndpoint.setText(viewModel.app2appEndpoint().getValue());
        mIsSsoEnabled.setChecked(viewModel.isSsoEnabled().getValue());
        mUseWebKitWebView.setChecked(viewModel.useWebKitWebView().getValue());
        mSessionState.setText(viewModel.sessionState().getValue().toString());
        mIsPreAuthenticatedURLEnabled.setChecked(viewModel.isPreAuthenticatedURLEnabled().getValue());
        mPreAuthenticatedURLClientID.setText(viewModel.preAuthenticatedURLClientID().getValue());
        mPreAuthenticatedURLRedirectURI.setText(viewModel.preAuthenticatedURLRedirectURI().getValue());

        viewModel.isConfigured().observe(this, isConfigured -> {
            updateButtonDisabledState(viewModel);
            mLoading.setText(isConfigured ? "Loading..." : "Configuring...");
            boolean app2appEnabled = !viewModel.app2appEndpoint().getValue().isEmpty();
            mApp2AppstateField.setVisibility(app2appEnabled ? View.VISIBLE : View.GONE);
        });

        viewModel.sessionState().observe(this, sessionState -> mSessionState.setText(viewModel.sessionState().getValue().toString()));

        viewModel.isLoading().observe(this, isLoading -> updateButtonDisabledState(viewModel));
        viewModel.isBiometricEnabled().observe(this, isEnabled -> updateButtonDisabledState(viewModel));

        viewModel.userInfo().observe(this, userInfo -> {
            updateButtonDisabledState(viewModel);
            if (userInfo == null) return;
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Got UserInfo");
            builder.setMessage(userInfo.toString());
            builder.setPositiveButton("OK", (dialogInterface, i) -> {
            });
            builder.create().show();
        });

        viewModel.error().observe(this, e -> {
            if (e == null) return;
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Error");

            String message = e.toString();
            if (e instanceof BiometricPrivateKeyNotFoundException) {
                message = "Your biometric info has changed. For security reason, you have to set up biometric authentication again.";
            } else if (e instanceof BiometricNoEnrollmentException) {
                message = "You have not set up biometric yet. Please set up your fingerprint or face";
            } else if (e instanceof BiometricNotSupportedOrPermissionDeniedException) {
                message = "Your device does not support biometric. The developer should have checked this and not letting you to see feature that requires biometric";
            } else if (e instanceof BiometricNoPasscodeException) {
                message = "You device does not have credential set up. Please set up either a PIN, a pattern or a password";
            } else if (e instanceof BiometricLockoutException) {
                message = "The biometric is locked out due to too many failed attempts. The developer should handle this error by using normal authentication as a fallback. So normally you should not see this error";
            }

            builder.setMessage(message);
            builder.setPositiveButton("OK", (dialogInterface, i) -> {
            });
            builder.create().show();
        });

        viewModel.app2appConfirmation().observe(this, c -> {
            if (c == null) {
                return;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("App2App");
            builder.setMessage(c.message);
            builder.setPositiveButton("OK", (dialogInterface, i) -> {
                c.onConfirm();
            });
            builder.setNegativeButton("Cancel", (dialogInterface, i) -> {
                c.onCancel();
            });
            builder.create().show();
        });

        if (savedInstanceState == null) {
            // Is first launch
            viewModel.appendApp2AppRequest(this.getIntent());
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        viewModel.appendApp2AppRequest(intent);
    }

    private void updateButtonDisabledState(MainViewModel viewModel) {
        UserInfo userInfo = viewModel.userInfo().getValue();
        boolean isLoading = viewModel.isLoading().getValue();
        boolean isConfigured = viewModel.isConfigured().getValue();
        boolean isAnonymous = userInfo != null && userInfo.isAnonymous();
        boolean isBiometricEnabled = viewModel.isBiometricEnabled().getValue();
        boolean isLoggedIn = viewModel.sessionState().getValue() == SessionState.AUTHENTICATED;
        boolean canReauthenticate = viewModel.canReauthenticate().getValue();
        boolean isPreAuthenticatedURLEnabled = viewModel.isPreAuthenticatedURLEnabled().getValue();
        String app2appEndpoint = viewModel.app2appEndpoint().getValue();
        mLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        mConfigure.setEnabled(!isLoading);
        mAuthenticate.setEnabled(!isLoading && isConfigured && !isLoggedIn);
        mAuthenticateApp2App.setEnabled(!isLoading && isConfigured && !isLoggedIn && !app2appEndpoint.isEmpty());
        mAuthenticateAnonymously.setEnabled(!isLoading && isConfigured && !isLoggedIn);
        mPromoteAnonymousUser.setEnabled(!isLoading && isConfigured && isLoggedIn && isAnonymous);
        mReauthenticate.setEnabled(!isLoading && isConfigured && isLoggedIn && !isAnonymous);
        mReauthenticateWebOnly.setEnabled(!isLoading && isConfigured && isLoggedIn && !isAnonymous && canReauthenticate);
        mEnableBiometric.setEnabled(!isLoading && isConfigured && isLoggedIn && !isBiometricEnabled);
        mDisableBiometric.setEnabled(!isLoading && isConfigured && isBiometricEnabled);
        mAuthenticateBiometric.setEnabled(!isLoading && isConfigured && !isLoggedIn && isBiometricEnabled);
        mPreAuthenticatedURL.setEnabled(!isLoading && isConfigured && isLoggedIn && isPreAuthenticatedURLEnabled);
        mOpenSettings.setEnabled(!isLoading && isConfigured && isLoggedIn);
        mFetchUserInfo.setEnabled(!isLoading && isConfigured && isLoggedIn);
        mShowAuthTime.setEnabled(!isLoading && isConfigured && isLoggedIn);
        mLogout.setEnabled(!isLoading && isConfigured && isLoggedIn);
    }
}
