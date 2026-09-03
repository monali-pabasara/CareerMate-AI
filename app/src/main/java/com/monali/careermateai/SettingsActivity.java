package com.monali.careermateai;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SettingsActivity extends AppCompatActivity {

    private TextView fullNameText, accountEmailText;
    private TextView editProfileCard, changePasswordCard, changeEmailCard;
    private TextView privacySecurityCard, aboutCard;
    private Button backToDashboardButton, logoutButton;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    private static final String PREF_NAME = "CareerMateUser";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        fullNameText = findViewById(R.id.fullNameText);
        accountEmailText = findViewById(R.id.accountEmailText);

        editProfileCard = findViewById(R.id.editProfileCard);
        changePasswordCard = findViewById(R.id.changePasswordCard);
        changeEmailCard = findViewById(R.id.changeEmailCard);
        privacySecurityCard = findViewById(R.id.privacySecurityCard);
        aboutCard = findViewById(R.id.aboutCard);

        backToDashboardButton = findViewById(R.id.backToDashboardButton);
        logoutButton = findViewById(R.id.logoutButton);

        loadUserProfileHeader();

        editProfileCard.setOnClickListener(view -> openEditProfile());
        changePasswordCard.setOnClickListener(view -> sendPasswordResetEmail());
        changeEmailCard.setOnClickListener(view -> showChangeEmailFutureMessage());
        privacySecurityCard.setOnClickListener(view -> openPrivacySecurity());
        aboutCard.setOnClickListener(view -> openAbout());

        backToDashboardButton.setOnClickListener(view -> goToDashboard());
        logoutButton.setOnClickListener(view -> logoutUser());
    }

    private void loadUserProfileHeader() {
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        String savedName = preferences.getString("fullName", "Student User");
        fullNameText.setText(savedName);

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            goToLoginWithoutToast();
            return;
        }

        String email = currentUser.getEmail();

        if (email == null || email.trim().isEmpty()) {
            accountEmailText.setText("Email not available");
        } else {
            accountEmailText.setText(email);
        }

        firestore.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.contains("fullName")) {
                        Object value = documentSnapshot.get("fullName");

                        if (value != null && !value.toString().trim().isEmpty()) {
                            String firestoreName = value.toString().trim();
                            fullNameText.setText(firestoreName);

                            preferences.edit()
                                    .putString("fullName", firestoreName)
                                    .apply();
                        }
                    }
                })
                .addOnFailureListener(e -> CustomToast.showInfo(
                        this,
                        "Using saved profile details"
                ));
    }

    private void openEditProfile() {
        Intent intent = new Intent(SettingsActivity.this, ProfileSetupActivity.class);
        intent.putExtra("editMode", true);
        startActivity(intent);
    }

    private void sendPasswordResetEmail() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            CustomToast.showError(this, "Please login again to reset your password.");
            goToLoginWithoutToast();
            return;
        }

        String email = currentUser.getEmail();

        if (email == null || email.trim().isEmpty()) {
            CustomToast.showError(this, "Email address not found.");
            return;
        }

        changePasswordCard.setEnabled(false);
        changePasswordCard.setText("Sending password reset email...");

        firebaseAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> {
                    changePasswordCard.setEnabled(true);
                    changePasswordCard.setText("🔐  Change Password");
                    CustomToast.showSuccess(this, "Password reset email sent.");
                })
                .addOnFailureListener(e -> {
                    changePasswordCard.setEnabled(true);
                    changePasswordCard.setText("🔐  Change Password");
                    CustomToast.showError(this, "Could not send password reset email.");
                });
    }

    private void showChangeEmailFutureMessage() {
        CustomToast.showInfo(
                this,
                "Change email will be available in a future version."
        );
    }

    private void openPrivacySecurity() {
        Intent intent = new Intent(SettingsActivity.this, PrivacySecurityActivity.class);
        startActivity(intent);
    }

    private void openAbout() {
        Intent intent = new Intent(SettingsActivity.this, AboutActivity.class);
        startActivity(intent);
    }

    private void logoutUser() {
        firebaseAuth.signOut();

        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        preferences.edit().clear().apply();

        CustomToast.showSuccess(this, "Logged out successfully");

        Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void goToDashboard() {
        Intent intent = new Intent(SettingsActivity.this, DashboardActivity.class);
        startActivity(intent);
        finish();
    }

    private void goToLoginWithoutToast() {
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        preferences.edit().clear().apply();

        Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}