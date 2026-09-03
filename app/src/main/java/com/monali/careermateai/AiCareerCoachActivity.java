package com.monali.careermateai;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.functions.FirebaseFunctions;

import java.util.HashMap;
import java.util.Map;

public class AiCareerCoachActivity extends AppCompatActivity {

    private TextView careerSummaryText, aiCareerPlanText;
    private Button generateAiPlanButton, backToDashboardButton;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private FirebaseFunctions functions;

    private static final String PREF_NAME = "CareerMateUser";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_career_coach);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        functions = FirebaseFunctions.getInstance();

        careerSummaryText = findViewById(R.id.careerSummaryText);
        aiCareerPlanText = findViewById(R.id.aiCareerPlanText);

        generateAiPlanButton = findViewById(R.id.generateAiPlanButton);
        backToDashboardButton = findViewById(R.id.backToDashboardButton);

        loadCareerSummaryFromCache();
        loadSavedAiPlanFromFirestore();

        generateAiPlanButton.setOnClickListener(view -> generateAiCareerPlan());
        backToDashboardButton.setOnClickListener(view -> goToDashboard());
    }

    private void loadCareerSummaryFromCache() {
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        String fullName = preferences.getString("fullName", "Student User");
        String degreeProgramme = preferences.getString("degreeProgramme", "Not selected");
        String targetRole = preferences.getString("targetRole", "Not selected");
        String selectedSkills = preferences.getString("selectedSkills", "Not added");
        String missingSkills = preferences.getString("missingSkills", "Not generated yet");
        String suggestedJobRole = preferences.getString("suggestedJobRole", "Not generated yet");

        int careerReadiness = preferences.getInt("careerReadiness", 0);
        int profileCompletion = preferences.getInt("profileCompletion", 0);
        int skillEvidence = preferences.getInt(
                "dashboardSkillEvidence",
                preferences.getInt("skillMatch", 0)
        );
        int cvReadiness = preferences.getInt("cvReadiness", 0);
        int portfolioProgress = preferences.getInt("portfolioProgress", 0);
        int professionalLinksScore = preferences.getInt("professionalLinksScore", 0);

        String summary =
                "Name: " + fullName + "\n" +
                        "Degree Programme: " + degreeProgramme + "\n" +
                        "Target Role: " + targetRole + "\n" +
                        "Selected Skills: " + cleanEmptyText(selectedSkills, "Not added") + "\n" +
                        "Skills to Improve: " + cleanEmptyText(missingSkills, "Not generated yet") + "\n\n" +
                        "Career Readiness: " + careerReadiness + "%\n" +
                        "Profile Completion: " + profileCompletion + "%\n" +
                        "Skill Evidence: " + skillEvidence + "%\n" +
                        "CV Readiness: " + cvReadiness + "%\n" +
                        "Portfolio Evidence: " + portfolioProgress + "%\n" +
                        "Professional Links: " + professionalLinksScore + "%\n" +
                        "Suggested Job Role: " + cleanEmptyText(suggestedJobRole, "Not generated yet");

        careerSummaryText.setText(summary);

        String savedAiPlan = preferences.getString("aiCareerPlan", "");

        if (savedAiPlan != null && !savedAiPlan.trim().isEmpty()) {
            aiCareerPlanText.setText(savedAiPlan);
        }
    }

    private void loadSavedAiPlanFromFirestore() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            goToLoginWithoutToast();
            return;
        }

        firestore.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.contains("aiCareerPlan")) {
                        Object value = documentSnapshot.get("aiCareerPlan");

                        if (value != null && !value.toString().trim().isEmpty()) {
                            String savedPlan = value.toString().trim();

                            aiCareerPlanText.setText(savedPlan);

                            SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
                            preferences.edit()
                                    .putString("aiCareerPlan", savedPlan)
                                    .putBoolean("aiCoachCompleted", true)
                                    .apply();
                        }
                    }
                })
                .addOnFailureListener(e -> CustomToast.showInfo(
                        this,
                        "Using saved AI coach result"
                ));
    }

    private void generateAiCareerPlan() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            CustomToast.showError(this, "Please login again to use AI Career Coach.");
            goToLoginWithoutToast();
            return;
        }

        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        String targetRole = preferences.getString("targetRole", "Not selected");

        if (targetRole == null || targetRole.trim().isEmpty() || targetRole.equals("Not selected")) {
            CustomToast.showError(this, "Please complete your profile before using AI Career Coach.");
            return;
        }

        Map<String, Object> careerData = buildSafeCareerSummary(preferences);

        generateAiPlanButton.setEnabled(false);
        generateAiPlanButton.setText("Generating AI plan...");
        aiCareerPlanText.setText("Please wait. AI Career Coach is preparing your personalised career plan...");

        functions
                .getHttpsCallable("generateCareerPlan")
                .call(careerData)
                .addOnSuccessListener(httpsCallableResult -> {
                    Object result = httpsCallableResult.getData();

                    String aiPlan = extractCareerPlan(result);

                    if (aiPlan.isEmpty()) {
                        aiCareerPlanText.setText("AI Career Coach could not generate a plan. Please try again.");
                        CustomToast.showError(this, "AI response was empty.");
                    } else {
                        aiCareerPlanText.setText(aiPlan);
                        saveAiPlan(aiPlan);
                        CustomToast.showSuccess(this, "AI Career Plan generated.");
                    }

                    generateAiPlanButton.setEnabled(true);
                    generateAiPlanButton.setText("Generate AI Career Plan");
                })
                .addOnFailureListener(e -> {
                    generateAiPlanButton.setEnabled(true);
                    generateAiPlanButton.setText("Generate AI Career Plan");

                    aiCareerPlanText.setText(
                            "AI Career Coach is ready in the app, but the backend AI function is not connected yet.\n\n" +
                                    "Next step: deploy the Firebase Cloud Function named generateCareerPlan."
                    );

                    CustomToast.showInfo(this, "AI backend setup is the next step.");
                });
    }

    private Map<String, Object> buildSafeCareerSummary(SharedPreferences preferences) {
        Map<String, Object> careerData = new HashMap<>();

        careerData.put("fullName", preferences.getString("fullName", "Student User"));
        careerData.put("degreeProgramme", preferences.getString("degreeProgramme", "Not selected"));
        careerData.put("targetRole", preferences.getString("targetRole", "Not selected"));
        careerData.put("selectedSkills", preferences.getString("selectedSkills", ""));
        careerData.put("missingSkills", preferences.getString("missingSkills", ""));
        careerData.put("strongSkills", preferences.getString("strongSkills", ""));

        careerData.put("careerReadiness", preferences.getInt("careerReadiness", 0));
        careerData.put("profileCompletion", preferences.getInt("profileCompletion", 0));
        careerData.put(
                "skillEvidence",
                preferences.getInt("dashboardSkillEvidence", preferences.getInt("skillMatch", 0))
        );
        careerData.put("cvReadiness", preferences.getInt("cvReadiness", 0));
        careerData.put("portfolioProgress", preferences.getInt("portfolioProgress", 0));
        careerData.put("professionalLinksScore", preferences.getInt("professionalLinksScore", 0));

        careerData.put("jobMatchCompleted", preferences.getBoolean("jobMatchCompleted", false));
        careerData.put("suggestedJobRole", preferences.getString("suggestedJobRole", ""));
        careerData.put("suggestedJobScore", preferences.getInt("suggestedJobScore", 0));

        careerData.put("aiFeature", "AI Career Coach");
        careerData.put("privacyMode", "Safe summary only. Full CV text is not sent from Android.");

        return careerData;
    }

    private String extractCareerPlan(Object result) {
        if (result == null) {
            return "";
        }

        if (result instanceof String) {
            return result.toString().trim();
        }

        if (result instanceof Map) {
            Map<?, ?> resultMap = (Map<?, ?>) result;

            Object careerPlan = resultMap.get("careerPlan");

            if (careerPlan != null) {
                return careerPlan.toString().trim();
            }

            Object message = resultMap.get("message");

            if (message != null) {
                return message.toString().trim();
            }
        }

        return "";
    }

    private void saveAiPlan(String aiPlan) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            return;
        }

        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        preferences.edit()
                .putString("aiCareerPlan", aiPlan)
                .putBoolean("aiCoachCompleted", true)
                .apply();

        Map<String, Object> data = new HashMap<>();
        data.put("aiCareerPlan", aiPlan);
        data.put("aiCoachCompleted", true);
        data.put("aiCoachFeatureName", "AI Career Coach");
        data.put("aiCoachPrivacyMode", "Safe summary only. Full CV text is not sent from Android.");
        data.put("aiCoachUpdatedAt", FieldValue.serverTimestamp());

        firestore.collection("users")
                .document(currentUser.getUid())
                .set(data, SetOptions.merge());
    }

    private String cleanEmptyText(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }

        return value.trim();
    }

    private void goToDashboard() {
        Intent intent = new Intent(AiCareerCoachActivity.this, DashboardActivity.class);
        startActivity(intent);
        finish();
    }

    private void goToLoginWithoutToast() {
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        preferences.edit().clear().apply();

        Intent intent = new Intent(AiCareerCoachActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}