package com.oursky.authgeartest;

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
import com.oursky.authgear.UIVariant;
import com.oursky.authgear.UserInfo;

@SuppressWarnings("ConstantConditions")
public class MainActivity extends AppCompatActivity {
    private static final String COLOR_SCHEME_USE_SYSTEM = "Use System";
    private static final String PAGE_UNSET = "Unset";

    private EditText mClientId;
    private EditText mEndpoint;
    private Spinner mPage;
    private Spinner mTokenStorage;
    private Spinner mColorScheme;
    private Spinner mUIVariant;
    private CheckBox mIsSsoEnabled;
    private EditText mCustomUIQuery;
    private TextView mSessionState;
    private TextView mLoading;
    private View mConfigure;
    private View mAuthenticate;
    private View mAuthenticateAnonymously;
    private View mPromoteAnonymousUser;
    private View mReauthenticate;
    private View mReauthenticateWebOnly;
    private View mEnableBiometric;
    private View mDisableBiometric;
    private View mAuthenticateBiometric;
    private View mOpenSettings;
    private View mFetchUserInfo;
    private View mShowAuthTime;
    private View mLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final MainViewModel viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        mClientId = findViewById(R.id.clientIdInput);
        mEndpoint = findViewById(R.id.endpointInput);
        mLoading = findViewById(R.id.loading);
        mConfigure = findViewById(R.id.configure);
        mAuthenticate = findViewById(R.id.authenticate);
        mAuthenticateAnonymously = findViewById(R.id.authenticateAnonymously);
        mPromoteAnonymousUser = findViewById(R.id.promoteAnonymousUser);
        mReauthenticate = findViewById(R.id.reauthenticate);
        mReauthenticateWebOnly = findViewById(R.id.reauthenticateWebOnly);
        mEnableBiometric = findViewById(R.id.enableBiometric);
        mDisableBiometric = findViewById(R.id.disableBiometric);
        mAuthenticateBiometric = findViewById(R.id.authenticateBiometric);
        mOpenSettings = findViewById(R.id.openSettings);
        mFetchUserInfo = findViewById(R.id.fetchUserInfo);
        mShowAuthTime = findViewById(R.id.showAuthTime);
        mLogout = findViewById(R.id.logout);
        mIsSsoEnabled = findViewById(R.id.isSsoEnabled);
        mSessionState = findViewById(R.id.sessionStateInput);
        mCustomUIQuery = findViewById(R.id.customUIQueryInput);

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

        String[] uiVariants = {
                UIVariant.CUSTOM_TABS.name(),
                UIVariant.WEB_VIEW.name(),
                UIVariant.WEB_VIEW_FULL_SCREEN.name(),
        };
        mUIVariant = findViewById(R.id.uiVariantSpinner);
        ArrayAdapter<String> mUIVariantAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, uiVariants);
        mUIVariantAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mUIVariant.setAdapter(mUIVariantAdapter);

        mConfigure.setOnClickListener(
                view -> viewModel.configure(
                        mClientId.getText().toString(),
                        mEndpoint.getText().toString(),
                        mIsSsoEnabled.isChecked()
                )
        );
        mAuthenticate.setOnClickListener(view -> viewModel.authenticate());
        mAuthenticateAnonymously.setOnClickListener(view -> viewModel.authenticateAnonymously());
        mPromoteAnonymousUser.setOnClickListener(view -> viewModel.promoteAnonymousUser());
        mReauthenticate.setOnClickListener(view -> viewModel.reauthenticate(this));
        mReauthenticateWebOnly.setOnClickListener(view -> viewModel.reauthenticateWebOnly());
        mEnableBiometric.setOnClickListener(view -> viewModel.enableBiometric(this));
        mDisableBiometric.setOnClickListener(view -> viewModel.disableBiometric());
        mAuthenticateBiometric.setOnClickListener(view -> viewModel.authenticateBiometric(this));
        mOpenSettings.setOnClickListener(view -> viewModel.openSettings());
        mFetchUserInfo.setOnClickListener(view -> viewModel.fetchUserInfo());
        mShowAuthTime.setOnClickListener(view -> viewModel.showAuthTime(this));
        mLogout.setOnClickListener(view -> viewModel.logout());

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
        mUIVariant.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String value = (String) parent.getItemAtPosition(position);
                if (UIVariant.CUSTOM_TABS.name().equals(value)) {
                    viewModel.setUIVariant(UIVariant.CUSTOM_TABS);
                } else if (UIVariant.WEB_VIEW.name().equals(value)) {
                    viewModel.setUIVariant(UIVariant.WEB_VIEW);
                } else if (UIVariant.WEB_VIEW_FULL_SCREEN.name().equals(value)) {
                    viewModel.setUIVariant(UIVariant.WEB_VIEW_FULL_SCREEN);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        mCustomUIQuery.setText(viewModel.customUIQuery().getValue());
        mCustomUIQuery.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                viewModel.setCustomUIQuery(charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        mClientId.setText(viewModel.clientID().getValue());
        mEndpoint.setText(viewModel.endpoint().getValue());
        mIsSsoEnabled.setChecked(viewModel.isSsoEnabled().getValue());
        mSessionState.setText(viewModel.sessionState().getValue().toString());

        viewModel.isConfigured().observe(this, isConfigured -> {
            updateButtonDisabledState(viewModel);
            mLoading.setText(isConfigured ? "Loading..." : "Configuring...");
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

        viewModel.verifyLoginLink(getIntent());
    }

    private void updateButtonDisabledState(MainViewModel viewModel) {
        UserInfo userInfo = viewModel.userInfo().getValue();
        boolean isLoading = viewModel.isLoading().getValue();
        boolean isConfigured = viewModel.isConfigured().getValue();
        boolean isAnonymous = userInfo != null && userInfo.isAnonymous();
        boolean isBiometricEnabled = viewModel.isBiometricEnabled().getValue();
        boolean isLoggedIn = viewModel.sessionState().getValue() == SessionState.AUTHENTICATED;
        boolean canReauthenticate = viewModel.canReauthenticate().getValue();
        mLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        mConfigure.setEnabled(!isLoading);
        mAuthenticate.setEnabled(!isLoading && isConfigured && !isLoggedIn);
        mAuthenticateAnonymously.setEnabled(!isLoading && isConfigured && !isLoggedIn);
        mPromoteAnonymousUser.setEnabled(!isLoading && isConfigured && isLoggedIn && isAnonymous);
        mReauthenticate.setEnabled(!isLoading && isConfigured && isLoggedIn && !isAnonymous);
        mReauthenticateWebOnly.setEnabled(!isLoading && isConfigured && isLoggedIn && !isAnonymous && canReauthenticate);
        mEnableBiometric.setEnabled(!isLoading && isConfigured && isLoggedIn && !isBiometricEnabled);
        mDisableBiometric.setEnabled(!isLoading && isConfigured && isBiometricEnabled);
        mAuthenticateBiometric.setEnabled(!isLoading && isConfigured && !isLoggedIn && isBiometricEnabled);
        mOpenSettings.setEnabled(!isLoading && isConfigured && isLoggedIn);
        mFetchUserInfo.setEnabled(!isLoading && isConfigured && isLoggedIn);
        mShowAuthTime.setEnabled(!isLoading && isConfigured && isLoggedIn);
        mLogout.setEnabled(!isLoading && isConfigured && isLoggedIn);
    }
}
