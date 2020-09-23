package com.oursky.authgeartest;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

@SuppressWarnings("ConstantConditions")
public class MainActivity extends AppCompatActivity {
    private TextView mLoading;
    private View mButtonWrapper;
    private View mLogout;
    private View mAuthorize;
    private View mAuthenticateAnonymously;

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
        findViewById(R.id.authenticateAnonymously).setOnClickListener(view -> viewModel.authenticateAnonymously());

        mLoading = findViewById(R.id.loading);
        mButtonWrapper = findViewById(R.id.button_wrapper);
        mLogout = findViewById(R.id.logout);
        mAuthorize = findViewById(R.id.authorize);
        mAuthenticateAnonymously = findViewById(R.id.authenticateAnonymously);
        mLogout.setOnClickListener(view -> viewModel.logout());
        mAuthorize.setOnClickListener(view -> viewModel.authorize());
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

        findViewById(R.id.handleDeepLink).setOnClickListener(view -> viewModel.handleDeepLink());

        findViewById(R.id.promoteAnonymousUser).setOnClickListener(view -> viewModel.promoteAnonymousUser());
    }

    private void updateButtonVisibility(boolean isConfigured, boolean isLoading) {
        final boolean showLoading = !isConfigured || isLoading;
        mLoading.setVisibility(showLoading? View.VISIBLE : View.GONE);
        mButtonWrapper.setVisibility(showLoading ? View.GONE : View.VISIBLE);
    }
}
