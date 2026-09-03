package com.monali.careermateai;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class PrivacySecurityActivity extends AppCompatActivity {

    private Button backToSettingsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_security);

        backToSettingsButton = findViewById(R.id.backToSettingsButton);

        backToSettingsButton.setOnClickListener(view -> {
            Intent intent = new Intent(PrivacySecurityActivity.this, SettingsActivity.class);
            startActivity(intent);
            finish();
        });
    }
}