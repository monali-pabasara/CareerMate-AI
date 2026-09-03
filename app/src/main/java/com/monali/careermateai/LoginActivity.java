package com.monali.careermateai;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Patterns;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private EditText emailInput, passwordInput;
    private Button loginButton;
    private TextView forgotPasswordText, signUpText;
    private CheckBox showPasswordCheckBox;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    private static final String PREF_NAME = "CareerMateUser";

    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MILLIS = 5 * 60 * 1000; // 5 minutes

    private static final String KEY_FAILED_ATTEMPTS_PREFIX = "failedLoginAttempts_";
    private static final String KEY_LOCK_UNTIL_PREFIX = "loginLockedUntil_";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        forgotPasswordText = findViewById(R.id.forgotPasswordText);
        signUpText = findViewById(R.id.signUpText);
        showPasswordCheckBox = findViewById(R.id.showPasswordCheckBox);

        setupPasswordVisibility();
        setupEmailLockWatcher();
        resetLoginButton();

        loginButton.setOnClickListener(view -> validateLogin());

        forgotPasswordText.setOnClickListener(view -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });

        signUpText.setOnClickListener(view -> {
            Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
            startActivity(intent);
        });
    }

    private void setupPasswordVisibility() {
        showPasswordCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                passwordInput.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                passwordInput.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }

            passwordInput.setSelection(passwordInput.getText().length());
        });
    }

    private void setupEmailLockWatcher() {
        emailInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                // Not needed
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if ("Logging in...".contentEquals(loginButton.getText())) {
                    return;
                }

                updateLoginButtonForCurrentEmail();
            }
        });
    }

    private void validateLogin() {
        String email = emailInput.getText().toString().trim().toLowerCase();
        String password = passwordInput.getText().toString().trim();

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

        if (isLoginLocked(email)) {
            updateLoginButtonForLock(email);
            showLockMessage(email);
            return;
        }

        if (password.isEmpty()) {
            passwordInput.setError("Please enter your password");
            passwordInput.requestFocus();
            return;
        }

        if (password.length() < 6) {
            passwordInput.setError("Password must be at least 6 characters");
            passwordInput.requestFocus();
            return;
        }

        loginButton.setEnabled(false);
        loginButton.setText("Logging in...");

        loginWithFirebase(email, password);
    }

    private void loginWithFirebase(String email, String password) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        clearFailedLoginAttempts(email);

                        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

                        if (currentUser == null) {
                            resetLoginButton();
                            CustomToast.showError(this, "Login failed. Please try again.");
                            return;
                        }

                        // Email verification is temporarily disabled for testing version.
                        // Users can continue directly after successful login.
                        updateEmailVerifiedStatus(currentUser);

                    } else {
                        handleFailedLogin(email);
                    }
                });
    }

    private void checkEmailVerification(FirebaseUser currentUser) {
        currentUser.reload().addOnCompleteListener(task -> {
            FirebaseUser refreshedUser = firebaseAuth.getCurrentUser();

            if (refreshedUser == null) {
                resetLoginButton();
                CustomToast.showError(this, "Please login again.");
                return;
            }

            if (!refreshedUser.isEmailVerified()) {
                resetLoginButton();

                CustomToast.showInfo(this, "Please verify your email before continuing.");

                Intent intent = new Intent(LoginActivity.this, EmailVerificationActivity.class);
                startActivity(intent);
                return;
            }

            updateEmailVerifiedStatus(refreshedUser);
        });
    }

    private void updateEmailVerifiedStatus(FirebaseUser firebaseUser) {
        String userId = firebaseUser.getUid();

        firestore.collection("users")
                .document(userId)
                .update(
                        "emailVerified", true,
                        "updatedAt", FieldValue.serverTimestamp()
                )
                .addOnSuccessListener(unused -> loadUserDataAndGoNext(firebaseUser))
                .addOnFailureListener(e -> loadUserDataAndGoNext(firebaseUser));
    }

    private void loadUserDataAndGoNext(FirebaseUser firebaseUser) {
        String userId = firebaseUser.getUid();
        String email = firebaseUser.getEmail();

        firestore.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        saveFirestoreDataLocally(userId, email, documentSnapshot);
                        goToNextScreen(documentSnapshot);
                    } else {
                        createMissingUserDocument(firebaseUser);
                    }
                })
                .addOnFailureListener(e -> {
                    resetLoginButton();
                    CustomToast.showError(
                            this,
                            "Login successful, but profile data could not load."
                    );
                });
    }

    private void saveFirestoreDataLocally(String userId, String email, DocumentSnapshot documentSnapshot) {
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        // Clear old local user cache before saving current Firebase user data.
        editor.clear();

        String fullName = getStringValue(documentSnapshot, "fullName");
        String degreeProgramme = getStringValue(documentSnapshot, "degreeProgramme");
        String academicYear = getStringValue(documentSnapshot, "academicYear");
        String targetRole = getStringValue(documentSnapshot, "targetRole");
        String selectedSkills = getStringValue(documentSnapshot, "selectedSkills");
        String careerGoal = getStringValue(documentSnapshot, "careerGoal");
        String portfolioLink = getStringValue(documentSnapshot, "portfolioLink");

        boolean realProfileCompleted = isRealProfileCompleted(
                fullName,
                degreeProgramme,
                academicYear,
                targetRole
        );

        editor.putString("userId", userId);
        editor.putString("email", email == null ? "" : email);
        editor.putString("fullName", fullName);
        editor.putString("degreeProgramme", degreeProgramme);
        editor.putString("academicYear", academicYear);
        editor.putString("targetRole", targetRole);
        editor.putString("selectedSkills", selectedSkills);
        editor.putString("careerGoal", careerGoal);
        editor.putString("portfolioLink", portfolioLink);

        editor.putBoolean("accountCreated", true);
        editor.putBoolean("emailVerified", true);
        editor.putBoolean("profileCompleted", realProfileCompleted);

        if (realProfileCompleted) {
            int savedCareerReadiness = getIntValue(
                    documentSnapshot,
                    "careerReadiness",
                    getIntValue(documentSnapshot, "overallReadinessScore", 0)
            );

            editor.putInt("profileCompletion", getIntValue(documentSnapshot, "profileCompletion", 100));
            editor.putInt("claimedSkillCoverage", getIntValue(documentSnapshot, "claimedSkillCoverage", 0));
            editor.putInt("skillMatch", getIntValue(documentSnapshot, "skillMatch", 0));
            editor.putInt("dashboardSkillEvidence", getIntValue(documentSnapshot, "dashboardSkillEvidence", 0));
            editor.putInt("cvReadiness", getIntValue(documentSnapshot, "cvReadiness", 0));
            editor.putInt("portfolioProgress", getIntValue(documentSnapshot, "portfolioProgress", 0));
            editor.putInt("interviewReadiness", getIntValue(documentSnapshot, "interviewReadiness", 0));
            editor.putInt("suggestedJobScore", getIntValue(documentSnapshot, "suggestedJobScore", 0));
            editor.putInt("careerReadiness", savedCareerReadiness);

            editor.putBoolean("skillGapCompleted", getBooleanValue(documentSnapshot, "skillGapCompleted"));
            editor.putBoolean("cvCompleted", getBooleanValue(documentSnapshot, "cvCompleted"));
            editor.putBoolean("portfolioStarted", getBooleanValue(documentSnapshot, "portfolioStarted"));
            editor.putBoolean("interviewCompleted", getBooleanValue(documentSnapshot, "interviewCompleted"));
            editor.putBoolean("jobMatchCompleted", getBooleanValue(documentSnapshot, "jobMatchCompleted"));

            editor.putString("missingSkills", getStringValue(documentSnapshot, "missingSkills"));
            editor.putString("strongSkills", getStringValue(documentSnapshot, "strongSkills"));
            editor.putString("suggestedJobRole", getStringValue(documentSnapshot, "suggestedJobRole"));

        } else {
            editor.putInt("profileCompletion", 0);
            editor.putInt("claimedSkillCoverage", 0);
            editor.putInt("skillMatch", 0);
            editor.putInt("dashboardSkillEvidence", 0);
            editor.putInt("cvReadiness", 0);
            editor.putInt("portfolioProgress", 0);
            editor.putInt("interviewReadiness", 0);
            editor.putInt("suggestedJobScore", 0);
            editor.putInt("careerReadiness", 0);

            editor.putBoolean("skillGapCompleted", false);
            editor.putBoolean("cvCompleted", false);
            editor.putBoolean("portfolioStarted", false);
            editor.putBoolean("interviewCompleted", false);
            editor.putBoolean("jobMatchCompleted", false);

            editor.putString("missingSkills", "");
            editor.putString("strongSkills", "");
            editor.putString("suggestedJobRole", "");
        }

        editor.apply();
    }

    private void goToNextScreen(DocumentSnapshot documentSnapshot) {
        String fullName = getStringValue(documentSnapshot, "fullName");
        String degreeProgramme = getStringValue(documentSnapshot, "degreeProgramme");
        String academicYear = getStringValue(documentSnapshot, "academicYear");
        String targetRole = getStringValue(documentSnapshot, "targetRole");

        boolean realProfileCompleted = isRealProfileCompleted(
                fullName,
                degreeProgramme,
                academicYear,
                targetRole
        );

        CustomToast.showSuccess(this, "Login successful.");

        Intent intent;

        if (realProfileCompleted) {
            intent = new Intent(LoginActivity.this, DashboardActivity.class);
        } else {
            intent = new Intent(LoginActivity.this, ProfileSetupActivity.class);
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private boolean isRealProfileCompleted(
            String fullName,
            String degreeProgramme,
            String academicYear,
            String targetRole
    ) {
        return !fullName.trim().isEmpty()
                && !degreeProgramme.trim().isEmpty()
                && !academicYear.trim().isEmpty()
                && !targetRole.trim().isEmpty();
    }

    private void createMissingUserDocument(FirebaseUser firebaseUser) {
        String userId = firebaseUser.getUid();
        String email = firebaseUser.getEmail() == null ? "" : firebaseUser.getEmail();

        Map<String, Object> userData = new HashMap<>();

        userData.put("userId", userId);
        userData.put("fullName", "");
        userData.put("email", email);
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
        userData.put("cvReadiness", 0);
        userData.put("portfolioProgress", 0);
        userData.put("interviewReadiness", 0);
        userData.put("suggestedJobScore", 0);
        userData.put("careerReadiness", 0);
        userData.put("overallReadinessScore", 0);

        userData.put("skillGapCompleted", false);
        userData.put("cvCompleted", false);
        userData.put("portfolioStarted", false);
        userData.put("interviewCompleted", false);
        userData.put("jobMatchCompleted", false);

        userData.put("missingSkills", "");
        userData.put("strongSkills", "");
        userData.put("suggestedJobRole", "");

        userData.put("createdAt", FieldValue.serverTimestamp());
        userData.put("updatedAt", FieldValue.serverTimestamp());

        firestore.collection("users")
                .document(userId)
                .set(userData)
                .addOnSuccessListener(unused -> {
                    SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();

                    editor.clear();

                    editor.putString("userId", userId);
                    editor.putString("email", email);
                    editor.putString("fullName", "");
                    editor.putString("degreeProgramme", "");
                    editor.putString("academicYear", "");
                    editor.putString("targetRole", "");
                    editor.putString("selectedSkills", "");
                    editor.putString("careerGoal", "");
                    editor.putString("portfolioLink", "");

                    editor.putBoolean("accountCreated", true);
                    editor.putBoolean("emailVerified", true);
                    editor.putBoolean("profileCompleted", false);

                    editor.putInt("profileCompletion", 0);
                    editor.putInt("claimedSkillCoverage", 0);
                    editor.putInt("skillMatch", 0);
                    editor.putInt("dashboardSkillEvidence", 0);
                    editor.putInt("cvReadiness", 0);
                    editor.putInt("portfolioProgress", 0);
                    editor.putInt("interviewReadiness", 0);
                    editor.putInt("suggestedJobScore", 0);
                    editor.putInt("careerReadiness", 0);

                    editor.putBoolean("skillGapCompleted", false);
                    editor.putBoolean("cvCompleted", false);
                    editor.putBoolean("portfolioStarted", false);
                    editor.putBoolean("interviewCompleted", false);
                    editor.putBoolean("jobMatchCompleted", false);

                    editor.putString("missingSkills", "");
                    editor.putString("strongSkills", "");
                    editor.putString("suggestedJobRole", "");

                    editor.apply();

                    CustomToast.showSuccess(this, "Login successful.");

                    Intent intent = new Intent(LoginActivity.this, ProfileSetupActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    resetLoginButton();
                    CustomToast.showError(this, "Could not create profile data.");
                });
    }

    private void handleFailedLogin(String email) {
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        int failedAttempts = preferences.getInt(getFailedAttemptsKey(email), 0);
        failedAttempts++;

        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(getFailedAttemptsKey(email), failedAttempts);

        int remainingAttempts = MAX_LOGIN_ATTEMPTS - failedAttempts;

        if (failedAttempts >= MAX_LOGIN_ATTEMPTS) {
            long lockUntil = System.currentTimeMillis() + LOCK_DURATION_MILLIS;

            editor.putLong(getLockUntilKey(email), lockUntil);
            editor.apply();

            CustomToast.showError(
                    this,
                    "Too many failed attempts for this email. Try again in 5 minutes."
            );

            updateLoginButtonForLock(email);

        } else {
            editor.apply();

            resetLoginButton();

            CustomToast.showError(
                    this,
                    "Incorrect email or password. Remaining attempts for this email: " + remainingAttempts
            );
        }
    }

    private boolean isLoginLocked(String email) {
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        long lockUntil = preferences.getLong(getLockUntilKey(email), 0);

        if (lockUntil == 0) {
            return false;
        }

        if (System.currentTimeMillis() >= lockUntil) {
            clearFailedLoginAttempts(email);
            return false;
        }

        return true;
    }

    private void updateLoginButtonForCurrentEmail() {
        String email = emailInput.getText().toString().trim().toLowerCase();

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            resetLoginButton();
            return;
        }

        if (isLoginLocked(email)) {
            updateLoginButtonForLock(email);
        } else {
            resetLoginButton();
        }
    }

    private void updateLoginButtonForLock(String email) {
        loginButton.setEnabled(false);
        loginButton.setText("Login Locked");

        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        long lockUntil = preferences.getLong(getLockUntilKey(email), 0);

        long delay = lockUntil - System.currentTimeMillis();

        if (delay > 0) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                clearFailedLoginAttempts(email);
                updateLoginButtonForCurrentEmail();
            }, delay);
        }
    }

    private void showLockMessage(String email) {
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        long lockUntil = preferences.getLong(getLockUntilKey(email), 0);

        long remainingMillis = lockUntil - System.currentTimeMillis();
        long remainingSeconds = Math.max(remainingMillis / 1000, 1);

        CustomToast.showError(
                this,
                "This email is temporarily locked. Try again in " + remainingSeconds + " seconds."
        );
    }

    private void clearFailedLoginAttempts(String email) {
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        editor.remove(getFailedAttemptsKey(email));
        editor.remove(getLockUntilKey(email));

        editor.apply();
    }

    private String getFailedAttemptsKey(String email) {
        return KEY_FAILED_ATTEMPTS_PREFIX + getSafeEmailKey(email);
    }

    private String getLockUntilKey(String email) {
        return KEY_LOCK_UNTIL_PREFIX + getSafeEmailKey(email);
    }

    private String getSafeEmailKey(String email) {
        if (email == null) {
            return "unknown_email";
        }

        return email.trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "_");
    }

    private void resetLoginButton() {
        loginButton.setEnabled(true);
        loginButton.setText("Login");
    }

    private String getStringValue(DocumentSnapshot documentSnapshot, String key) {
        String value = documentSnapshot.getString(key);
        return value == null ? "" : value;
    }

    private int getIntValue(DocumentSnapshot documentSnapshot, String key, int defaultValue) {
        Long value = documentSnapshot.getLong(key);
        return value == null ? defaultValue : value.intValue();
    }

    private boolean getBooleanValue(DocumentSnapshot documentSnapshot, String key) {
        Boolean value = documentSnapshot.getBoolean(key);
        return value != null && value;
    }
}