package com.monali.careermateai;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class OnboardingActivity extends AppCompatActivity {

    private TextView onboardIcon, onboardTitle, onboardDescription, skipText;
    private View dot1, dot2, dot3;
    private Button nextButton;

    private int currentPage = 0;

    private final String[] icons = {
            "🎓",
            "🤖",
            "📈"
    };

    private final String[] titles = {
            "Build Your Career Readiness",
            "Get AI-Powered Guidance",
            "Track Your Progress"
    };

    private final String[] descriptions = {
            "Manage your CV, portfolio and interview preparation in one place.",
            "Receive CV feedback, interview practice and skill-gap suggestions.",
            "Improve your career readiness step by step."
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        onboardIcon = findViewById(R.id.onboardIcon);
        onboardTitle = findViewById(R.id.onboardTitle);
        onboardDescription = findViewById(R.id.onboardDescription);
        skipText = findViewById(R.id.skipText);
        nextButton = findViewById(R.id.nextButton);

        dot1 = findViewById(R.id.dot1);
        dot2 = findViewById(R.id.dot2);
        dot3 = findViewById(R.id.dot3);

        updateOnboardingScreen();

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goToNextScreen();
            }
        });

        skipText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goToLogin();
            }
        });
    }

    private void goToNextScreen() {
        if (currentPage < 2) {
            currentPage++;
            updateOnboardingScreen();
        } else {
            goToLogin();
        }
    }

    private void updateOnboardingScreen() {
        onboardIcon.setText(icons[currentPage]);
        onboardTitle.setText(titles[currentPage]);
        onboardDescription.setText(descriptions[currentPage]);

        dot1.setBackgroundResource(currentPage == 0 ? R.drawable.dot_active : R.drawable.dot_inactive);
        dot2.setBackgroundResource(currentPage == 1 ? R.drawable.dot_active : R.drawable.dot_inactive);
        dot3.setBackgroundResource(currentPage == 2 ? R.drawable.dot_active : R.drawable.dot_inactive);

        if (currentPage == 2) {
            nextButton.setText("Get Started 🎯");
            skipText.setVisibility(View.INVISIBLE);
        } else {
            nextButton.setText("Next →");
            skipText.setVisibility(View.VISIBLE);
        }
    }

    private void goToLogin() {
        Intent intent = new Intent(OnboardingActivity.this, LoginActivity.class);

        // This clears onboarding from the back stack.
        // After clicking Skip or Get Started, user goes directly to Login.
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(intent);
    }
}