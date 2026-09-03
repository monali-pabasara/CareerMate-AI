package com.monali.careermateai;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class DashboardActivity extends AppCompatActivity {

    private CareerProgressView readinessCircle;

    private TextView greetingText, degreeInfoText, targetInfoText;
    private TextView readinessMessageText, levelText;
    private TextView profileCompletionText, skillMatchText, cvReadinessText, portfolioProgressText;
    private TextView professionalLinksText, jobMatchText;
    private TextView nextStepTitleText, nextStepDescriptionText, missingSkillsText;

    private TextView profileFeature, skillGapFeature, portfolioFeature;
    private TextView cvFeature, interviewFeature, jobRecommendationFeature;
    private LinearLayout aiCareerCoachFeature;

    private Button uploadCvButton, addProjectButton, practiceButton;
    private ImageButton dashboardSettingsButton;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    private static final String PREF_NAME = "CareerMateUser";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        readinessCircle = findViewById(R.id.readinessCircle);

        greetingText = findViewById(R.id.greetingText);
        degreeInfoText = findViewById(R.id.degreeInfoText);
        targetInfoText = findViewById(R.id.targetInfoText);

        readinessMessageText = findViewById(R.id.readinessMessageText);
        levelText = findViewById(R.id.levelText);

        profileCompletionText = findViewById(R.id.profileCompletionText);
        skillMatchText = findViewById(R.id.skillMatchText);
        cvReadinessText = findViewById(R.id.cvReadinessText);
        portfolioProgressText = findViewById(R.id.portfolioProgressText);
        professionalLinksText = findViewById(R.id.professionalLinksText);
        jobMatchText = findViewById(R.id.jobMatchText);

        nextStepTitleText = findViewById(R.id.nextStepTitleText);
        nextStepDescriptionText = findViewById(R.id.nextStepDescriptionText);
        missingSkillsText = findViewById(R.id.missingSkillsText);

        profileFeature = findViewById(R.id.profileFeature);
        portfolioFeature = findViewById(R.id.portfolioFeature);
        cvFeature = findViewById(R.id.cvFeature);
        interviewFeature = findViewById(R.id.interviewFeature);
        skillGapFeature = findViewById(R.id.skillGapFeature);
        jobRecommendationFeature = findViewById(R.id.jobRecommendationFeature);
        aiCareerCoachFeature = findViewById(R.id.aiCareerCoachFeature);

        uploadCvButton = findViewById(R.id.uploadCvButton);
        addProjectButton = findViewById(R.id.addProjectButton);
        practiceButton = findViewById(R.id.practiceButton);
        dashboardSettingsButton = findViewById(R.id.dashboardSettingsButton);

        loadDashboardDataFromLocalCache();
        loadDashboardDataFromFirestore();
        setupPrototypeClicks();
    }

    @Override
    protected void onResume() {
        super.onResume();

        loadDashboardDataFromLocalCache();
        loadDashboardDataFromFirestore();
    }

    private void loadDashboardDataFromFirestore() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            goToLoginWithoutToast();
            return;
        }

        firestore.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        cacheDashboardDataFromFirestore(documentSnapshot);
                        loadDashboardDataFromLocalCache();
                    }
                })
                .addOnFailureListener(e -> CustomToast.showInfo(
                        this,
                        "Using local dashboard cache"
                ));
    }

    private void cacheDashboardDataFromFirestore(DocumentSnapshot documentSnapshot) {
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        if (documentSnapshot.contains("fullName")) {
            editor.putString("fullName", getStringValue(documentSnapshot, "fullName"));
        }

        if (documentSnapshot.contains("degreeProgramme")) {
            editor.putString("degreeProgramme", getStringValue(documentSnapshot, "degreeProgramme"));
        }

        if (documentSnapshot.contains("targetRole")) {
            editor.putString("targetRole", getStringValue(documentSnapshot, "targetRole"));
        }

        if (documentSnapshot.contains("selectedSkills")) {
            editor.putString("selectedSkills", getStringValue(documentSnapshot, "selectedSkills"));
        }

        if (documentSnapshot.contains("profileCompletion")) {
            editor.putInt("profileCompletion", getIntValue(documentSnapshot, "profileCompletion"));
        }

        if (documentSnapshot.contains("claimedSkillCoverage")) {
            editor.putInt("claimedSkillCoverage", getIntValue(documentSnapshot, "claimedSkillCoverage"));
        }

        if (documentSnapshot.contains("skillMatch")) {
            editor.putInt("skillMatch", getIntValue(documentSnapshot, "skillMatch"));
        }

        if (documentSnapshot.contains("skillGapCompleted")) {
            editor.putBoolean("skillGapCompleted", getBooleanValue(documentSnapshot, "skillGapCompleted"));
        }

        if (documentSnapshot.contains("strongSkills")) {
            editor.putString("strongSkills", getStringValue(documentSnapshot, "strongSkills"));
        }

        if (documentSnapshot.contains("missingSkills")) {
            editor.putString("missingSkills", getStringValue(documentSnapshot, "missingSkills"));
        }

        if (documentSnapshot.contains("cvCompleted")) {
            editor.putBoolean("cvCompleted", getBooleanValue(documentSnapshot, "cvCompleted"));
        }

        if (documentSnapshot.contains("cvReadiness")) {
            editor.putInt("cvReadiness", getIntValue(documentSnapshot, "cvReadiness"));
        }

        if (documentSnapshot.contains("portfolioStarted")) {
            editor.putBoolean("portfolioStarted", getBooleanValue(documentSnapshot, "portfolioStarted"));
        }

        if (documentSnapshot.contains("portfolioProgress")) {
            editor.putInt("portfolioProgress", getIntValue(documentSnapshot, "portfolioProgress"));
        }

        if (documentSnapshot.contains("portfolioEvidenceScore")) {
            editor.putInt("portfolioEvidenceScore", getIntValue(documentSnapshot, "portfolioEvidenceScore"));
        }

        if (documentSnapshot.contains("savedProjectsSummary")) {
            editor.putString("savedProjectsSummary", getStringValue(documentSnapshot, "savedProjectsSummary"));
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

        if (documentSnapshot.contains("interviewCompleted")) {
            editor.putBoolean("interviewCompleted", getBooleanValue(documentSnapshot, "interviewCompleted"));
        }

        editor.putInt("interviewReadiness", 0);
        editor.putBoolean("interviewPracticeOnly", true);

        if (documentSnapshot.contains("jobMatchCompleted")) {
            editor.putBoolean("jobMatchCompleted", getBooleanValue(documentSnapshot, "jobMatchCompleted"));
        }

        if (documentSnapshot.contains("suggestedJobRole")) {
            editor.putString("suggestedJobRole", getStringValue(documentSnapshot, "suggestedJobRole"));
        }

        if (documentSnapshot.contains("suggestedJobScore")) {
            editor.putInt("suggestedJobScore", getIntValue(documentSnapshot, "suggestedJobScore"));
        }

        if (documentSnapshot.contains("careerReadiness")) {
            editor.putInt("careerReadiness", getIntValue(documentSnapshot, "careerReadiness"));
        }

        if (documentSnapshot.contains("aiCareerPlan")) {
            editor.putString("aiCareerPlan", getStringValue(documentSnapshot, "aiCareerPlan"));
        }

        if (documentSnapshot.contains("aiCoachCompleted")) {
            editor.putBoolean("aiCoachCompleted", getBooleanValue(documentSnapshot, "aiCoachCompleted"));
        }

        editor.apply();
    }

    private void loadDashboardDataFromLocalCache() {
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        String fullName = preferences.getString("fullName", "Student User");
        String degreeProgramme = preferences.getString("degreeProgramme", "Not selected");
        String targetRole = preferences.getString("targetRole", "Not selected");
        String selectedSkills = preferences.getString("selectedSkills", "");

        String githubLink = preferences.getString("githubLink", "");
        String linkedinLink = preferences.getString("linkedinLink", "");
        String portfolioWebsite = preferences.getString("portfolioWebsite", "");

        int profileCompletion = preferences.getInt("profileCompletion", 0);
        int storedSkillMatch = preferences.getInt(
                "skillMatch",
                preferences.getInt("claimedSkillCoverage", 0)
        );
        int cvReadiness = preferences.getInt("cvReadiness", 0);
        int portfolioProgress = preferences.getInt("portfolioProgress", 0);

        boolean skillGapCompleted = preferences.getBoolean("skillGapCompleted", false);
        boolean jobMatchCompleted = preferences.getBoolean("jobMatchCompleted", false);

        String suggestedJobRole = preferences.getString("suggestedJobRole", "");
        int suggestedJobScore = preferences.getInt("suggestedJobScore", 0);

        int professionalLinksScore = calculateProfessionalLinksScore(
                preferences,
                githubLink,
                linkedinLink,
                portfolioWebsite
        );

        if (portfolioProgress == 0 && hasAnyPortfolioLink(githubLink, linkedinLink, portfolioWebsite)) {
            portfolioProgress = 20;
        }

        int realisticSkillMatch = calculateRealisticSkillMatch(
                storedSkillMatch,
                selectedSkills,
                targetRole,
                skillGapCompleted,
                cvReadiness,
                portfolioProgress
        );

        int careerReadiness = calculateEvidenceBasedCareerReadiness(
                profileCompletion,
                realisticSkillMatch,
                cvReadiness,
                portfolioProgress,
                professionalLinksScore
        );

        saveAdjustedDashboardScores(
                realisticSkillMatch,
                careerReadiness,
                portfolioProgress,
                professionalLinksScore,
                skillGapCompleted
        );

        String firstName = getFirstName(fullName);

        greetingText.setText("Hi, " + firstName + " 👋");

        degreeInfoText.setText("🎓 Degree Programme: " + degreeProgramme);
        targetInfoText.setText("💼 Target Role: " + targetRole);

        readinessCircle.setProgress(careerReadiness);

        readinessMessageText.setText(getReadinessMessage(careerReadiness));
        levelText.setText("Level: " + getReadinessLevel(careerReadiness));

        profileCompletionText.setText("Profile Completion: " + profileCompletion + "%");

        if (skillGapCompleted) {
            skillMatchText.setText("Assessed Skill Evidence: " + realisticSkillMatch + "%");
        } else {
            skillMatchText.setText("Claimed Skill Coverage: " + realisticSkillMatch + "%");
        }

        if (cvReadiness == 0) {
            cvReadinessText.setText("CV Readiness: Not started");
        } else {
            cvReadinessText.setText("CV Readiness: " + cvReadiness + "%");
        }

        if (portfolioProgress == 0) {
            portfolioProgressText.setText("Portfolio Evidence: Not started");
        } else {
            portfolioProgressText.setText("Portfolio Evidence: " + portfolioProgress + "%");
        }

        if (professionalLinksScore == 0) {
            professionalLinksText.setText("Professional Links: Not added");
        } else {
            professionalLinksText.setText("Professional Links: " + professionalLinksScore + "%");
        }

        if (jobMatchCompleted && !suggestedJobRole.isEmpty()) {
            jobMatchText.setText("Job Match: " + suggestedJobRole + " (" + suggestedJobScore + "%)");
        } else {
            jobMatchText.setText("Job Match: Not generated");
        }

        String savedMissingSkills = preferences.getString("missingSkills", "");
        String missingSkills;

        if (skillGapCompleted && !savedMissingSkills.isEmpty()) {
            missingSkills = "Skills to Improve:\n" + savedMissingSkills;
        } else {
            missingSkills = getMissingSkills(targetRole, selectedSkills);
        }

        missingSkills = addIndustryGaps(missingSkills, targetRole, portfolioProgress);
        missingSkillsText.setText(missingSkills);

        setRecommendedNextStep(
                profileCompletion,
                realisticSkillMatch,
                portfolioProgress,
                cvReadiness,
                professionalLinksScore,
                jobMatchCompleted
        );
    }

    private boolean hasAnyPortfolioLink(String githubLink, String linkedinLink, String portfolioWebsite) {
        return (githubLink != null && !githubLink.trim().isEmpty())
                || (linkedinLink != null && !linkedinLink.trim().isEmpty())
                || (portfolioWebsite != null && !portfolioWebsite.trim().isEmpty());
    }

    private int calculateProfessionalLinksScore(
            SharedPreferences preferences,
            String githubLink,
            String linkedinLink,
            String portfolioWebsite
    ) {
        int score = 0;

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

    private void saveAdjustedDashboardScores(
            int realisticSkillMatch,
            int careerReadiness,
            int portfolioProgress,
            int professionalLinksScore,
            boolean skillGapCompleted
    ) {
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        if (!skillGapCompleted) {
            editor.putInt("skillMatch", realisticSkillMatch);
        }

        editor.putInt("dashboardSkillEvidence", realisticSkillMatch);
        editor.putInt("careerReadiness", careerReadiness);
        editor.putInt("portfolioProgress", portfolioProgress);
        editor.putInt("professionalLinksScore", professionalLinksScore);
        editor.putInt("interviewReadiness", 0);
        editor.putBoolean("interviewPracticeOnly", true);
        editor.putBoolean("evidenceBasedScoring", true);
        editor.putString(
                "scoreExplanation",
                "Career readiness uses profile, skill evidence, CV readiness, portfolio evidence and professional links. Interview Practice is practice-only and does not add marks."
        );

        editor.apply();

        saveDashboardScoresToFirestore(realisticSkillMatch, careerReadiness, professionalLinksScore);
    }

    private void saveDashboardScoresToFirestore(
            int realisticSkillMatch,
            int careerReadiness,
            int professionalLinksScore
    ) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            return;
        }

        Map<String, Object> dashboardData = new HashMap<>();
        dashboardData.put("dashboardSkillEvidence", realisticSkillMatch);
        dashboardData.put("careerReadiness", careerReadiness);
        dashboardData.put("professionalLinksScore", professionalLinksScore);
        dashboardData.put("interviewReadiness", 0);
        dashboardData.put("interviewPracticeOnly", true);
        dashboardData.put("evidenceBasedScoring", true);
        dashboardData.put("dashboardFormula", "Profile 15%, Skill Evidence 25%, CV Readiness 25%, Portfolio Evidence 25%, Professional Links 10%");
        dashboardData.put(
                "scoreExplanation",
                "Career readiness uses profile, skill evidence, CV readiness, portfolio evidence and professional links. Interview Practice is practice-only and does not add marks."
        );
        dashboardData.put("dashboardUpdatedAt", FieldValue.serverTimestamp());

        firestore.collection("users")
                .document(currentUser.getUid())
                .set(dashboardData, SetOptions.merge());
    }

    private int calculateRealisticSkillMatch(
            int storedSkillMatch,
            String selectedSkills,
            String targetRole,
            boolean skillGapCompleted,
            int cvReadiness,
            int portfolioProgress
    ) {
        String[] requiredSkills = getRequiredSkillsForRole(targetRole);

        if (requiredSkills.length == 0 || selectedSkills == null || selectedSkills.trim().isEmpty()) {
            if (skillGapCompleted) {
                return boundScore(storedSkillMatch);
            }

            return 0;
        }

        int selectedCount = 0;

        for (String skill : requiredSkills) {
            if (containsSkill(selectedSkills, skill)) {
                selectedCount++;
            }
        }

        double claimedSkillCoverage = ((double) selectedCount / requiredSkills.length) * 40.0;

        int baseSkillScore;

        if (skillGapCompleted) {
            baseSkillScore = Math.max(storedSkillMatch, (int) Math.round(claimedSkillCoverage));
        } else {
            baseSkillScore = (int) Math.round(claimedSkillCoverage);
        }

        int evidenceCap;

        if (!skillGapCompleted) {
            evidenceCap = 40;
        } else if (portfolioProgress < 40 && cvReadiness == 0) {
            evidenceCap = 60;
        } else if (portfolioProgress < 70) {
            evidenceCap = 75;
        } else if (cvReadiness < 60) {
            evidenceCap = 80;
        } else {
            evidenceCap = 90;
        }

        if (baseSkillScore > evidenceCap) {
            baseSkillScore = evidenceCap;
        }

        return boundScore(baseSkillScore);
    }

    private boolean containsSkill(String selectedSkills, String requiredSkill) {
        if (selectedSkills == null || requiredSkill == null) {
            return false;
        }

        String selectedLower = selectedSkills.toLowerCase();
        String requiredLower = requiredSkill.toLowerCase();

        if (selectedLower.contains(requiredLower)) {
            return true;
        }

        if (requiredLower.equals("programming basics")) {
            return selectedLower.contains("java")
                    || selectedLower.contains("python")
                    || selectedLower.contains("programming");
        }

        if (requiredLower.equals("app development")) {
            return selectedLower.contains("android")
                    || selectedLower.contains("mobile")
                    || selectedLower.contains("app development");
        }

        if (requiredLower.equals("ui ux design")) {
            return selectedLower.contains("ui")
                    || selectedLower.contains("ux")
                    || selectedLower.contains("figma")
                    || selectedLower.contains("xml");
        }

        if (requiredLower.equals("github / online portfolio")) {
            return selectedLower.contains("github")
                    || selectedLower.contains("portfolio");
        }

        if (requiredLower.equals("sql / database")) {
            return selectedLower.contains("sql")
                    || selectedLower.contains("database")
                    || selectedLower.contains("firebase");
        }

        if (requiredLower.equals("excel / spreadsheet skills")) {
            return selectedLower.contains("excel")
                    || selectedLower.contains("spreadsheet");
        }

        if (requiredLower.equals("canva / design tools")) {
            return selectedLower.contains("canva")
                    || selectedLower.contains("design");
        }

        if (requiredLower.equals("figma / ui ux design")) {
            return selectedLower.contains("figma")
                    || selectedLower.contains("ui")
                    || selectedLower.contains("ux");
        }

        return false;
    }

    private int calculateEvidenceBasedCareerReadiness(
            int profileCompletion,
            int skillMatch,
            int cvReadiness,
            int portfolioProgress,
            int professionalLinksScore
    ) {
        double readiness =
                (profileCompletion * 0.15) +
                        (skillMatch * 0.25) +
                        (cvReadiness * 0.25) +
                        (portfolioProgress * 0.25) +
                        (professionalLinksScore * 0.10);

        int finalScore = (int) Math.round(readiness);

        return applyFreshGraduateScoreCaps(
                finalScore,
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

    private int boundScore(int score) {
        if (score < 0) {
            return 0;
        }

        if (score > 100) {
            return 100;
        }

        return score;
    }

    private String getFirstName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "Student";
        }

        String[] parts = fullName.trim().split(" ");
        return parts[0];
    }

    private String getReadinessLevel(int score) {
        if (score < 35) {
            return "Beginner";
        } else if (score < 60) {
            return "Developing";
        } else if (score < 75) {
            return "Evidence Building";
        } else {
            return "Career Ready";
        }
    }

    private String getReadinessMessage(int score) {
        if (score < 35) {
            return "Start building your career evidence";
        } else if (score < 60) {
            return "Good start, but more proof is needed";
        } else if (score < 75) {
            return "You are building stronger evidence";
        } else {
            return "Strong progress with supporting evidence";
        }
    }

    private void setRecommendedNextStep(
            int profileCompletion,
            int skillMatch,
            int portfolioProgress,
            int cvReadiness,
            int professionalLinksScore,
            boolean jobMatchCompleted
    ) {
        if (profileCompletion < 100) {
            nextStepTitleText.setText("Complete your profile");
            nextStepDescriptionText.setText("Add missing profile details first. Profile data helps CareerMate AI personalise your skill gap, portfolio, CV and job guidance.");

        } else if (portfolioProgress < 40) {
            nextStepTitleText.setText("Add portfolio evidence");
            nextStepDescriptionText.setText("Your selected skills are treated as claimed skills only. Add projects, GitHub, LinkedIn or portfolio evidence to unlock a more realistic readiness score.");

        } else if (cvReadiness == 0) {
            nextStepTitleText.setText("Upload your CV");
            nextStepDescriptionText.setText("Your readiness score is limited until your CV is reviewed. Upload your CV to check role keywords, structure and graduate job readiness.");

        } else if (professionalLinksScore < 30) {
            nextStepTitleText.setText("Add professional links");
            nextStepDescriptionText.setText("Add GitHub, LinkedIn, portfolio website or project links. These links help prove your skills with real career evidence.");

        } else if (cvReadiness < 55) {
            nextStepTitleText.setText("Improve your CV evidence");
            nextStepDescriptionText.setText("Your CV score is still low. Improve project details, measurable achievements, role keywords and professional wording.");

        } else if (skillMatch < 70) {
            nextStepTitleText.setText("Improve skill evidence");
            nextStepDescriptionText.setText("Open Skill Gap Analysis and focus on both visible skills and hidden industry gaps required for your target role.");

        } else if (!jobMatchCompleted) {
            nextStepTitleText.setText("Check your job match");
            nextStepDescriptionText.setText("Open Job Match to see which career roles are most suitable based on your skills, CV, portfolio and professional links.");

        } else {
            nextStepTitleText.setText("Continue improving evidence");
            nextStepDescriptionText.setText("Keep improving your CV, portfolio, professional links and missing skills before applying for graduate-level roles.");
        }
    }

    private String getMissingSkills(String targetRole, String selectedSkills) {
        String[] requiredSkills = getRequiredSkillsForRole(targetRole);

        if (requiredSkills.length == 0) {
            return "Missing Skills: Select a target role to calculate skill gaps.";
        }

        StringBuilder missing = new StringBuilder();
        int missingCount = 0;

        for (String skill : requiredSkills) {
            if (!containsSkill(selectedSkills, skill)) {
                missing.append("• ").append(skill).append("\n");
                missingCount++;
            }
        }

        if (missingCount == 0) {
            return "Visible Skill Gaps: No major gaps based on your selected skills.";
        }

        return "Visible Skill Gaps:\n" + missing.toString().trim();
    }

    private String addIndustryGaps(String currentText, String targetRole, int portfolioProgress) {
        String industryGaps = getHiddenIndustryGaps(targetRole, portfolioProgress);

        if (industryGaps.isEmpty()) {
            return currentText;
        }

        return currentText + "\n\nIndustry Gaps to Improve:\n" + industryGaps;
    }

    private String getHiddenIndustryGaps(String targetRole, int portfolioProgress) {
        StringBuilder gaps = new StringBuilder();

        if (targetRole.equals("Mobile App Developer")) {
            if (portfolioProgress < 40) {
                gaps.append("• GitHub or project evidence\n");
            }
            gaps.append("• API Integration\n");
            gaps.append("• Testing Basics\n");
            gaps.append("• App Security Basics\n");
            gaps.append("• App Deployment Knowledge");

        } else if (targetRole.equals("Software Developer")) {
            if (portfolioProgress < 40) {
                gaps.append("• GitHub or coding portfolio evidence\n");
            }
            gaps.append("• Unit Testing\n");
            gaps.append("• Database/API Integration\n");
            gaps.append("• Version Control Workflow\n");
            gaps.append("• Debugging and Code Review Practice");

        } else if (targetRole.equals("Data Analyst")) {
            if (portfolioProgress < 40) {
                gaps.append("• Data project or dashboard evidence\n");
            }
            gaps.append("• Data Cleaning\n");
            gaps.append("• Dashboard Visualisation\n");
            gaps.append("• SQL Practice\n");
            gaps.append("• Data Storytelling");

        } else if (targetRole.equals("Business Analyst")) {
            if (portfolioProgress < 40) {
                gaps.append("• Business case study or report evidence\n");
            }
            gaps.append("• Requirements Documentation\n");
            gaps.append("• Process Mapping\n");
            gaps.append("• Stakeholder Communication\n");
            gaps.append("• Problem Analysis");

        } else if (targetRole.equals("Marketing Executive")) {
            if (portfolioProgress < 40) {
                gaps.append("• Campaign or content portfolio evidence\n");
            }
            gaps.append("• Marketing Analytics\n");
            gaps.append("• Content Strategy\n");
            gaps.append("• Audience Research\n");
            gaps.append("• Campaign Performance Reporting");

        } else if (targetRole.equals("UI UX Designer")) {
            if (portfolioProgress < 40) {
                gaps.append("• Figma case study or design portfolio evidence\n");
            }
            gaps.append("• User Research\n");
            gaps.append("• Usability Testing\n");
            gaps.append("• Design Justification\n");
            gaps.append("• Accessibility Basics");

        } else if (targetRole.equals("Project Coordinator")) {
            if (portfolioProgress < 40) {
                gaps.append("• Project documentation evidence\n");
            }
            gaps.append("• Risk Management\n");
            gaps.append("• Task Tracking\n");
            gaps.append("• Meeting Documentation\n");
            gaps.append("• Team Coordination Evidence");

        } else if (targetRole.equals("General Graduate Role")) {
            if (portfolioProgress < 40) {
                gaps.append("• LinkedIn or portfolio evidence\n");
            }
            gaps.append("• Professional Communication\n");
            gaps.append("• Workplace Problem Solving\n");
            gaps.append("• Graduate Application Evidence");
        }

        return gaps.toString().trim();
    }

    private String[] getRequiredSkillsForRole(String targetRole) {
        if (targetRole.equals("Mobile App Developer")) {
            return new String[]{
                    "Programming Basics",
                    "App Development",
                    "UI UX Design",
                    "GitHub / Online Portfolio",
                    "Problem Solving",
                    "Communication"
            };

        } else if (targetRole.equals("Software Developer")) {
            return new String[]{
                    "Programming Basics",
                    "SQL / Database",
                    "GitHub / Online Portfolio",
                    "Problem Solving",
                    "Teamwork",
                    "Communication"
            };

        } else if (targetRole.equals("Data Analyst")) {
            return new String[]{
                    "Excel / Spreadsheet Skills",
                    "Data Analysis",
                    "SQL / Database",
                    "Report Writing",
                    "Problem Solving",
                    "Presentation Skills"
            };

        } else if (targetRole.equals("Business Analyst")) {
            return new String[]{
                    "Excel / Spreadsheet Skills",
                    "Data Analysis",
                    "Communication",
                    "Problem Solving",
                    "Report Writing",
                    "Presentation Skills"
            };

        } else if (targetRole.equals("Marketing Executive")) {
            return new String[]{
                    "Digital Marketing",
                    "Social Media Marketing",
                    "Canva / Design Tools",
                    "Communication",
                    "Presentation Skills",
                    "Report Writing"
            };

        } else if (targetRole.equals("UI UX Designer")) {
            return new String[]{
                    "Figma / UI UX Design",
                    "Canva / Design Tools",
                    "Web Design",
                    "Communication",
                    "Problem Solving",
                    "Presentation Skills"
            };

        } else if (targetRole.equals("Project Coordinator")) {
            return new String[]{
                    "Leadership",
                    "Communication",
                    "Teamwork",
                    "Problem Solving",
                    "Report Writing",
                    "Presentation Skills"
            };

        } else if (targetRole.equals("General Graduate Role")) {
            return new String[]{
                    "Communication",
                    "Teamwork",
                    "Problem Solving",
                    "Presentation Skills",
                    "Report Writing",
                    "Leadership"
            };
        }

        return new String[]{};
    }

    private void setupPrototypeClicks() {
        profileFeature.setOnClickListener(view -> {
            Intent intent = new Intent(DashboardActivity.this, ProfileSetupActivity.class);
            intent.putExtra("editMode", true);
            startActivity(intent);
        });

        portfolioFeature.setOnClickListener(view -> {
            Intent intent = new Intent(DashboardActivity.this, PortfolioActivity.class);
            startActivity(intent);
        });

        cvFeature.setOnClickListener(view -> {
            Intent intent = new Intent(DashboardActivity.this, CvFeedbackActivity.class);
            startActivity(intent);
        });

        interviewFeature.setOnClickListener(view -> {
            Intent intent = new Intent(DashboardActivity.this, InterviewPracticeActivity.class);
            startActivity(intent);
        });

        skillGapFeature.setOnClickListener(view -> {
            Intent intent = new Intent(DashboardActivity.this, SkillGapActivity.class);
            startActivity(intent);
        });

        jobRecommendationFeature.setOnClickListener(view -> {
            Intent intent = new Intent(DashboardActivity.this, JobRecommendationActivity.class);
            startActivity(intent);
        });

        aiCareerCoachFeature.setOnClickListener(view -> {
            Intent intent = new Intent(DashboardActivity.this, AiCareerCoachActivity.class);
            startActivity(intent);
        });

        uploadCvButton.setOnClickListener(view -> {
            Intent intent = new Intent(DashboardActivity.this, CvFeedbackActivity.class);
            startActivity(intent);
        });

        addProjectButton.setOnClickListener(view -> {
            Intent intent = new Intent(DashboardActivity.this, PortfolioActivity.class);
            startActivity(intent);
        });

        practiceButton.setOnClickListener(view -> {
            Intent intent = new Intent(DashboardActivity.this, InterviewPracticeActivity.class);
            startActivity(intent);
        });

        dashboardSettingsButton.setOnClickListener(view -> {
            Intent intent = new Intent(DashboardActivity.this, SettingsActivity.class);
            startActivity(intent);
        });
    }

    private void goToLoginWithoutToast() {
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        preferences.edit().clear().apply();

        Intent intent = new Intent(DashboardActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
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

    private boolean getBooleanValue(DocumentSnapshot document, String key) {
        Boolean value = document.getBoolean(key);
        return value != null && value;
    }
}