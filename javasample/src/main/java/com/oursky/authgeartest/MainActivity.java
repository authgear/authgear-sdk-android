package com.oursky.authgeartest;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.snackbar.Snackbar;
import com.oursky.authgear.Authgear;
import com.oursky.authgear.OnAuthorizeListener;

public class MainActivity extends AppCompatActivity {
    private Authgear mAuthgear;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // TODO: Make proper state machine based on logged in/logged out etc
        mAuthgear = ((MainApplication) getApplication()).getAuthgear();

        TextView configClientId = findViewById(R.id.config_client_id);
        configClientId.setText(BuildConfig.LOCAL_AUTHGEAR_CLIENT_ID);
        TextView configEndpoint = findViewById(R.id.config_endpoint);
        configEndpoint.setText(BuildConfig.LOCAL_AUTHGEAR_ENDPOINT);

        findViewById(R.id.authorize).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                mAuthgear.authorize(new OnAuthorizeListener() {
                    @Override
                    public void onAuthorized(@NonNull String state) {
                        Snackbar.make(view, state, Snackbar.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onAuthorizationFailed(@NonNull Throwable throwable) {

                    }
                });
            }
        });

        findViewById(R.id.logout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAuthgear.logout();
            }
        });

        findViewById(R.id.handleDeepLink).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAuthgear.handleDeepLink();
            }
        });

        findViewById(R.id.promoteAnonymousUser).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAuthgear.promoteAnonymousUser();
            }
        });
    }
}
