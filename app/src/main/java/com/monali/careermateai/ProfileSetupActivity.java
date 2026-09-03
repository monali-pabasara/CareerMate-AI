package com.monali.careermateai;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ProfileSetupActivity extends AppCompatActivity {

    private EditText fullNameInput, careerGoalInput;
    private Spinner degreeProgrammeSpinner, academicYearSpinner, targetRoleSpinner;
    private TextView skillsInstructionText;

    private CheckBox skillCheckBox1, skillCheckBox2, skillCheckBox3;
    private CheckBox skillCheckBox4, skillCheckBox5, skillCheckBox6;

    private Button saveProfileButton, logoutButton;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    private static final String PREF_NAME = "CareerMateUser";

    private String[] currentSkillOptions = {};
    private String pendingRestoredSkills = "";
    private String pendingRestoredRole = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_setup);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        fullNameInput = findViewById(R.id.profileFullNameInput);
        careerGoalInput = findViewById(R.id.careerGoalInput);

        degreeProgrammeSpinner = findViewById(R.id.degreeProgrammeSpinner);
        academicYearSpinner = findViewById(R.id.academicYearSpinner);
        targetRoleSpinner = findViewById(R.id.targetRoleSpinner);

        skillsInstructionText = findViewById(R.id.skillsInstructionText);

        skillCheckBox1 = findViewById(R.id.skillCheckBox1);
        skillCheckBox2 = findViewById(R.id.skillCheckBox2);
        skillCheckBox3 = findViewById(R.id.skillCheckBox3);
        skillCheckBox4 = findViewById(R.id.skillCheckBox4);
        skillCheckBox5 = findViewById(R.id.skillCheckBox5);
        skillCheckBox6 = findViewById(R.id.skillCheckBox6);

        saveProfileButton = findViewById(R.id.saveProfileButton);
        logoutButton = findViewById(R.id.logoutButton);

        setupSpinners();
        hideSkillCheckBoxes();

        targetRoleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedRole = targetRoleSpinner.getSelectedItem().toString();
                updateSkillOptions(selectedRole);

                // Spinner callbacks may run after the profile fields have been loaded.
                // Restore saved checks only after the correct role-specific options exist.
                if (selectedRole.equals(pendingRestoredRole)) {
                    restoreSelectedSkills(pendingRestoredSkills);
                    pendingRestoredRole = "";
                    pendingRestoredSkills = "";
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                hideSkillCheckBoxes();
            }
        });

        loadSavedProfileData();

        saveProfileButton.setOnClickListener(view -> saveProfile());

        if (logoutButton != null) {
            logoutButton.setOnClickListener(view -> logoutUser());
        }
    }

    private void setupSpinners() {
        String[] degreeProgrammes = {
                "Select degree programme",
                "BSc Computer Science",
                "BSc Information Technology",
                "BSc Software Engineering",
                "BSc Data Analytics",
                "BSc Business Management",
                "BSc Marketing",
                "BSc Accounting and Finance",
                "BSc Hospitality Management",
                "BA Graphic Design",
                "BSc Project Management",
                "Other University Programme"
        };

        String[] academicYears = {
                "Select academic year",
                "Year 1",
                "Year 2",
                "Final Year",
                "Graduate"
        };

        String[] careerRoles = {
                "Select target career role",
                "Mobile App Developer",
                "Software Developer",
                "Data Analyst",
                "Business Analyst",
                "Marketing Executive",
                "UI UX Designer",
                "Project Coordinator",
                "General Graduate Role"
        };

        degreeProgrammeSpinner.setAdapter(createHintSpinnerAdapter(degreeProgrammes));
        academicYearSpinner.setAdapter(createHintSpinnerAdapter(academicYears));
        targetRoleSpinner.setAdapter(createHintSpinnerAdapter(careerRoles));
    }

    private ArrayAdapter<String> createHintSpinnerAdapter(String[] items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                items
        ) {
            @Override
            public boolean isEnabled(int position) {
                return position != 0;
            }

            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                TextView textView = (TextView) super.getView(position, convertView, parent);

                textView.setTextSize(15);
                textView.setPadding(16, 0, 16, 0);

                if (position == 0) {
                    textView.setTextColor(Color.parseColor("#8FA5C0"));
                    textView.setTypeface(null, Typeface.NORMAL);
                } else {
                    textView.setTextColor(Color.parseColor("#1A1A2E"));
                    textView.setTypeface(null, Typeface.NORMAL);
                }

                return textView;
            }

            @Override
            public View getDropDownView(int position, View convertView, android.view.ViewGroup parent) {
                TextView textView = (TextView) super.getDropDownView(position, convertView, parent);

                textView.setTextSize(16);
                textView.setPadding(28, 22, 28, 22);

                if (position == 0) {
                    textView.setTextColor(Color.parseColor("#185FA5"));
                    textView.setTypeface(null, Typeface.BOLD);
                    textView.setBackgroundColor(Color.parseColor("#EEF5FF"));
                } else {
                    textView.setTextColor(Color.parseColor("#1A1A2E"));
                    textView.setTypeface(null, Typeface.NORMAL);
                    textView.setBackgroundColor(Color.WHITE);
                }

                return textView;
            }
        };

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    private void loadSavedProfileData() {
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        String savedName = preferences.getString("fullName", "");
        String savedDegreeProgramme = preferences.getString("degreeProgramme", "");
        String savedAcademicYear = preferences.getString("academicYear", "");
        String savedTargetRole = preferences.getString("targetRole", "");
        String savedCareerGoal = preferences.getString("careerGoal", "");
        String savedSelectedSkills = preferences.getString("selectedSkills", "");

        if (!savedName.isEmpty()) {
            fullNameInput.setText(savedName);
        }

        if (!savedCareerGoal.isEmpty()) {
            careerGoalInput.setText(savedCareerGoal);
        }

        if (!savedTargetRole.isEmpty() && !savedTargetRole.equals("Select target career role")) {
            pendingRestoredRole = savedTargetRole;
            pendingRestoredSkills = savedSelectedSkills;
        }

        setSpinnerSelection(degreeProgrammeSpinner, savedDegreeProgramme);
        setSpinnerSelection(academicYearSpinner, savedAcademicYear);
        setSpinnerSelection(targetRoleSpinner, savedTargetRole);

        boolean editMode = getIntent().getBooleanExtra("editMode", false);

        if (editMode) {
            saveProfileButton.setText("Update Profile");
        } else {
            saveProfileButton.setText("Save and Continue");
        }
    }

    private void setSpinnerSelection(Spinner spinner, String savedValue) {
        if (savedValue == null || savedValue.isEmpty()) {
            return;
        }

        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equals(savedValue)) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private void restoreSelectedSkills(String savedSelectedSkills) {
        if (savedSelectedSkills == null || savedSelectedSkills.isEmpty()) {
            return;
        }

        Set<String> savedSkillSet = new HashSet<>();
        for (String skill : savedSelectedSkills.split(",")) {
            String normalisedSkill = skill.trim();
            if (!normalisedSkill.isEmpty()) {
                savedSkillSet.add(normalisedSkill);
            }
        }

        if (skillCheckBox1.getVisibility() == View.VISIBLE &&
                savedSkillSet.contains(skillCheckBox1.getText().toString())) {
            skillCheckBox1.setChecked(true);
        }

        if (skillCheckBox2.getVisibility() == View.VISIBLE &&
                savedSkillSet.contains(skillCheckBox2.getText().toString())) {
            skillCheckBox2.setChecked(true);
        }

        if (skillCheckBox3.getVisibility() == View.VISIBLE &&
                savedSkillSet.contains(skillCheckBox3.getText().toString())) {
            skillCheckBox3.setChecked(true);
        }

        if (skillCheckBox4.getVisibility() == View.VISIBLE &&
                savedSkillSet.contains(skillCheckBox4.getText().toString())) {
            skillCheckBox4.setChecked(true);
        }

        if (skillCheckBox5.getVisibility() == View.VISIBLE &&
                savedSkillSet.contains(skillCheckBox5.getText().toString())) {
            skillCheckBox5.setChecked(true);
        }

        if (skillCheckBox6.getVisibility() == View.VISIBLE &&
                savedSkillSet.contains(skillCheckBox6.getText().toString())) {
            skillCheckBox6.setChecked(true);
        }
    }

    private void updateSkillOptions(String targetRole) {
        clearSkillSelections();

        if (targetRole.equals("Select target career role")) {
            currentSkillOptions = new String[]{};
            skillsInstructionText.setText("Select a target career role to view related skills.");
            hideSkillCheckBoxes();
            return;
        }

        skillsInstructionText.setText("Select the skills you already have for this career role. These are claimed skills only until supported by skill gap, CV, portfolio or interview evidence.");

        if (targetRole.equals("Mobile App Developer")) {
            currentSkillOptions = new String[]{
                    "Programming Basics",
                    "App Development",
                    "UI UX Design",
                    "GitHub / Online Portfolio",
                    "Problem Solving",
                    "Communication"
            };

        } else if (targetRole.equals("Software Developer")) {
            currentSkillOptions = new String[]{
                    "Programming Basics",
                    "SQL / Database",
                    "GitHub / Online Portfolio",
                    "Problem Solving",
                    "Teamwork",
                    "Communication"
            };

        } else if (targetRole.equals("Data Analyst")) {
            currentSkillOptions = new String[]{
                    "Excel / Spreadsheet Skills",
                    "Data Analysis",
                    "SQL / Database",
                    "Report Writing",
                    "Problem Solving",
                    "Presentation Skills"
            };

        } else if (targetRole.equals("Business Analyst")) {
            currentSkillOptions = new String[]{
                    "Excel / Spreadsheet Skills",
                    "Data Analysis",
                    "Communication",
                    "Problem Solving",
                    "Report Writing",
                    "Presentation Skills"
            };

        } else if (targetRole.equals("Marketing Executive")) {
            currentSkillOptions = new String[]{
                    "Digital Marketing",
                    "Social Media Marketing",
                    "Canva / Design Tools",
                    "Communication",
                    "Presentation Skills",
                    "Report Writing"
            };

        } else if (targetRole.equals("UI UX Designer")) {
            currentSkillOptions = new String[]{
                    "Figma / UI UX Design",
                    "Canva / Design Tools",
                    "Web Design",
                    "Communication",
                    "Problem Solving",
                    "Presentation Skills"
            };

        } else if (targetRole.equals("Project Coordinator")) {
            currentSkillOptions = new String[]{
                    "Leadership",
                    "Communication",
                    "Teamwork",
                    "Problem Solving",
                    "Report Writing",
                    "Presentation Skills"
            };

        } else {
            currentSkillOptions = new String[]{
                    "Communication",
                    "Teamwork",
                    "Problem Solving",
                    "Presentation Skills",
                    "Report Writing",
                    "Leadership"
            };
        }

        showSkillCheckBoxes();
    }

    private void showSkillCheckBoxes() {
        CheckBox[] checkBoxes = {
                skillCheckBox1,
                skillCheckBox2,
                skillCheckBox3,
                skillCheckBox4,
                skillCheckBox5,
                skillCheckBox6
        };

        for (int i = 0; i < checkBoxes.length; i++) {
            if (i < currentSkillOptions.length) {
                checkBoxes[i].setText(currentSkillOptions[i]);
                checkBoxes[i].setVisibility(View.VISIBLE);
            } else {
                checkBoxes[i].setVisibility(View.GONE);
            }
        }
    }

    private void hideSkillCheckBoxes() {
        skillCheckBox1.setVisibility(View.GONE);
        skillCheckBox2.setVisibility(View.GONE);
        skillCheckBox3.setVisibility(View.GONE);
        skillCheckBox4.setVisibility(View.GONE);
        skillCheckBox5.setVisibility(View.GONE);
        skillCheckBox6.setVisibility(View.GONE);
    }

    private void clearSkillSelections() {
        skillCheckBox1.setChecked(false);
        skillCheckBox2.setChecked(false);
        skillCheckBox3.setChecked(false);
        skillCheckBox4.setChecked(false);
        skillCheckBox5.setChecked(false);
        skillCheckBox6.setChecked(false);
    }

    private void saveProfile() {
        String fullName = fullNameInput.getText().toString().trim();
        String degreeProgramme = degreeProgrammeSpinner.getSelectedItem().toString();
        String academicYear = academicYearSpinner.getSelectedItem().toString();
        String targetRole = targetRoleSpinner.getSelectedItem().toString();
        String careerGoal = careerGoalInput.getText().toString().trim();
        String selectedSkills = getSelectedSkills();

        if (fullName.isEmpty()) {
            fullNameInput.setError("Please enter your full name");
            fullNameInput.requestFocus();
            return;
        }

        if (degreeProgramme.equals("Select degree programme")) {
            Toast.makeText(this, "Please select your degree programme", Toast.LENGTH_SHORT).show();
            return;
        }

        if (academicYear.equals("Select academic year")) {
            Toast.makeText(this, "Please select your academic year", Toast.LENGTH_SHORT).show();
            return;
        }

        if (targetRole.equals("Select target career role")) {
            Toast.makeText(this, "Please select your target career role", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedSkills.isEmpty()) {
            Toast.makeText(this, "Please select at least one current skill", Toast.LENGTH_SHORT).show();
            return;
        }

        if (careerGoal.isEmpty()) {
            careerGoalInput.setError("Please enter your career goal");
            careerGoalInput.requestFocus();
            return;
        }

        int profileCompletion = calculateProfileCompletion(
                fullName,
                degreeProgramme,
                academicYear,
                targetRole,
                selectedSkills,
                careerGoal
        );

        int claimedSkillCoverage = calculateSkillMatch();
        int careerReadiness = calculateEvidenceBasedCareerReadiness(
                profileCompletion,
                claimedSkillCoverage
        );

        saveProfileButton.setEnabled(false);
        saveProfileButton.setText("Saving Profile...");

        saveProfileToFirebase(
                fullName,
                degreeProgramme,
                academicYear,
                targetRole,
                selectedSkills,
                careerGoal,
                profileCompletion,
                claimedSkillCoverage,
                careerReadiness
        );
    }

    private void saveProfileToFirebase(
            String fullName,
            String degreeProgramme,
            String academicYear,
            String targetRole,
            String selectedSkills,
            String careerGoal,
            int profileCompletion,
            int claimedSkillCoverage,
            int careerReadiness
    ) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            resetSaveButton();
            Toast.makeText(this, "Please login again before saving your profile", Toast.LENGTH_LONG).show();

            Intent intent = new Intent(ProfileSetupActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            return;
        }

        String userId = currentUser.getUid();
        String email = currentUser.getEmail() == null ? "" : currentUser.getEmail();

        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        int currentCvReadiness = preferences.getInt("cvReadiness", 0);
        int currentPortfolioProgress = preferences.getInt("portfolioProgress", 0);
        int currentInterviewReadiness = preferences.getInt("interviewReadiness", 0);

        Map<String, Object> profileData = new HashMap<>();

        profileData.put("userId", userId);
        profileData.put("email", email);
        profileData.put("emailVerified", currentUser.isEmailVerified());

        profileData.put("fullName", fullName);
        profileData.put("degreeProgramme", degreeProgramme);
        profileData.put("academicYear", academicYear);
        profileData.put("targetRole", targetRole);
        profileData.put("selectedSkills", selectedSkills);
        profileData.put("careerGoal", careerGoal);

        // Professional links are now handled only inside Portfolio Builder.
        profileData.put("portfolioLink", "");

        profileData.put("profileCompleted", true);
        profileData.put("profileCompletion", profileCompletion);

        profileData.put("skillMatch", claimedSkillCoverage);
        profileData.put("claimedSkillCoverage", claimedSkillCoverage);

        profileData.put("cvReadiness", currentCvReadiness);
        profileData.put("portfolioProgress", currentPortfolioProgress);
        profileData.put("interviewReadiness", currentInterviewReadiness);
        profileData.put("overallReadinessScore", careerReadiness);

        profileData.put("scoringModel", "Evidence-based readiness scoring");
        profileData.put("scoreExplanation", "Profile Setup stores basic student information only. Professional links are handled in Portfolio Builder as portfolio evidence.");
        profileData.put("updatedAt", FieldValue.serverTimestamp());

        firestore.collection("users")
                .document(userId)
                .set(profileData, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    saveProfileLocally(
                            userId,
                            email,
                            fullName,
                            degreeProgramme,
                            academicYear,
                            targetRole,
                            selectedSkills,
                            careerGoal,
                            profileCompletion,
                            claimedSkillCoverage,
                            careerReadiness
                    );

                    Toast.makeText(this, "Profile saved successfully", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(ProfileSetupActivity.this, DashboardActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    resetSaveButton();
                    Toast.makeText(this, "Failed to save profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void saveProfileLocally(
            String userId,
            String email,
            String fullName,
            String degreeProgramme,
            String academicYear,
            String targetRole,
            String selectedSkills,
            String careerGoal,
            int profileCompletion,
            int claimedSkillCoverage,
            int careerReadiness
    ) {
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        editor.putString("userId", userId);
        editor.putString("email", email);
        editor.putString("fullName", fullName);
        editor.putString("degreeProgramme", degreeProgramme);
        editor.putString("academicYear", academicYear);
        editor.putString("targetRole", targetRole);
        editor.putString("careerGoal", careerGoal);
        editor.putString("selectedSkills", selectedSkills);

        // Clear old single-link profile field to avoid duplicated data.
        editor.putString("portfolioLink", "");

        editor.putBoolean("accountCreated", true);
        editor.putBoolean("emailVerified", true);
        editor.putBoolean("profileCompleted", true);
        editor.putBoolean("evidenceBasedScoring", true);

        editor.putInt("profileCompletion", profileCompletion);
        editor.putInt("skillMatch", claimedSkillCoverage);
        editor.putInt("claimedSkillCoverage", claimedSkillCoverage);
        editor.putInt("careerReadiness", careerReadiness);

        editor.putString(
                "scoreExplanation",
                "Profile Setup stores basic student information only. Professional links are handled in Portfolio Builder as portfolio evidence."
        );

        editor.apply();
    }

    private void logoutUser() {
        firebaseAuth.signOut();

        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.apply();

        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(ProfileSetupActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void resetSaveButton() {
        saveProfileButton.setEnabled(true);

        boolean editMode = getIntent().getBooleanExtra("editMode", false);

        if (editMode) {
            saveProfileButton.setText("Update Profile");
        } else {
            saveProfileButton.setText("Save and Continue");
        }
    }

    private String getSelectedSkills() {
        StringBuilder skills = new StringBuilder();

        if (skillCheckBox1.getVisibility() == View.VISIBLE && skillCheckBox1.isChecked()) {
            skills.append(skillCheckBox1.getText().toString()).append(", ");
        }

        if (skillCheckBox2.getVisibility() == View.VISIBLE && skillCheckBox2.isChecked()) {
            skills.append(skillCheckBox2.getText().toString()).append(", ");
        }

        if (skillCheckBox3.getVisibility() == View.VISIBLE && skillCheckBox3.isChecked()) {
            skills.append(skillCheckBox3.getText().toString()).append(", ");
        }

        if (skillCheckBox4.getVisibility() == View.VISIBLE && skillCheckBox4.isChecked()) {
            skills.append(skillCheckBox4.getText().toString()).append(", ");
        }

        if (skillCheckBox5.getVisibility() == View.VISIBLE && skillCheckBox5.isChecked()) {
            skills.append(skillCheckBox5.getText().toString()).append(", ");
        }

        if (skillCheckBox6.getVisibility() == View.VISIBLE && skillCheckBox6.isChecked()) {
            skills.append(skillCheckBox6.getText().toString()).append(", ");
        }

        if (skills.length() > 0) {
            skills.setLength(skills.length() - 2);
        }

        return skills.toString();
    }

    private int calculateSkillMatch() {
        int selectedCount = 0;
        int totalSkills = currentSkillOptions.length;

        if (skillCheckBox1.getVisibility() == View.VISIBLE && skillCheckBox1.isChecked()) selectedCount++;
        if (skillCheckBox2.getVisibility() == View.VISIBLE && skillCheckBox2.isChecked()) selectedCount++;
        if (skillCheckBox3.getVisibility() == View.VISIBLE && skillCheckBox3.isChecked()) selectedCount++;
        if (skillCheckBox4.getVisibility() == View.VISIBLE && skillCheckBox4.isChecked()) selectedCount++;
        if (skillCheckBox5.getVisibility() == View.VISIBLE && skillCheckBox5.isChecked()) selectedCount++;
        if (skillCheckBox6.getVisibility() == View.VISIBLE && skillCheckBox6.isChecked()) selectedCount++;

        if (totalSkills == 0) {
            return 0;
        }

        /*
         * Realistic scoring rule:
         * Profile setup skills are self-selected claimed skills only.
         * They are not verified by skill gap assessment, projects, CV, GitHub, LinkedIn or interview practice yet.
         * Therefore, checkbox-only skill coverage is capped at 40%.
         */
        double claimedSkillCoverage = ((double) selectedCount / totalSkills) * 40.0;

        return (int) Math.round(claimedSkillCoverage);
    }

    private int calculateProfileCompletion(
            String fullName,
            String degreeProgramme,
            String academicYear,
            String targetRole,
            String selectedSkills,
            String careerGoal
    ) {
        int completed = 0;
        int total = 6;

        if (!fullName.isEmpty()) completed++;
        if (!degreeProgramme.equals("Select degree programme")) completed++;
        if (!academicYear.equals("Select academic year")) completed++;
        if (!targetRole.equals("Select target career role")) completed++;
        if (!selectedSkills.isEmpty()) completed++;
        if (!careerGoal.isEmpty()) completed++;

        return (completed * 100) / total;
    }

    private int calculateEvidenceBasedCareerReadiness(
            int profileCompletion,
            int claimedSkillCoverage
    ) {
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        int cvReadiness = preferences.getInt("cvReadiness", 0);
        int portfolioProgress = preferences.getInt("portfolioProgress", 0);
        int interviewReadiness = preferences.getInt("interviewReadiness", 0);

        double readiness =
                (profileCompletion * 0.15) +
                        (claimedSkillCoverage * 0.30) +
                        (cvReadiness * 0.20) +
                        (portfolioProgress * 0.20) +
                        (interviewReadiness * 0.15);

        int finalScore = (int) Math.round(readiness);

        return applyFreshGraduateScoreCaps(finalScore, cvReadiness, portfolioProgress, interviewReadiness);
    }

    private int applyFreshGraduateScoreCaps(
            int score,
            int cvReadiness,
            int portfolioProgress,
            int interviewReadiness
    ) {
        /*
         * Score dampening rule:
         * A fresh graduate should not receive a high readiness score without evidence.
         * Evidence means CV quality, portfolio/project proof and interview practice.
         */

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

        if (score < 0) {
            return 0;
        }

        if (score > 100) {
            return 100;
        }

        return score;
    }
}