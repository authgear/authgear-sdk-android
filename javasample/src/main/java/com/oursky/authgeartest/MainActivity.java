package com.oursky.authgeartest;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.oursky.authgear.UserInfo;

@SuppressWarnings("ConstantConditions")
public class MainActivity extends AppCompatActivity {
    private TextView mLoading;
    private View mButtonWrapper;
    private View mLogout;
    private View mAuthorize;
    private View mAuthenticateAnonymously;
    private View mUserInfoWrapper;
    private TextView mUserInfoIsAnonymous;
    private View mAccessTokenWrapper;
    private TextView mAccessToken;
    private boolean mHasBindError = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        TextView configClientId = findViewById(R.id.config_client_id);
        configClientId.setText(BuildConfig.LOCAL_AUTHGEAR_CLIENT_ID);
        TextView configEndpoint = findViewById(R.id.config_endpoint);
        configEndpoint.setText(BuildConfig.LOCAL_AUTHGEAR_ENDPOINT);

        final MainApplication mainApp = (MainApplication) getApplication();
        final MainViewModel viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        mLoading = findViewById(R.id.loading);
        mButtonWrapper = findViewById(R.id.button_wrapper);
        mLogout = findViewById(R.id.logout);
        mAuthorize = findViewById(R.id.authorize);
        mAuthenticateAnonymously = findViewById(R.id.authenticateAnonymously);
        mUserInfoWrapper = findViewById(R.id.user_info_wrapper);
        mUserInfoIsAnonymous = findViewById(R.id.is_anonymous);
        mAccessTokenWrapper = findViewById(R.id.access_token_wrapper);
        mAccessToken = findViewById(R.id.access_token);

        mLogout.setOnClickListener(view -> viewModel.logout());
        mAuthorize.setOnClickListener(view -> viewModel.authorize());
        mAuthenticateAnonymously.setOnClickListener(view -> viewModel.authenticateAnonymously());
        findViewById(R.id.open_settings).setOnClickListener(view -> viewModel.openSettings(this));
        findViewById(R.id.promoteAnonymousUser).setOnClickListener(view -> viewModel.promoteAnonymousUser());
        findViewById(R.id.fetchUserInfo).setOnClickListener(view -> viewModel.fetchUserInfo());

        mainApp.isConfigured().observe(this, isConfigured -> {
            updateButtonVisibility(isConfigured, viewModel.isLoading().getValue());
            mLoading.setText(isConfigured ? "Loading..." : "Configuring...");
        });
        viewModel.isLoggedIn().observe(this, isLoggedIn -> {
            mLogout.setVisibility(isLoggedIn ? View.VISIBLE : View.GONE);
            mAuthorize.setVisibility(isLoggedIn ? View.GONE : View.VISIBLE);
            mAuthenticateAnonymously.setVisibility(isLoggedIn ? View.GONE : View.VISIBLE);
        });
        viewModel.isLoading().observe(this, isLoading -> updateButtonVisibility(mainApp.isConfigured().getValue(), isLoading));
        viewModel.userInfo().observe(this, this::updateUserInfo);
        viewModel.accessToken().observe(this, accessToken -> {
            mAccessTokenWrapper.setVisibility(accessToken == null ? View.GONE : View.VISIBLE);
            mAccessToken.setText(accessToken);
        });
        viewModel.error().observe(this, e -> {
            if (!mHasBindError) {
                mHasBindError = true;
                return;
            }
            if (e == null) return;
            Snackbar.make(findViewById(android.R.id.content), e.toString(), Snackbar.LENGTH_SHORT);
        });
    }

    private void updateButtonVisibility(boolean isConfigured, boolean isLoading) {
        final boolean showLoading = !isConfigured || isLoading;
        mLoading.setVisibility(showLoading? View.VISIBLE : View.GONE);
        mButtonWrapper.setVisibility(showLoading ? View.GONE : View.VISIBLE);
    }

    @SuppressLint("SetTextI18n")
    private void updateUserInfo(UserInfo userInfo) {
        if (userInfo == null) {
            mUserInfoWrapper.setVisibility(View.GONE);
        } else {
            mUserInfoWrapper.setVisibility(View.VISIBLE);
            mUserInfoIsAnonymous.setText("" + userInfo.isAnonymous());
        }
    }
}
