package com.monali.careermateai;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText resetEmailInput;
    private Button sendResetButton;
    private TextView backToLoginText;

    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        firebaseAuth = FirebaseAuth.getInstance();

        resetEmailInput = findViewById(R.id.resetEmailInput);
        sendResetButton = findViewById(R.id.sendResetButton);
        backToLoginText = findViewById(R.id.backToLoginText);

        sendResetButton.setOnClickListener(view -> sendPasswordResetLink());

        backToLoginText.setOnClickListener(view -> goToLogin());
    }

    private void sendPasswordResetLink() {
        String email = resetEmailInput.getText().toString().trim().toLowerCase();

        if (email.isEmpty()) {
            resetEmailInput.setError("Please enter your email address");
            resetEmailInput.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            resetEmailInput.setError("Please enter a valid email address");
            resetEmailInput.requestFocus();
            return;
        }

        sendResetButton.setEnabled(false);
        sendResetButton.setText("Sending...");

        firebaseAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    sendResetButton.setEnabled(true);
                    sendResetButton.setText("Send Reset Link");

                    if (task.isSuccessful()) {
                        Toast.makeText(
                                this,
                                "If this email is registered, a password reset link has been sent.",
                                Toast.LENGTH_LONG
                        ).show();

                        goToLogin();

                    } else {
                        String errorMessage = "Could not send reset link. Please check the email and try again.";

                        if (task.getException() != null && task.getException().getMessage() != null) {
                            errorMessage = task.getException().getMessage();
                        }

                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void goToLogin() {
        Intent intent = new Intent(ForgotPasswordActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}