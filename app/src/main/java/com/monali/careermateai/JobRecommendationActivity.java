package com.monali.careermateai;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class JobRecommendationActivity extends AppCompatActivity {

    private TextView bestRoleText, bestRoleScoreText, recommendationSummaryText;
    private TextView alternativeRolesText, matchedEvidenceText, missingSkillsText, jobAdviceText;

    private Button refreshRecommendationButton, backToDashboardButton;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    private static final String PREF_NAME = "CareerMateUser";

    private static final String[] JOB_ROLES = {
            "Junior Software Developer",
            "Frontend Developer Trainee",
            "Backend Developer Trainee",
            "Full Stack Developer Trainee",
            "QA Tester",
            "Application Support Engineer",
            "Database Developer",
            "Junior Android Developer",
            "Mobile App Developer Trainee",
            "Firebase App Developer Trainee",
            "Junior Data Analyst",
            "BI Reporting Assistant",
            "Data Reporting Analyst",
            "Business Analyst Intern",
            "Junior Business Analyst",
            "UI UX Designer Trainee",
            "Junior Product Designer",
            "Project Coordinator Assistant",
            "Junior Project Coordinator",
            "Digital Marketing Assistant",
            "Social Media Marketing Assistant",
            "Graduate Technology Trainee"
    };

    private static class RoleResult {
        String roleName;
        int score;
        String matchedEvidence;
        String missingSkills;

        RoleResult(String roleName, int score, String matchedEvidence, String missingSkills) {
            this.roleName = roleName;
            this.score = score;
            this.matchedEvidence = matchedEvidence;
            this.missingSkills = missingSkills;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_job_recommendation);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        bestRoleText = findViewById(R.id.bestRoleText);
        bestRoleScoreText = findViewById(R.id.bestRoleScoreText);
        recommendationSummaryText = findViewById(R.id.recommendationSummaryText);

        alternativeRolesText = findViewById(R.id.alternativeRolesText);
        matchedEvidenceText = findViewById(R.id.matchedEvidenceText);
        missingSkillsText = findViewById(R.id.missingSkillsText);
        jobAdviceText = findViewById(R.id.jobAdviceText);

        refreshRecommendationButton = findViewById(R.id.refreshRecommendationButton);
        backToDashboardButton = findViewById(R.id.backToDashboardButton);

        loadSavedRecommendationFromLocalCache();
        generateJobRecommendation(false);
        loadRecommendationFromFirestore();

        refreshRecommendationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                generateJobRecommendation(true);
            }
        });

        backToDashboardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goToDashboard();
            }
        });
    }

    private void loadSavedRecommendationFromLocalCache() {
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        boolean jobMatchCompleted = preferences.getBoolean("jobMatchCompleted", false);

        if (!jobMatchCompleted) {
            bestRoleText.setText("Top Recommended Role\nNot generated yet");
            bestRoleScoreText.setText("Match Score: 0%");
            recommendationSummaryText.setText("Tap refresh to generate job role suggestions based on your profile, skills, CV and portfolio evidence.");
            alternativeRolesText.setText("Alternative roles will appear here.");
            matchedEvidenceText.setText("Matched evidence will appear here.");
            missingSkillsText.setText("Skills to improve will appear here.");
            jobAdviceText.setText("Career advice will appear here.");
            return;
        }

        String suggestedJobRole = preferences.getString("suggestedJobRole", "");
        int suggestedJobScore = preferences.getInt("suggestedJobScore", 0);
        String suggestedJobEvidence = preferences.getString("suggestedJobEvidence", "");
        String suggestedJobMissingSkills = preferences.getString("suggestedJobMissingSkills", "");
        String jobRecommendationSummary = preferences.getString("jobRecommendationSummary", "");
        String alternativeRoles = preferences.getString("alternativeRoles", "");
        String jobAdvice = preferences.getString("jobAdvice", "");

        bestRoleText.setText("Top Recommended Role\n" + suggestedJobRole);
        bestRoleScoreText.setText("Match Score: " + suggestedJobScore + "%");
        recommendationSummaryText.setText(jobRecommendationSummary);
        matchedEvidenceText.setText(suggestedJobEvidence);
        missingSkillsText.setText(suggestedJobMissingSkills);
        alternativeRolesText.setText(alternativeRoles);
        jobAdviceText.setText(jobAdvice);
    }

    private void loadRecommendationFromFirestore() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            return;
        }

        firestore.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        cacheRecommendationFromFirestore(documentSnapshot);
                        loadSavedRecommendationFromLocalCache();
                    }
                })
                .addOnFailureListener(e -> CustomToast.showInfo(
                        this,
                        "Using local job recommendation cache"
                ));
    }

    private void cacheRecommendationFromFirestore(DocumentSnapshot documentSnapshot) {
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        if (documentSnapshot.contains("degreeProgramme")) {
            editor.putString("degreeProgramme", getStringValue(documentSnapshot, "degreeProgramme"));
        }

        if (documentSnapshot.contains("targetRole")) {
            editor.putString("targetRole", getStringValue(documentSnapshot, "targetRole"));
        }

        if (documentSnapshot.contains("selectedSkills")) {
            editor.putString("selectedSkills", getStringValue(documentSnapshot, "selectedSkills"));
        }

        if (documentSnapshot.contains("missingSkills")) {
            editor.putString("missingSkills", getStringValue(documentSnapshot, "missingSkills"));
        }

        if (documentSnapshot.contains("cvReadiness")) {
            editor.putInt("cvReadiness", getIntValue(documentSnapshot, "cvReadiness"));
        }

        if (documentSnapshot.contains("skillMatch")) {
            editor.putInt("skillMatch", getIntValue(documentSnapshot, "skillMatch"));
        }

        if (documentSnapshot.contains("portfolioProgress")) {
            editor.putInt("portfolioProgress", getIntValue(documentSnapshot, "portfolioProgress"));
        }

        if (documentSnapshot.contains("portfolioEvidenceScore")) {
            editor.putInt("portfolioEvidenceScore", getIntValue(documentSnapshot, "portfolioEvidenceScore"));
        }

        if (documentSnapshot.contains("githubLink")) {
            editor.putString("githubLink", getStringValue(documentSnapshot, "githubLink"));
        }

        if (documentSnapshot.contains("linkedinLink")) {
            editor.putString("linkedinLink", getStringValue(documentSnapshot, "linkedinLink"));
        }

        if (documentSnapshot.contains("portfolioWebsite")) {
            editor.putString("portfolioWebsite", getStringValue(documentSnapshot, "portfolioWebsite"));
        }

        if (documentSnapshot.contains("professionalLinksScore")) {
            editor.putInt("professionalLinksScore", getIntValue(documentSnapshot, "professionalLinksScore"));
        }

        if (documentSnapshot.contains("jobMatchCompleted")) {
            Boolean completed = documentSnapshot.getBoolean("jobMatchCompleted");
            editor.putBoolean("jobMatchCompleted", completed != null && completed);
        }

        if (documentSnapshot.contains("suggestedJobRole")) {
            editor.putString("suggestedJobRole", getStringValue(documentSnapshot, "suggestedJobRole"));
        }

        if (documentSnapshot.contains("suggestedJobScore")) {
            editor.putInt("suggestedJobScore", getIntValue(documentSnapshot, "suggestedJobScore"));
        }

        if (documentSnapshot.contains("suggestedJobEvidence")) {
            editor.putString("suggestedJobEvidence", getStringValue(documentSnapshot, "suggestedJobEvidence"));
        }

        if (documentSnapshot.contains("suggestedJobMissingSkills")) {
            editor.putString("suggestedJobMissingSkills", getStringValue(documentSnapshot, "suggestedJobMissingSkills"));
        }

        if (documentSnapshot.contains("jobRecommendationSummary")) {
            editor.putString("jobRecommendationSummary", getStringValue(documentSnapshot, "jobRecommendationSummary"));
        }

        if (documentSnapshot.contains("alternativeRoles")) {
            editor.putString("alternativeRoles", getStringValue(documentSnapshot, "alternativeRoles"));
        }

        if (documentSnapshot.contains("jobAdvice")) {
            editor.putString("jobAdvice", getStringValue(documentSnapshot, "jobAdvice"));
        }

        if (documentSnapshot.contains("careerReadiness")) {
            editor.putInt("careerReadiness", getIntValue(documentSnapshot, "careerReadiness"));
        }

        editor.apply();
    }

    private void generateJobRecommendation(boolean showToast) {
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        String degreeProgramme = preferences.getString("degreeProgramme", "");
        String targetRole = preferences.getString("targetRole", "");
        String selectedSkills = preferences.getString("selectedSkills", "");
        String missingSkills = preferences.getString("missingSkills", "");
        String strongSkills = preferences.getString("strongSkills", "");

        String githubLink = preferences.getString("githubLink", "");
        String linkedinLink = preferences.getString("linkedinLink", "");
        String portfolioWebsite = preferences.getString("portfolioWebsite", "");
        String savedProjectsSummary = preferences.getString("savedProjectsSummary", "");
        String linkAnalysisSummary = preferences.getString("linkAnalysisSummary", "");

        String cvFeedback = preferences.getString("cvFeedback", "");
        String cvSuggestions = preferences.getString("cvSuggestions", "");
        String detectedCvSections = preferences.getString("detectedCvSections", "");

        String interviewStrengths = preferences.getString("interviewStrengths", "");
        String interviewImprovements = preferences.getString("interviewImprovements", "");
        String lastInterviewAnswer = preferences.getString("lastInterviewAnswer", "");

        int cvReadiness = preferences.getInt("cvReadiness", 0);
        int skillMatch = preferences.getInt(
                "skillMatch",
                preferences.getInt("claimedSkillCoverage", 0)
        );
        int portfolioProgress = preferences.getInt("portfolioProgress", 0);
        int portfolioEvidenceScore = preferences.getInt("portfolioEvidenceScore", 0);
        int professionalLinksScore = calculateProfessionalLinksScore(preferences);

        String projectEvidence = getAllProjectEvidence(preferences);

        String combinedEvidence = (
                degreeProgramme + " " +
                        targetRole + " " +
                        selectedSkills + " " +
                        missingSkills + " " +
                        strongSkills + " " +
                        projectEvidence + " " +
                        savedProjectsSummary + " " +
                        linkAnalysisSummary + " " +
                        githubLink + " " +
                        linkedinLink + " " +
                        portfolioWebsite + " " +
                        cvFeedback + " " +
                        cvSuggestions + " " +
                        detectedCvSections + " " +
                        interviewStrengths + " " +
                        interviewImprovements + " " +
                        lastInterviewAnswer
        ).toLowerCase();

        ArrayList<RoleResult> results = new ArrayList<>();

        for (String role : JOB_ROLES) {
            RoleResult result = calculateRoleScore(
                    role,
                    targetRole,
                    combinedEvidence,
                    cvReadiness,
                    skillMatch,
                    portfolioProgress,
                    portfolioEvidenceScore,
                    professionalLinksScore
            );

            results.add(result);
        }

        Collections.sort(results, new Comparator<RoleResult>() {
            @Override
            public int compare(RoleResult r1, RoleResult r2) {
                return r2.score - r1.score;
            }
        });

        RoleResult bestRole = results.get(0);

        String recommendedRoles = generateRecommendedRoles(results);
        String alternativeRoles = generateAlternativeRoles(results);
        String summary = generateSummary(bestRole.roleName, bestRole.score, targetRole);
        String advice = generateJobAdvice(bestRole.roleName, bestRole.score);

        bestRoleText.setText("Top Recommended Role\n" + bestRole.roleName);
        bestRoleScoreText.setText("Match Score: " + bestRole.score + "%");

        recommendationSummaryText.setText(summary);
        matchedEvidenceText.setText(bestRole.matchedEvidence);
        missingSkillsText.setText(bestRole.missingSkills);
        alternativeRolesText.setText(alternativeRoles);
        jobAdviceText.setText(advice);

        saveBestRecommendation(
                bestRole,
                recommendedRoles,
                summary,
                alternativeRoles,
                advice,
                targetRole,
                degreeProgramme,
                cvReadiness,
                skillMatch,
                portfolioProgress,
                professionalLinksScore,
                showToast
        );
    }

    private String getAllProjectEvidence(SharedPreferences preferences) {
        StringBuilder builder = new StringBuilder();

        for (int i = 1; i <= 5; i++) {
            builder.append(" ")
                    .append(preferences.getString("project" + i + "_title", ""))
                    .append(" ")
                    .append(preferences.getString("project" + i + "_type", ""))
                    .append(" ")
                    .append(preferences.getString("project" + i + "_description", ""))
                    .append(" ")
                    .append(preferences.getString("project" + i + "_problemSolved", ""))
                    .append(" ")
                    .append(preferences.getString("project" + i + "_tools", ""))
                    .append(" ")
                    .append(preferences.getString("project" + i + "_keyFeatures", ""))
                    .append(" ")
                    .append(preferences.getString("project" + i + "_role", ""))
                    .append(" ")
                    .append(preferences.getString("project" + i + "_link", ""))
                    .append(" ")
                    .append(preferences.getString("project" + i + "_learning", ""));
        }

        return builder.toString();
    }

    private int calculateProfessionalLinksScore(SharedPreferences preferences) {
        int score = 0;

        String githubLink = preferences.getString("githubLink", "");
        String linkedinLink = preferences.getString("linkedinLink", "");
        String portfolioWebsite = preferences.getString("portfolioWebsite", "");

        if (githubLink != null && !githubLink.trim().isEmpty()) {
            score += 35;
        }

        if (linkedinLink != null && !linkedinLink.trim().isEmpty()) {
            score += 30;
        }

        if (portfolioWebsite != null && !portfolioWebsite.trim().isEmpty()) {
            score += 20;
        }

        if (hasAnyProjectLinkInCache(preferences)) {
            score += 15;
        }

        return boundScore(score);
    }

    private boolean hasAnyProjectLinkInCache(SharedPreferences preferences) {
        for (int i = 1; i <= 5; i++) {
            String projectLink = preferences.getString("project" + i + "_link", "");

            if (projectLink != null && !projectLink.trim().isEmpty()) {
                return true;
            }
        }

        return false;
    }

    private RoleResult calculateRoleScore(
            String role,
            String targetRole,
            String evidenceText,
            int cvReadiness,
            int skillMatch,
            int portfolioProgress,
            int portfolioEvidenceScore,
            int professionalLinksScore
    ) {
        String[] keywords = getRoleKeywords(role);

        int matchedCount = 0;
        StringBuilder matched = new StringBuilder();
        StringBuilder missing = new StringBuilder();

        for (String keyword : keywords) {
            if (evidenceText.contains(keyword.toLowerCase())) {
                matchedCount++;
                matched.append("✓ ").append(keyword).append("\n");
            } else {
                missing.append("• ").append(keyword).append("\n");
            }
        }

        int score = 0;

        if (keywords.length > 0) {
            score += (matchedCount * 50) / keywords.length;
        }

        if (isRoleRelatedToTarget(role, targetRole)) {
            score += 15;
        }

        score += (cvReadiness * 10) / 100;
        score += (portfolioEvidenceScore * 10) / 100;
        score += (portfolioProgress * 5) / 100;
        score += (professionalLinksScore * 5) / 100;

        if (isRoleRelatedToTarget(role, targetRole)) {
            score += (skillMatch * 10) / 100;
        }

        if (isDeveloperRole(role) && evidenceText.contains("github")) {
            score += 5;
        }

        if (!isDeveloperRole(role) && evidenceText.contains("linkedin")) {
            score += 5;
        }

        score = boundScore(score);

        String matchedText;
        if (matched.length() == 0) {
            matchedText = "Matched Evidence:\nNo strong evidence found yet for this role.";
        } else {
            matchedText = "Matched Evidence:\n" + matched.toString().trim();
        }

        String missingText;
        if (missing.length() == 0) {
            missingText = "Skills to Improve:\nNo major missing keywords detected.";
        } else {
            missingText = "Skills to Improve:\n" + limitMissingSkills(missing.toString());
        }

        return new RoleResult(role, score, matchedText, missingText);
    }

    private String generateRecommendedRoles(ArrayList<RoleResult> results) {
        StringBuilder builder = new StringBuilder();

        builder.append("Recommended Job Roles\n");

        int limit = Math.min(results.size(), 3);

        for (int i = 0; i < limit; i++) {
            RoleResult result = results.get(i);

            builder.append(i + 1)
                    .append(". ")
                    .append(result.roleName)
                    .append(" — ")
                    .append(result.score)
                    .append("%");

            if (i < limit - 1) {
                builder.append("\n");
            }
        }

        return builder.toString().trim();
    }

    private String generateAlternativeRoles(ArrayList<RoleResult> results) {
        StringBuilder builder = new StringBuilder();

        int start = 1;
        int end = Math.min(results.size(), 6);

        if (results.size() <= start) {
            return "Add stronger CV, portfolio and skill evidence to receive more role suggestions.";
        }

        for (int i = start; i < end; i++) {
            RoleResult result = results.get(i);

            builder.append("• ")
                    .append(result.roleName)
                    .append(" — ")
                    .append(result.score)
                    .append("%");

            if (i < end - 1) {
                builder.append("\n");
            }
        }

        return builder.toString().trim();
    }

    private String generateSummary(String bestRole, int score, String selectedTargetRole) {
        String selectedRoleText = selectedTargetRole == null || selectedTargetRole.trim().isEmpty()
                ? "your selected target role"
                : selectedTargetRole;

        if (score < 40) {
            return "CareerMate AI found early evidence for " + bestRole + ". Your selected target role is " + selectedRoleText + ", but your current evidence is still developing. Add stronger skills, CV content, portfolio projects and professional links.";
        } else if (score < 65) {
            return "CareerMate AI suggests " + bestRole + " as a possible graduate-level direction. This may be a related sub-role of your selected target role. Improve missing skills and add stronger project evidence before applying.";
        } else if (score < 80) {
            return "CareerMate AI suggests " + bestRole + " as a good match. Your skills, CV and portfolio evidence are moving in the right direction for this type of role.";
        } else {
            return "CareerMate AI suggests " + bestRole + " as a strong match. Your current evidence supports this role well, but you should continue improving your portfolio and CV quality.";
        }
    }

    private String generateJobAdvice(String role, int score) {
        StringBuilder advice = new StringBuilder();
        String lowerRole = role.toLowerCase();

        advice.append("Career Advice:\n");

        if (lowerRole.contains("android") || lowerRole.contains("mobile")) {
            advice.append("Build Android projects, upload them to GitHub, explain your Java/XML/Firebase work clearly, and add measurable project outcomes to your CV.");

        } else if (lowerRole.contains("frontend")) {
            advice.append("Build small web interface projects, improve HTML, CSS, JavaScript and UI basics, and show your work through GitHub or a portfolio.");

        } else if (lowerRole.contains("backend")) {
            advice.append("Strengthen database, API, server-side logic and debugging evidence. Add projects that show how data is stored, processed and retrieved.");

        } else if (lowerRole.contains("full stack")) {
            advice.append("Build projects that include both user interface and database/backend logic. Explain your frontend, backend and testing contribution clearly.");

        } else if (lowerRole.contains("qa") || lowerRole.contains("tester")) {
            advice.append("Learn software testing basics, test cases, bug reports, debugging and quality assurance workflows. A bug tracking or testing project would support this role.");

        } else if (lowerRole.contains("application support")) {
            advice.append("Improve troubleshooting, communication, documentation, database basics and user-support evidence. Show examples where you solved technical issues.");

        } else if (lowerRole.contains("database")) {
            advice.append("Improve SQL, database design, data validation and reporting skills. Add a project that stores and retrieves structured data.");

        } else if (lowerRole.contains("data") || lowerRole.contains("bi") || lowerRole.contains("reporting")) {
            advice.append("Show SQL, Excel, Python, dashboards and reporting projects. Add charts, insights and measurable data outcomes.");

        } else if (lowerRole.contains("business analyst")) {
            advice.append("Show requirements analysis, stakeholder communication, process improvement, reports and presentation evidence.");

        } else if (lowerRole.contains("ui") || lowerRole.contains("designer") || lowerRole.contains("product")) {
            advice.append("Show Figma, wireframes, prototypes, user research, usability testing and a clear design portfolio.");

        } else if (lowerRole.contains("project coordinator")) {
            advice.append("Show planning, teamwork, communication, leadership, timelines, reporting and coordination evidence.");

        } else if (lowerRole.contains("marketing") || lowerRole.contains("social media")) {
            advice.append("Show campaign work, social media content, Canva/design work, analytics, branding and communication evidence.");

        } else {
            advice.append("Build stronger communication, teamwork, problem-solving, reporting and portfolio evidence for graduate-level roles.");
        }

        if (score < 50) {
            advice.append("\n\nPriority: Improve your weak areas before applying seriously.");
        } else if (score < 75) {
            advice.append("\n\nPriority: You can prepare for entry-level roles, but your CV and portfolio still need stronger proof.");
        } else {
            advice.append("\n\nPriority: Start preparing applications while continuing to improve your project evidence.");
        }

        return advice.toString();
    }

    private void saveBestRecommendation(
            RoleResult bestRole,
            String recommendedRoles,
            String summary,
            String alternativeRoles,
            String advice,
            String targetRole,
            String degreeProgramme,
            int cvReadiness,
            int skillMatch,
            int portfolioProgress,
            int professionalLinksScore,
            boolean showToast
    ) {
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        editor.putBoolean("jobMatchCompleted", true);
        editor.putString("recommendedRoles", recommendedRoles);
        editor.putString("suggestedJobRole", bestRole.roleName);
        editor.putInt("suggestedJobScore", bestRole.score);
        editor.putString("suggestedJobEvidence", bestRole.matchedEvidence);
        editor.putString("suggestedJobMissingSkills", bestRole.missingSkills);
        editor.putString("jobRecommendationSummary", summary);
        editor.putString("alternativeRoles", alternativeRoles);
        editor.putString("jobAdvice", advice);
        editor.putInt("professionalLinksScore", professionalLinksScore);

        int profileCompletion = preferences.getInt("profileCompletion", 0);

        int careerReadiness = calculateCareerReadiness(
                profileCompletion,
                skillMatch,
                portfolioProgress,
                cvReadiness,
                professionalLinksScore
        );

        editor.putInt("careerReadiness", careerReadiness);
        editor.apply();

        saveBestRecommendationToFirestore(
                bestRole,
                recommendedRoles,
                summary,
                alternativeRoles,
                advice,
                targetRole,
                degreeProgramme,
                careerReadiness,
                professionalLinksScore,
                showToast
        );
    }

    private void saveBestRecommendationToFirestore(
            RoleResult bestRole,
            String recommendedRoles,
            String summary,
            String alternativeRoles,
            String advice,
            String targetRole,
            String degreeProgramme,
            int careerReadiness,
            int professionalLinksScore,
            boolean showToast
    ) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            if (showToast) {
                CustomToast.showError(this, "Job recommendation saved locally. Please login again for cloud save.");
            }
            return;
        }

        Map<String, Object> jobData = new HashMap<>();

        jobData.put("jobMatchCompleted", true);
        jobData.put("recommendedRoles", recommendedRoles);
        jobData.put("suggestedJobRole", bestRole.roleName);
        jobData.put("suggestedJobScore", bestRole.score);
        jobData.put("suggestedJobEvidence", bestRole.matchedEvidence);
        jobData.put("suggestedJobMissingSkills", bestRole.missingSkills);
        jobData.put("jobRecommendationSummary", summary);
        jobData.put("alternativeRoles", alternativeRoles);
        jobData.put("jobAdvice", advice);

        jobData.put("targetRole", targetRole);
        jobData.put("degreeProgramme", degreeProgramme);
        jobData.put("professionalLinksScore", professionalLinksScore);
        jobData.put("careerReadiness", careerReadiness);
        jobData.put("updatedAt", FieldValue.serverTimestamp());

        refreshRecommendationButton.setEnabled(false);
        refreshRecommendationButton.setText("Saving...");

        firestore.collection("users")
                .document(currentUser.getUid())
                .set(jobData, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    refreshRecommendationButton.setEnabled(true);
                    refreshRecommendationButton.setText("Refresh Recommendation");

                    if (showToast) {
                        CustomToast.showSuccess(this, "Job recommendation saved successfully");
                    }
                })
                .addOnFailureListener(e -> {
                    refreshRecommendationButton.setEnabled(true);
                    refreshRecommendationButton.setText("Refresh Recommendation");

                    if (showToast) {
                        CustomToast.showError(this, "Job recommendation saved locally, but cloud save failed");
                    }
                });
    }

    private int calculateCareerReadiness(
            int profileCompletion,
            int skillMatch,
            int portfolioProgress,
            int cvReadiness,
            int professionalLinksScore
    ) {
        double readiness =
                (profileCompletion * 0.15) +
                        (skillMatch * 0.25) +
                        (cvReadiness * 0.25) +
                        (portfolioProgress * 0.25) +
                        (professionalLinksScore * 0.10);

        int score = (int) Math.round(readiness);

        return applyFreshGraduateScoreCaps(
                score,
                cvReadiness,
                portfolioProgress,
                professionalLinksScore
        );
    }

    private int applyFreshGraduateScoreCaps(
            int score,
            int cvReadiness,
            int portfolioProgress,
            int professionalLinksScore
    ) {
        if (portfolioProgress < 30 && cvReadiness == 0 && professionalLinksScore == 0 && score > 45) {
            score = 45;
        } else if (portfolioProgress < 50 && cvReadiness < 50 && score > 55) {
            score = 55;
        } else if (cvReadiness == 0 && score > 65) {
            score = 65;
        } else if (portfolioProgress < 70 && score > 75) {
            score = 75;
        } else if (professionalLinksScore == 0 && score > 80) {
            score = 80;
        }

        return boundScore(score);
    }

    private String limitMissingSkills(String missingText) {
        String[] lines = missingText.split("\n");
        StringBuilder builder = new StringBuilder();

        int limit = Math.min(lines.length, 6);

        for (int i = 0; i < limit; i++) {
            builder.append(lines[i]).append("\n");
        }

        return builder.toString().trim();
    }

    private boolean isRoleRelatedToTarget(String role, String targetRole) {
        if (role == null || targetRole == null) {
            return false;
        }

        String lowerRole = role.toLowerCase();
        String lowerTarget = targetRole.toLowerCase();

        if (lowerTarget.contains("mobile")) {
            return lowerRole.contains("android")
                    || lowerRole.contains("mobile")
                    || lowerRole.contains("software")
                    || lowerRole.contains("qa")
                    || lowerRole.contains("application support");
        }

        if (lowerTarget.contains("software")) {
            return lowerRole.contains("software")
                    || lowerRole.contains("frontend")
                    || lowerRole.contains("backend")
                    || lowerRole.contains("full stack")
                    || lowerRole.contains("qa")
                    || lowerRole.contains("application support")
                    || lowerRole.contains("database")
                    || lowerRole.contains("android");
        }

        if (lowerTarget.contains("data")) {
            return lowerRole.contains("data")
                    || lowerRole.contains("bi")
                    || lowerRole.contains("reporting")
                    || lowerRole.contains("business analyst");
        }

        if (lowerTarget.contains("business")) {
            return lowerRole.contains("business analyst")
                    || lowerRole.contains("project")
                    || lowerRole.contains("data reporting");
        }

        if (lowerTarget.contains("marketing")) {
            return lowerRole.contains("marketing")
                    || lowerRole.contains("social media");
        }

        if (lowerTarget.contains("ui") || lowerTarget.contains("ux")) {
            return lowerRole.contains("ui")
                    || lowerRole.contains("designer")
                    || lowerRole.contains("product")
                    || lowerRole.contains("frontend");
        }

        if (lowerTarget.contains("project")) {
            return lowerRole.contains("project")
                    || lowerRole.contains("business analyst")
                    || lowerRole.contains("coordinator");
        }

        if (lowerTarget.contains("general")) {
            return lowerRole.contains("graduate")
                    || lowerRole.contains("assistant")
                    || lowerRole.contains("trainee")
                    || lowerRole.contains("support");
        }

        return lowerRole.contains(lowerTarget);
    }

    private boolean isDeveloperRole(String role) {
        if (role == null) {
            return false;
        }

        String lowerRole = role.toLowerCase();

        return lowerRole.contains("developer")
                || lowerRole.contains("frontend")
                || lowerRole.contains("backend")
                || lowerRole.contains("full stack")
                || lowerRole.contains("android")
                || lowerRole.contains("mobile")
                || lowerRole.contains("qa tester")
                || lowerRole.contains("application support")
                || lowerRole.contains("database");
    }

    private String[] getRoleKeywords(String role) {
        if (role.equals("Junior Software Developer")) {
            return new String[]{
                    "java", "python", "sql", "database", "api",
                    "github", "debugging", "testing", "software",
                    "programming", "problem solving"
            };

        } else if (role.equals("Frontend Developer Trainee")) {
            return new String[]{
                    "html", "css", "javascript", "ui", "web",
                    "frontend", "responsive", "design", "github",
                    "testing"
            };

        } else if (role.equals("Backend Developer Trainee")) {
            return new String[]{
                    "java", "python", "sql", "database", "api",
                    "backend", "server", "firebase", "debugging",
                    "testing"
            };

        } else if (role.equals("Full Stack Developer Trainee")) {
            return new String[]{
                    "frontend", "backend", "html", "css", "javascript",
                    "java", "sql", "database", "api", "github"
            };

        } else if (role.equals("QA Tester")) {
            return new String[]{
                    "testing", "debugging", "bug", "test cases",
                    "quality", "software", "problem solving", "report",
                    "documentation"
            };

        } else if (role.equals("Application Support Engineer")) {
            return new String[]{
                    "support", "troubleshooting", "debugging", "database",
                    "sql", "communication", "documentation", "problem solving",
                    "users"
            };

        } else if (role.equals("Database Developer")) {
            return new String[]{
                    "sql", "database", "data", "query", "table",
                    "firebase", "mysql", "schema", "report", "validation"
            };

        } else if (role.equals("Junior Android Developer")) {
            return new String[]{
                    "android", "java", "kotlin", "xml", "firebase",
                    "mobile app", "android studio", "api", "ui", "debugging",
                    "testing", "github"
            };

        } else if (role.equals("Mobile App Developer Trainee")) {
            return new String[]{
                    "mobile app", "android", "java", "xml", "firebase",
                    "ui", "app development", "android studio", "testing",
                    "github"
            };

        } else if (role.equals("Firebase App Developer Trainee")) {
            return new String[]{
                    "firebase", "authentication", "firestore", "database",
                    "android", "java", "mobile app", "cloud", "security",
                    "testing"
            };

        } else if (role.equals("Junior Data Analyst")) {
            return new String[]{
                    "excel", "sql", "python", "data analysis", "dashboard",
                    "visualisation", "visualization", "report", "statistics",
                    "power bi", "tableau"
            };

        } else if (role.equals("BI Reporting Assistant")) {
            return new String[]{
                    "power bi", "tableau", "excel", "report", "dashboard",
                    "data", "visualisation", "visualization", "analysis",
                    "presentation"
            };

        } else if (role.equals("Data Reporting Analyst")) {
            return new String[]{
                    "data", "report", "excel", "sql", "dashboard",
                    "analysis", "presentation", "statistics", "insights",
                    "visualisation"
            };

        } else if (role.equals("Business Analyst Intern")) {
            return new String[]{
                    "business analysis", "requirements", "stakeholder",
                    "excel", "report", "process", "documentation",
                    "data analysis", "presentation", "communication"
            };

        } else if (role.equals("Junior Business Analyst")) {
            return new String[]{
                    "requirements", "stakeholder", "process", "documentation",
                    "business", "analysis", "excel", "communication",
                    "problem solving", "presentation"
            };

        } else if (role.equals("UI UX Designer Trainee")) {
            return new String[]{
                    "figma", "ui", "ux", "prototype", "wireframe",
                    "user research", "design", "usability", "canva",
                    "portfolio"
            };

        } else if (role.equals("Junior Product Designer")) {
            return new String[]{
                    "design", "prototype", "user research", "ui", "ux",
                    "figma", "wireframe", "usability", "portfolio",
                    "presentation"
            };

        } else if (role.equals("Project Coordinator Assistant")) {
            return new String[]{
                    "project", "coordination", "planning", "timeline",
                    "stakeholder", "communication", "teamwork", "report",
                    "leadership", "risk"
            };

        } else if (role.equals("Junior Project Coordinator")) {
            return new String[]{
                    "project", "planning", "coordination", "teamwork",
                    "communication", "timeline", "documentation", "report",
                    "risk", "leadership"
            };

        } else if (role.equals("Digital Marketing Assistant")) {
            return new String[]{
                    "marketing", "campaign", "social media", "content",
                    "seo", "digital marketing", "analytics", "branding",
                    "canva", "communication"
            };

        } else if (role.equals("Social Media Marketing Assistant")) {
            return new String[]{
                    "social media", "content", "canva", "campaign",
                    "marketing", "analytics", "branding", "communication",
                    "audience", "design"
            };
        }

        return new String[]{
                "communication", "teamwork", "problem solving",
                "project", "leadership", "report", "presentation",
                "skills", "experience", "graduate"
        };
    }

    private int boundScore(int score) {
        if (score < 0) {
            return 0;
        }

        if (score > 100) {
            return 100;
        }

        return score;
    }

    private String getStringValue(DocumentSnapshot document, String key) {
        Object value = document.get(key);
        return value == null ? "" : value.toString();
    }

    private int getIntValue(DocumentSnapshot document, String key) {
        Object value = document.get(key);

        if (value instanceof Number) {
            return ((Number) value).intValue();
        }

        if (value == null) {
            return 0;
        }

        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void goToDashboard() {
        Intent intent = new Intent(JobRecommendationActivity.this, DashboardActivity.class);
        startActivity(intent);
        finish();
    }
}