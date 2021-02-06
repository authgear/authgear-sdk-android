package com.oursky.authgeartest;

import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.oursky.authgear.UserInfo;

@SuppressWarnings("ConstantConditions")
public class MainActivity extends AppCompatActivity {
    private EditText mClientId;
    private EditText mEndpoint;
    private EditText mPage;
    private CheckBox mIsThirdParty;
    private TextView mLoading;
    private View mAuthorize;
    private View mAuthenticateAnonymously;
    private View mPromoteAnonymousUser;
    private View mOpenSettings;
    private View mFetchUserInfo;
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
        mIsThirdParty = findViewById(R.id.isThirdPartyInput);
        mLoading = findViewById(R.id.loading);
        mAuthorize = findViewById(R.id.authorize);
        mAuthenticateAnonymously = findViewById(R.id.authenticateAnonymously);
        mPromoteAnonymousUser = findViewById(R.id.promoteAnonymousUser);
        mOpenSettings = findViewById(R.id.openSettings);
        mFetchUserInfo = findViewById(R.id.fetchUserInfo);
        mLogout = findViewById(R.id.logout);

        findViewById(R.id.configure).setOnClickListener(
            view -> viewModel.configure(
                mClientId.getText().toString(),
                mEndpoint.getText().toString(),
                mIsThirdParty.isChecked()
            )
        );
        mAuthorize.setOnClickListener(view -> viewModel.authorize(mPage.getText().toString()));
        mAuthenticateAnonymously.setOnClickListener(view -> viewModel.authenticateAnonymously());
        mOpenSettings.setOnClickListener(view -> viewModel.openSettings());
        mPromoteAnonymousUser.setOnClickListener(view -> viewModel.promoteAnonymousUser());
        mFetchUserInfo.setOnClickListener(view -> viewModel.fetchUserInfo());
        mLogout.setOnClickListener(view -> viewModel.logout());

        mClientId.setText(viewModel.clientID().getValue());
        mEndpoint.setText(viewModel.endpoint().getValue());
        mPage.setText(viewModel.page().getValue());
        mIsThirdParty.setChecked(viewModel.isThirdParty().getValue());

        viewModel.isConfigured().observe(this, isConfigured -> {
            updateButtonDisabledState(viewModel);
            mLoading.setText(isConfigured ? "Loading..." : "Configuring...");
        });

        viewModel.isLoggedIn().observe(this, isLoggedIn -> updateButtonDisabledState(viewModel));

        viewModel.isLoading().observe(this, isLoading -> updateButtonDisabledState(viewModel));

        viewModel.userInfo().observe(this, userInfo -> {
            if (userInfo == null) return;
            updateButtonDisabledState(viewModel);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Got UserInfo");
            builder.setMessage(userInfo.toString());
            builder.setPositiveButton("OK", (dialogInterface, i) -> {
            });
            builder.create().show();
        });

        viewModel.successDialogMessage().observe(this, message -> {
            if (message == null || message == "") return;
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Success");
            builder.setMessage(message);
            builder.setPositiveButton("OK", (dialogInterface, i) -> {
            });
            builder.create().show();
        });

        viewModel.error().observe(this, e -> {
            if (e == null) return;
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Error");
            builder.setMessage(e.toString());
            builder.setPositiveButton("OK", (dialogInterface, i) -> {
            });
            builder.create().show();
        });
    }

    private void updateButtonDisabledState(MainViewModel viewModel) {
        UserInfo userInfo = viewModel.userInfo().getValue();
        boolean isLoading = viewModel.isLoading().getValue();
        boolean isConfigured = viewModel.isConfigured().getValue();
        boolean isLoggedIn = viewModel.isLoggedIn().getValue();
        boolean isAnonymous = userInfo != null && userInfo.isAnonymous();
        mLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        mAuthorize.setEnabled(!isLoading && isConfigured && !isLoggedIn);
        mAuthenticateAnonymously.setEnabled(!isLoading && isConfigured && !isLoggedIn);
        mPromoteAnonymousUser.setEnabled(!isLoading && isConfigured && isLoggedIn && isAnonymous);
        mOpenSettings.setEnabled(!isLoading && isConfigured);
        mFetchUserInfo.setEnabled(!isLoading && isConfigured && isLoggedIn);
        mLogout.setEnabled(!isLoading && isConfigured && isLoggedIn);
    }
}
