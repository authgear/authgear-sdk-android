package com.oursky.authgeartest;

import android.os.Bundle;
import android.view.View;
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
import com.oursky.authgear.PersistentTokenStorage;
import com.oursky.authgear.SessionState;
import com.oursky.authgear.TransientTokenStorage;
import com.oursky.authgear.UserInfo;

@SuppressWarnings("ConstantConditions")
public class MainActivity extends AppCompatActivity {
    private EditText mClientId;
    private EditText mEndpoint;
    private EditText mPage;
    private Spinner mTokenStorage;
    private CheckBox mShareSessionWithDeviceBrowser;
    private TextView mLoading;
    private View mConfigure;
    private View mAuthorize;
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
        mPage = findViewById(R.id.pageInput);
        mLoading = findViewById(R.id.loading);
        mConfigure = findViewById(R.id.configure);
        mAuthorize = findViewById(R.id.authorize);
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
        mShareSessionWithDeviceBrowser = findViewById(R.id.shareSessionWithDeviceBrowser);
        // Setup session type spinner
        String[] tokenStorages = {
                TransientTokenStorage.class.getSimpleName(),
                PersistentTokenStorage.class.getSimpleName(),
        };
        mTokenStorage = findViewById(R.id.tokenStorageSpinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, tokenStorages);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mTokenStorage.setAdapter(adapter);

        mConfigure.setOnClickListener(
                view -> viewModel.configure(
                        mClientId.getText().toString(),
                        mEndpoint.getText().toString(),
                        mTokenStorage.getSelectedItem().toString(),
                        mShareSessionWithDeviceBrowser.isChecked()
                )
        );
        mAuthorize.setOnClickListener(view -> viewModel.authorize(mPage.getText().toString()));
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

        mClientId.setText(viewModel.clientID().getValue());
        mEndpoint.setText(viewModel.endpoint().getValue());
        mPage.setText(viewModel.page().getValue());
        mShareSessionWithDeviceBrowser.setChecked(viewModel.shareSessionWithDeviceBrowser().getValue());

        {
            int idx = 0;
            String value = viewModel.tokenStorage().getValue();
            for (int i = 0; i < tokenStorages.length ; i++) {
                if (tokenStorages[i].equals(value)) {
                    idx = i;
                    break;
                }
            }
            mTokenStorage.setSelection(idx);
        }

        viewModel.isConfigured().observe(this, isConfigured -> {
            updateButtonDisabledState(viewModel);
            mLoading.setText(isConfigured ? "Loading..." : "Configuring...");
        });

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
        mAuthorize.setEnabled(!isLoading && isConfigured && !isLoggedIn);
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
