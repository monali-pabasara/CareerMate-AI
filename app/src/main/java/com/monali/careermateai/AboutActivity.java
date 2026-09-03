package com.monali.careermateai;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class AboutActivity extends AppCompatActivity {

    private Button backToSettingsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        backToSettingsButton = findViewById(R.id.backToSettingsButton);

        backToSettingsButton.setOnClickListener(view -> {
            Intent intent = new Intent(AboutActivity.this, SettingsActivity.class);
            startActivity(intent);
            finish();
        });
    }
}