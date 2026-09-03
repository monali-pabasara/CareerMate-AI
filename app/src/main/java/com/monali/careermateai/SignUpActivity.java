package com.monali.careermateai;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    private EditText fullNameInput, emailInput, passwordInput, confirmPasswordInput;
    private CheckBox privacyCheckBox;
    private Button createAccountButton;
    private TextView loginText;
    private ImageButton passwordEyeButton, confirmPasswordEyeButton;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    private static final String PREF_NAME = "CareerMateUser";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        fullNameInput = findViewById(R.id.fullNameInput);
        emailInput = findViewById(R.id.signUpEmailInput);
        passwordInput = findViewById(R.id.signUpPasswordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        privacyCheckBox = findViewById(R.id.privacyCheckBox);
        createAccountButton = findViewById(R.id.createAccountButton);
        loginText = findViewById(R.id.loginText);
        passwordEyeButton = findViewById(R.id.passwordEyeButton);
        confirmPasswordEyeButton = findViewById(R.id.confirmPasswordEyeButton);

        setupPasswordEyeIcons();

        createAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createAccount();
            }
        });

        loginText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goToLogin();
            }
        });
    }

    private void setupPasswordEyeIcons() {
        passwordEyeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isPasswordVisible = !isPasswordVisible;
                updatePasswordVisibility(passwordInput, passwordEyeButton, isPasswordVisible);
            }
        });

        confirmPasswordEyeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isConfirmPasswordVisible = !isConfirmPasswordVisible;
                updatePasswordVisibility(confirmPasswordInput, confirmPasswordEyeButton, isConfirmPasswordVisible);
            }
        });
    }

    private void updatePasswordVisibility(EditText input, ImageButton eyeButton, boolean isVisible) {
        if (isVisible) {
            input.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            eyeButton.setImageResource(R.drawable.ic_eye);
        } else {
            input.setTransformationMethod(PasswordTransformationMethod.getInstance());
            eyeButton.setImageResource(R.drawable.ic_eye_off);
        }

        input.setSelection(input.getText().length());
    }

    private void createAccount() {
        String fullName = fullNameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim().toLowerCase();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();

        if (fullName.isEmpty()) {
            fullNameInput.setError("Please enter your full name");
            fullNameInput.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            emailInput.setError("Please enter your email address");
            emailInput.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Please enter a valid email address");
            emailInput.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            passwordInput.setError("Please create a password");
            passwordInput.requestFocus();
            return;
        }

        if (!isStrongPassword(password)) {
            passwordInput.setError(getPasswordRulesMessage());
            passwordInput.requestFocus();

            CustomToast.showError(
                    this,
                    "Password must include uppercase, lowercase, number and special character."
            );

            return;
        }

        if (confirmPassword.isEmpty()) {
            confirmPasswordInput.setError("Please confirm your password");
            confirmPasswordInput.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordInput.setError("Passwords do not match");
            confirmPasswordInput.requestFocus();

            CustomToast.showError(this, "Passwords do not match.");
            return;
        }

        if (!privacyCheckBox.isChecked()) {
            CustomToast.showError(this, "Please agree to the privacy policy.");
            return;
        }

        createAccountButton.setEnabled(false);
        createAccountButton.setText("Creating Account...");

        createFirebaseAccount(fullName, email, password);
    }

    private void createFirebaseAccount(String fullName, String email, String password) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

                        if (firebaseUser != null) {
                            String userId = firebaseUser.getUid();
                            saveUserProfileToFirestore(userId, fullName, email);
                        } else {
                            resetButton();
                            CustomToast.showError(this, "Account created, but user data was not found.");
                        }

                    } else {
                        resetButton();

                        String errorMessage = "Account creation failed.";

                        if (task.getException() != null && task.getException().getMessage() != null) {
                            errorMessage = task.getException().getMessage();
                        }

                        CustomToast.showError(this, errorMessage);
                    }
                });
    }

    private void saveUserProfileToFirestore(String userId, String fullName, String email) {
        Map<String, Object> userData = new HashMap<>();

        userData.put("userId", userId);
        userData.put("fullName", fullName);
        userData.put("email", email);

        // Temporarily marked true for testing version because email verification is disabled.
        userData.put("emailVerified", true);

        userData.put("degreeProgramme", "");
        userData.put("academicYear", "");
        userData.put("targetRole", "");
        userData.put("selectedSkills", "");
        userData.put("careerGoal", "");
        userData.put("portfolioLink", "");

        userData.put("profileCompleted", false);
        userData.put("profileCompletion", 0);
        userData.put("claimedSkillCoverage", 0);
        userData.put("skillMatch", 0);
        userData.put("dashboardSkillEvidence", 0);

        userData.put("cvCompleted", false);
        userData.put("cvReadiness", 0);

        userData.put("portfolioStarted", false);
        userData.put("portfolioProgress", 0);
        userData.put("portfolioEvidenceScore", 0);

        userData.put("skillGapCompleted", false);
        userData.put("strongSkills", "");
        userData.put("missingSkills", "");

        userData.put("interviewCompleted", false);
        userData.put("interviewReadiness", 0);

        userData.put("jobMatchCompleted", false);
        userData.put("suggestedJobRole", "");
        userData.put("suggestedJobScore", 0);

        userData.put("careerReadiness", 0);
        userData.put("overallReadinessScore", 0);

        userData.put("createdAt", FieldValue.serverTimestamp());
        userData.put("updatedAt", FieldValue.serverTimestamp());

        firestore.collection("users")
                .document(userId)
                .set(userData)
                .addOnSuccessListener(unused -> {
                    saveUserLocally(userId, fullName, email);

                    CustomToast.showSuccess(this, "Account created successfully.");

                    Intent intent = new Intent(SignUpActivity.this, ProfileSetupActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    resetButton();
                    CustomToast.showError(this, "Failed to save profile data.");
                });
    }

    private void saveUserLocally(String userId, String fullName, String email) {
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        editor.clear();

        editor.putString("userId", userId);
        editor.putString("fullName", fullName);
        editor.putString("email", email);

        editor.putBoolean("accountCreated", true);

        // Temporarily true for testing version because verification step is disabled.
        editor.putBoolean("emailVerified", true);

        editor.putBoolean("profileCompleted", false);

        editor.putInt("profileCompletion", 0);
        editor.putInt("claimedSkillCoverage", 0);
        editor.putInt("skillMatch", 0);
        editor.putInt("dashboardSkillEvidence", 0);
        editor.putInt("cvReadiness", 0);
        editor.putInt("portfolioProgress", 0);
        editor.putInt("portfolioEvidenceScore", 0);
        editor.putInt("interviewReadiness", 0);
        editor.putInt("suggestedJobScore", 0);
        editor.putInt("careerReadiness", 0);

        editor.putBoolean("cvCompleted", false);
        editor.putBoolean("portfolioStarted", false);
        editor.putBoolean("skillGapCompleted", false);
        editor.putBoolean("interviewCompleted", false);
        editor.putBoolean("jobMatchCompleted", false);

        editor.putString("strongSkills", "");
        editor.putString("missingSkills", "");
        editor.putString("suggestedJobRole", "");

        editor.apply();
    }

    private void resetButton() {
        createAccountButton.setEnabled(true);
        createAccountButton.setText("Create Account");
    }

    private boolean isStrongPassword(String password) {
        boolean hasMinimumLength = password.length() >= 8;
        boolean hasUppercase = false;
        boolean hasLowercase = false;
        boolean hasNumber = false;
        boolean hasSpecialCharacter = false;

        for (int i = 0; i < password.length(); i++) {
            char character = password.charAt(i);

            if (Character.isUpperCase(character)) {
                hasUppercase = true;
            } else if (Character.isLowerCase(character)) {
                hasLowercase = true;
            } else if (Character.isDigit(character)) {
                hasNumber = true;
            } else {
                hasSpecialCharacter = true;
            }
        }

        return hasMinimumLength
                && hasUppercase
                && hasLowercase
                && hasNumber
                && hasSpecialCharacter;
    }

    private String getPasswordRulesMessage() {
        return "Password must have at least 8 characters, uppercase letter, lowercase letter, number and special character";
    }

    private void goToLogin() {
        Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}