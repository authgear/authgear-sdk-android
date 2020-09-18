package com.oursky.authgeartest;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

public class MainActivity extends AppCompatActivity {
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

        final MainViewModel viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        findViewById(R.id.authenticateAnonymously).setOnClickListener(view -> viewModel.authenticateAnonymously());

        findViewById(R.id.authorize).setOnClickListener(view -> viewModel.authorize());

        findViewById(R.id.logout).setOnClickListener(view -> viewModel.logout());

        findViewById(R.id.handleDeepLink).setOnClickListener(view -> viewModel.handleDeepLink());

        findViewById(R.id.promoteAnonymousUser).setOnClickListener(view -> viewModel.promoteAnonymousUser());
    }
}
