package com.monali.careermateai;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
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

public class SkillGapActivity extends AppCompatActivity {

    private TextView targetRoleText, degreeProgrammeText, instructionText;
    private TextView skillMatchPercentText, skillMatchMessageText;
    private TextView strongSkillsText, weakSkillsText, priorityText, aiAdviceText;

    private TextView skillName1, skillName2, skillName3, skillName4, skillName5, skillName6;
    private Spinner skillLevelSpinner1, skillLevelSpinner2, skillLevelSpinner3;
    private Spinner skillLevelSpinner4, skillLevelSpinner5, skillLevelSpinner6;

    private LinearLayout skillRow1, skillRow2, skillRow3, skillRow4, skillRow5, skillRow6;

    private Button analyzeSkillGapButton, backToDashboardButton;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    private static final String PREF_NAME = "CareerMateUser";

    private String targetRole;
    private String degreeProgramme;
    private String selectedSkills;
    private String[] requiredSkills = {};

    private final String[] levelOptions = {
            "No experience",
            "Beginner",
            "Intermediate",
            "Confident"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_skill_gap);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        targetRoleText = findViewById(R.id.targetRoleText);
        degreeProgrammeText = findViewById(R.id.degreeProgrammeText);
        instructionText = findViewById(R.id.instructionText);

        skillMatchPercentText = findViewById(R.id.skillMatchPercentText);
        skillMatchMessageText = findViewById(R.id.skillMatchMessageText);
        strongSkillsText = findViewById(R.id.strongSkillsText);
        weakSkillsText = findViewById(R.id.weakSkillsText);
        priorityText = findViewById(R.id.priorityText);
        aiAdviceText = findViewById(R.id.aiAdviceText);

        skillName1 = findViewById(R.id.skillName1);
        skillName2 = findViewById(R.id.skillName2);
        skillName3 = findViewById(R.id.skillName3);
        skillName4 = findViewById(R.id.skillName4);
        skillName5 = findViewById(R.id.skillName5);
        skillName6 = findViewById(R.id.skillName6);

        skillLevelSpinner1 = findViewById(R.id.skillLevelSpinner1);
        skillLevelSpinner2 = findViewById(R.id.skillLevelSpinner2);
        skillLevelSpinner3 = findViewById(R.id.skillLevelSpinner3);
        skillLevelSpinner4 = findViewById(R.id.skillLevelSpinner4);
        skillLevelSpinner5 = findViewById(R.id.skillLevelSpinner5);
        skillLevelSpinner6 = findViewById(R.id.skillLevelSpinner6);

        skillRow1 = findViewById(R.id.skillRow1);
        skillRow2 = findViewById(R.id.skillRow2);
        skillRow3 = findViewById(R.id.skillRow3);
        skillRow4 = findViewById(R.id.skillRow4);
        skillRow5 = findViewById(R.id.skillRow5);
        skillRow6 = findViewById(R.id.skillRow6);

        analyzeSkillGapButton = findViewById(R.id.analyzeSkillGapButton);
        backToDashboardButton = findViewById(R.id.backToDashboardButton);

        loadProfileDataFromLocalCache();
        loadSkillGapFromFirestore();
        setupButtons();
    }

    private void loadProfileDataFromLocalCache() {
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        targetRole = preferences.getString("targetRole", "Not selected");
        degreeProgramme = preferences.getString("degreeProgramme", "Not selected");
        selectedSkills = preferences.getString("selectedSkills", "");

        targetRoleText.setText("Target Role: " + targetRole);
        degreeProgrammeText.setText("Degree Programme: " + degreeProgramme);

        requiredSkills = getRequiredSkillsForRole(targetRole);

        if (requiredSkills.length == 0) {
            instructionText.setText("Please complete your profile setup first to start skill gap analysis.");
            hideAllSkillRows();
            return;
        }

        instructionText.setText("Select your current confidence level for each required skill.");
        setupSkillRows(preferences);
        calculateSkillGap(false);
    }

    private void loadSkillGapFromFirestore() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            return;
        }

        firestore.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        cacheSkillGapFromFirestore(documentSnapshot);
                        loadProfileDataFromLocalCache();
                    }
                })
                .addOnFailureListener(e -> CustomToast.showInfo(
                        this,
                        "Using local skill gap cache"
                ));
    }

    private void cacheSkillGapFromFirestore(DocumentSnapshot documentSnapshot) {
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        if (documentSnapshot.contains("targetRole")) {
            editor.putString("targetRole", getStringValue(documentSnapshot, "targetRole"));
        }

        if (documentSnapshot.contains("degreeProgramme")) {
            editor.putString("degreeProgramme", getStringValue(documentSnapshot, "degreeProgramme"));
        }

        if (documentSnapshot.contains("selectedSkills")) {
            editor.putString("selectedSkills", getStringValue(documentSnapshot, "selectedSkills"));
        }

        if (documentSnapshot.contains("skillGapCompleted")) {
            Boolean completed = documentSnapshot.getBoolean("skillGapCompleted");
            editor.putBoolean("skillGapCompleted", completed != null && completed);
        }

        if (documentSnapshot.contains("skillMatch")) {
            editor.putInt("skillMatch", getIntValue(documentSnapshot, "skillMatch"));
        }

        if (documentSnapshot.contains("strongSkills")) {
            editor.putString("strongSkills", getStringValue(documentSnapshot, "strongSkills"));
        }

        if (documentSnapshot.contains("missingSkills")) {
            editor.putString("missingSkills", getStringValue(documentSnapshot, "missingSkills"));
        }

        if (documentSnapshot.contains("skillGapPriority")) {
            editor.putString("skillGapPriority", getStringValue(documentSnapshot, "skillGapPriority"));
        }

        if (documentSnapshot.contains("skillGapAdvice")) {
            editor.putString("skillGapAdvice", getStringValue(documentSnapshot, "skillGapAdvice"));
        }

        if (documentSnapshot.contains("careerReadiness")) {
            editor.putInt("careerReadiness", getIntValue(documentSnapshot, "careerReadiness"));
        }

        Object skillLevelsObject = documentSnapshot.get("skillLevels");

        if (skillLevelsObject instanceof Map) {
            Map<?, ?> skillLevels = (Map<?, ?>) skillLevelsObject;

            for (Object key : skillLevels.keySet()) {
                Object value = skillLevels.get(key);

                if (key != null && value != null) {
                    editor.putString("skillLevel_" + key.toString(), value.toString());
                }
            }
        }

        editor.apply();
    }

    private void setupSkillRows(SharedPreferences preferences) {
        TextView[] skillNames = {
                skillName1, skillName2, skillName3, skillName4, skillName5, skillName6
        };

        Spinner[] spinners = {
                skillLevelSpinner1, skillLevelSpinner2, skillLevelSpinner3,
                skillLevelSpinner4, skillLevelSpinner5, skillLevelSpinner6
        };

        LinearLayout[] rows = {
                skillRow1, skillRow2, skillRow3, skillRow4, skillRow5, skillRow6
        };

        for (int i = 0; i < rows.length; i++) {
            if (i < requiredSkills.length) {
                rows[i].setVisibility(View.VISIBLE);
                skillNames[i].setText(requiredSkills[i]);

                setupLevelSpinner(spinners[i]);

                String savedLevel = preferences.getString("skillLevel_" + makeSkillKey(requiredSkills[i]), "");

                if (!savedLevel.isEmpty()) {
                    setSpinnerSelection(spinners[i], savedLevel);
                } else if (selectedSkills.contains(requiredSkills[i])) {
                    setSpinnerSelection(spinners[i], "Beginner");
                } else {
                    setSpinnerSelection(spinners[i], "No experience");
                }

            } else {
                rows[i].setVisibility(View.GONE);
            }
        }
    }

    private void setupLevelSpinner(Spinner spinner) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                levelOptions
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void setSpinnerSelection(Spinner spinner, String value) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equals(value)) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private void calculateSkillGap(boolean saveResult) {
        if (requiredSkills.length == 0) {
            return;
        }

        Spinner[] spinners = {
                skillLevelSpinner1, skillLevelSpinner2, skillLevelSpinner3,
                skillLevelSpinner4, skillLevelSpinner5, skillLevelSpinner6
        };

        StringBuilder strongSkills = new StringBuilder();
        StringBuilder weakSkills = new StringBuilder();

        int totalPoints = 0;
        int totalSkills = requiredSkills.length;

        for (int i = 0; i < totalSkills; i++) {
            String skill = requiredSkills[i];
            String level = spinners[i].getSelectedItem().toString();

            int points = getPointsForLevel(level);
            totalPoints += points;

            if (points >= 65) {
                strongSkills.append("✓ ").append(skill).append(" - ").append(level).append("\n");
            } else {
                weakSkills.append("• ").append(skill).append(" - ").append(level).append("\n");
            }
        }

        int skillMatch = totalPoints / totalSkills;

        skillMatchPercentText.setText("Skill Match: " + skillMatch + "%");
        skillMatchMessageText.setText(getSkillMatchMessage(skillMatch));

        if (strongSkills.length() == 0) {
            strongSkillsText.setText("No strong skills yet. Start improving your confidence level step by step.");
        } else {
            strongSkillsText.setText(strongSkills.toString().trim());
        }

        if (weakSkills.length() == 0) {
            weakSkillsText.setText("No weak skills based on your current confidence levels.");
        } else {
            weakSkillsText.setText(weakSkills.toString().trim());
        }

        String prioritySkill = getPrioritySkill(spinners);
        String priorityMessage = getPriorityMessage(prioritySkill);
        String careerAdvice = generateRuleBasedCareerAdvice(skillMatch, prioritySkill);

        priorityText.setText(priorityMessage);
        aiAdviceText.setText(careerAdvice);

        if (saveResult) {
            saveSkillGapResult(
                    skillMatch,
                    strongSkills.toString().trim(),
                    weakSkills.toString().trim(),
                    prioritySkill,
                    careerAdvice,
                    spinners
            );
        }
    }

    private int getPointsForLevel(String level) {
        if (level.equals("No experience")) {
            return 0;
        } else if (level.equals("Beginner")) {
            return 30;
        } else if (level.equals("Intermediate")) {
            return 65;
        } else if (level.equals("Confident")) {
            return 100;
        }

        return 0;
    }

    private String getPrioritySkill(Spinner[] spinners) {
        for (int i = 0; i < requiredSkills.length; i++) {
            String level = spinners[i].getSelectedItem().toString();

            if (level.equals("No experience")) {
                return requiredSkills[i];
            }
        }

        for (int i = 0; i < requiredSkills.length; i++) {
            String level = spinners[i].getSelectedItem().toString();

            if (level.equals("Beginner")) {
                return requiredSkills[i];
            }
        }

        return "Portfolio proof and interview practice";
    }

    private String getPriorityMessage(String prioritySkill) {
        if (prioritySkill.equals("Portfolio proof and interview practice")) {
            return "You have a strong skill base. Next, prove your skills through projects, CV improvement and interview practice.";
        }

        return "Start improving " + prioritySkill + " first because it is important for your target role: " + targetRole + ".";
    }

    private String getSkillMatchMessage(int skillMatch) {
        if (skillMatch < 40) {
            return "You are still at an early stage. Focus on the weakest skills first.";
        } else if (skillMatch < 70) {
            return "You are developing well, but you still need to improve some important skills.";
        } else if (skillMatch < 90) {
            return "Good progress. You are close to a strong skill match.";
        } else {
            return "Excellent skill confidence. Now focus on proving these skills with portfolio evidence.";
        }
    }

    private String generateRuleBasedCareerAdvice(int skillMatch, String prioritySkill) {
        if (skillMatch < 40) {
            return "Career Advice: You are at the beginning stage for the " + targetRole + " role. Start with " + prioritySkill + " and practise with one simple task or small project.";
        } else if (skillMatch < 70) {
            return "Career Advice: You are making progress toward the " + targetRole + " role. Focus on " + prioritySkill + " next, then update your CV and portfolio with evidence.";
        } else if (skillMatch < 90) {
            return "Career Advice: You are close to your target role. Improve " + prioritySkill + " and prepare examples that show your skills clearly.";
        } else {
            return "Career Advice: Your confidence level is strong. Now focus on building portfolio proof, improving your CV, and practising interview answers.";
        }
    }

    private void saveSkillGapResult(
            int skillMatch,
            String strongSkills,
            String weakSkills,
            String prioritySkill,
            String careerAdvice,
            Spinner[] spinners
    ) {
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            CustomToast.showError(this, "Please login again before saving skill gap");
            return;
        }

        SharedPreferences.Editor editor = preferences.edit();

        editor.putInt("skillMatch", skillMatch);
        editor.putString("strongSkills", strongSkills);
        editor.putString("missingSkills", weakSkills);
        editor.putString("skillGapPriority", prioritySkill);
        editor.putString("skillGapAdvice", careerAdvice);
        editor.putBoolean("skillGapCompleted", true);

        Map<String, Object> skillLevels = new HashMap<>();

        for (int i = 0; i < requiredSkills.length; i++) {
            String skillKey = makeSkillKey(requiredSkills[i]);
            String level = spinners[i].getSelectedItem().toString();

            editor.putString("skillLevel_" + skillKey, level);
            skillLevels.put(skillKey, level);
        }

        int profileCompletion = preferences.getInt("profileCompletion", 0);
        int portfolioProgress = preferences.getInt("portfolioProgress", 0);
        int cvReadiness = preferences.getInt("cvReadiness", 0);
        int interviewReadiness = preferences.getInt("interviewReadiness", 0);

        int careerReadiness = calculateCareerReadiness(
                profileCompletion,
                skillMatch,
                portfolioProgress,
                cvReadiness,
                interviewReadiness
        );

        editor.putInt("careerReadiness", careerReadiness);
        editor.apply();

        Map<String, Object> skillGapData = new HashMap<>();

        skillGapData.put("skillGapCompleted", true);
        skillGapData.put("skillMatch", skillMatch);
        skillGapData.put("strongSkills", strongSkills);
        skillGapData.put("missingSkills", weakSkills);
        skillGapData.put("skillGapPriority", prioritySkill);
        skillGapData.put("skillGapAdvice", careerAdvice);
        skillGapData.put("skillLevels", skillLevels);
        skillGapData.put("careerReadiness", careerReadiness);
        skillGapData.put("targetRole", targetRole);
        skillGapData.put("degreeProgramme", degreeProgramme);
        skillGapData.put("updatedAt", FieldValue.serverTimestamp());

        for (int i = 0; i < requiredSkills.length; i++) {
            String skillKey = makeSkillKey(requiredSkills[i]);
            String level = spinners[i].getSelectedItem().toString();
            skillGapData.put("skillLevel_" + skillKey, level);
        }

        analyzeSkillGapButton.setEnabled(false);
        analyzeSkillGapButton.setText("Saving...");

        firestore.collection("users")
                .document(currentUser.getUid())
                .set(skillGapData, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    analyzeSkillGapButton.setEnabled(true);
                    analyzeSkillGapButton.setText("Analyze Skill Gap");
                    CustomToast.showSuccess(this, "Skill gap saved successfully");
                })
                .addOnFailureListener(e -> {
                    analyzeSkillGapButton.setEnabled(true);
                    analyzeSkillGapButton.setText("Analyze Skill Gap");
                    CustomToast.showError(this, "Skill gap saved locally, but cloud save failed");
                });
    }

    private int calculateCareerReadiness(
            int profileCompletion,
            int skillMatch,
            int portfolioProgress,
            int cvReadiness,
            int interviewReadiness
    ) {
        double readiness =
                (profileCompletion * 0.15) +
                        (skillMatch * 0.30) +
                        (cvReadiness * 0.20) +
                        (portfolioProgress * 0.20) +
                        (interviewReadiness * 0.15);

        int score = (int) Math.round(readiness);

        return applyFreshGraduateScoreCaps(
                score,
                cvReadiness,
                portfolioProgress,
                interviewReadiness
        );
    }

    private int applyFreshGraduateScoreCaps(
            int score,
            int cvReadiness,
            int portfolioProgress,
            int interviewReadiness
    ) {
        if (portfolioProgress < 30 && cvReadiness == 0 && interviewReadiness == 0 && score > 45) {
            score = 45;
        } else if (portfolioProgress < 50 && cvReadiness < 50 && score > 55) {
            score = 55;
        } else if (cvReadiness == 0 && score > 65) {
            score = 65;
        } else if (portfolioProgress < 70 && score > 70) {
            score = 70;
        } else if (interviewReadiness == 0 && score > 75) {
            score = 75;
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

    private String makeSkillKey(String skill) {
        return skill.replace(" ", "_")
                .replace("/", "_")
                .replace("-", "_")
                .replace("__", "_");
    }

    private void setupButtons() {
        analyzeSkillGapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                calculateSkillGap(true);
            }
        });

        backToDashboardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SkillGapActivity.this, DashboardActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void hideAllSkillRows() {
        skillRow1.setVisibility(View.GONE);
        skillRow2.setVisibility(View.GONE);
        skillRow3.setVisibility(View.GONE);
        skillRow4.setVisibility(View.GONE);
        skillRow5.setVisibility(View.GONE);
        skillRow6.setVisibility(View.GONE);
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
}