package com.monali.careermateai;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EmailVerificationActivity extends AppCompatActivity {

    private TextView emailDisplayText, verificationMessageText;
    private Button verifiedButton, resendEmailButton, backToLoginButton;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    private static final String PREF_NAME = "CareerMateUser";

    private boolean firstAutoEmailAttemptDone = false;
    private Handler resendHandler = new Handler();
    private int resendSecondsLeft = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_verification);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        emailDisplayText = findViewById(R.id.emailDisplayText);
        verificationMessageText = findViewById(R.id.verificationMessageText);
        verifiedButton = findViewById(R.id.verifiedButton);
        resendEmailButton = findViewById(R.id.resendEmailButton);
        backToLoginButton = findViewById(R.id.backToLoginButton);

        showCurrentUserEmail();
        updateVerificationMessage();

        verifiedButton.setOnClickListener(view -> checkIfEmailVerified());
        resendEmailButton.setOnClickListener(view -> resendVerificationEmail());
        backToLoginButton.setOnClickListener(view -> goToLogin());


    }

    private void showCurrentUserEmail() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser != null && currentUser.getEmail() != null) {
            emailDisplayText.setText(currentUser.getEmail());
        } else {
            SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
            String savedEmail = preferences.getString("email", "No email found");
            emailDisplayText.setText(savedEmail);
        }
    }

    private void updateVerificationMessage() {
        verificationMessageText.setText(
                "A verification email has been sent to your email address.\n\n" +
                        "Please check your Inbox, Spam/Junk or Promotions folder, then click the verification link.\n\n" +
                        "After verifying, return to this app and tap \"I Have Verified My Email\"."
        );
    }

    private void sendVerificationEmailAutomaticallyOnce() {
        if (firstAutoEmailAttemptDone) {
            return;
        }

        firstAutoEmailAttemptDone = true;

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            CustomToast.showError(this, "No user found. Please log in again.");
            goToLogin();
            return;
        }

        if (currentUser.isEmailVerified()) {
            saveVerificationStatus(currentUser.getUid());
            goToProfileSetup();
            return;
        }

        firebaseAuth.useAppLanguage();

        resendEmailButton.setEnabled(false);
        resendEmailButton.setText("Sending verification email...");

        currentUser.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        CustomToast.showSuccess(this, "Verification email sent.");
                        startResendCooldown();
                    } else {
                        resendEmailButton.setEnabled(true);
                        resendEmailButton.setText("Resend Verification Email");

                        String errorMessage = "Could not send verification email.";

                        if (task.getException() != null && task.getException().getMessage() != null) {
                            errorMessage = task.getException().getMessage();
                        }

                        CustomToast.showError(this, errorMessage);
                    }
                });
    }

    private void checkIfEmailVerified() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            CustomToast.showError(this, "No user found. Please log in again.");
            goToLogin();
            return;
        }

        verifiedButton.setEnabled(false);
        verifiedButton.setText("Checking...");

        currentUser.reload().addOnCompleteListener(task -> {
            verifiedButton.setEnabled(true);
            verifiedButton.setText("I Have Verified My Email");

            if (task.isSuccessful()) {
                FirebaseUser updatedUser = firebaseAuth.getCurrentUser();

                if (updatedUser != null && updatedUser.isEmailVerified()) {
                    saveVerificationStatus(updatedUser.getUid());
                    CustomToast.showSuccess(this, "Email verified successfully.");
                    goToProfileSetup();

                } else {
                    CustomToast.showInfo(
                            this,
                            "Email is still not verified. Please check Inbox, Spam/Junk or Promotions."
                    );
                }

            } else {
                String errorMessage = "Failed to check verification status.";

                if (task.getException() != null && task.getException().getMessage() != null) {
                    errorMessage = task.getException().getMessage();
                }

                CustomToast.showError(this, errorMessage);
            }
        });
    }

    private void resendVerificationEmail() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            CustomToast.showError(this, "No user found. Please log in again.");
            goToLogin();
            return;
        }

        if (currentUser.isEmailVerified()) {
            saveVerificationStatus(currentUser.getUid());
            CustomToast.showSuccess(this, "Email already verified.");
            goToProfileSetup();
            return;
        }

        firebaseAuth.useAppLanguage();

        resendEmailButton.setEnabled(false);
        resendEmailButton.setText("Sending...");

        currentUser.sendEmailVerification().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                CustomToast.showSuccess(this, "Verification email sent again.");
                startResendCooldown();
            } else {
                resendEmailButton.setEnabled(true);
                resendEmailButton.setText("Resend Verification Email");

                String errorMessage = "Could not resend verification email.";

                if (task.getException() != null && task.getException().getMessage() != null) {
                    errorMessage = task.getException().getMessage();
                }

                CustomToast.showError(this, errorMessage);
            }
        });
    }

    private void startResendCooldown() {
        resendSecondsLeft = 30;
        resendEmailButton.setEnabled(false);

        resendHandler.post(new Runnable() {
            @Override
            public void run() {
                if (resendSecondsLeft > 0) {
                    resendEmailButton.setText("Resend in " + resendSecondsLeft + "s");
                    resendSecondsLeft--;
                    resendHandler.postDelayed(this, 1000);
                } else {
                    resendEmailButton.setEnabled(true);
                    resendEmailButton.setText("Resend Verification Email");
                }
            }
        });
    }

    private void saveVerificationStatus(String userId) {
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("emailVerified", true);
        editor.apply();

        Map<String, Object> updates = new HashMap<>();
        updates.put("emailVerified", true);

        firestore.collection("users")
                .document(userId)
                .update(updates);
    }

    private void goToProfileSetup() {
        Intent intent = new Intent(EmailVerificationActivity.this, ProfileSetupActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void goToLogin() {
        firebaseAuth.signOut();

        Intent intent = new Intent(EmailVerificationActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}