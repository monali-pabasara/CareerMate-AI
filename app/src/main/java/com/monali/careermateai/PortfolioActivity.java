package com.monali.careermateai;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
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

public class PortfolioActivity extends AppCompatActivity {

    private TextView portfolioProgressText, portfolioMessageText;
    private TextView portfolioEvidenceText, linkAnalysisText, savedProjectsText;

    private EditText projectTitleInput, projectDescriptionInput, problemSolvedInput;
    private EditText projectToolsInput, keyFeaturesInput, projectRoleInput;
    private EditText projectLinkInput, projectLearningInput;
    private EditText githubLinkInput, linkedinLinkInput, portfolioWebsiteInput;

    private Spinner projectNumberSpinner, projectTypeSpinner;

    private Button saveProjectButton, clearProjectButton, backToDashboardButton;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    private static final String PREF_NAME = "CareerMateUser";
    private static final int MAX_PROJECTS = 5;

    private boolean isLoadingProject = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_portfolio);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        portfolioProgressText = findViewById(R.id.portfolioProgressText);
        portfolioMessageText = findViewById(R.id.portfolioMessageText);
        portfolioEvidenceText = findViewById(R.id.portfolioEvidenceText);
        linkAnalysisText = findViewById(R.id.linkAnalysisText);
        savedProjectsText = findViewById(R.id.savedProjectsText);

        projectTitleInput = findViewById(R.id.projectTitleInput);
        projectDescriptionInput = findViewById(R.id.projectDescriptionInput);
        problemSolvedInput = findViewById(R.id.problemSolvedInput);
        projectToolsInput = findViewById(R.id.projectToolsInput);
        keyFeaturesInput = findViewById(R.id.keyFeaturesInput);
        projectRoleInput = findViewById(R.id.projectRoleInput);
        projectLinkInput = findViewById(R.id.projectLinkInput);
        projectLearningInput = findViewById(R.id.projectLearningInput);

        githubLinkInput = findViewById(R.id.githubLinkInput);
        linkedinLinkInput = findViewById(R.id.linkedinLinkInput);
        portfolioWebsiteInput = findViewById(R.id.portfolioWebsiteInput);

        projectNumberSpinner = findViewById(R.id.projectNumberSpinner);
        projectTypeSpinner = findViewById(R.id.projectTypeSpinner);

        saveProjectButton = findViewById(R.id.saveProjectButton);
        clearProjectButton = findViewById(R.id.clearProjectButton);
        backToDashboardButton = findViewById(R.id.backToDashboardButton);

        setupProjectNumberSpinner();
        setupProjectTypeSpinner();

        migrateOldProjectDataIfNeeded();

        // First show local cache, then refresh from Firestore.
        loadSavedProfessionalLinks();
        selectBestProjectSlotFromLocalCache();
        updateOverallPortfolioDisplay();
        loadPortfolioFromFirestore();

        projectNumberSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!isLoadingProject) {
                    loadSelectedProject(position + 1);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        saveProjectButton.setOnClickListener(view -> saveProject());
        clearProjectButton.setOnClickListener(view -> clearSelectedProject());
        backToDashboardButton.setOnClickListener(view -> goToDashboard());
    }

    private void setupProjectNumberSpinner() {
        String[] projectNumbers = new String[MAX_PROJECTS];

        for (int i = 0; i < MAX_PROJECTS; i++) {
            projectNumbers[i] = "Project " + (i + 1);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                projectNumbers
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        projectNumberSpinner.setAdapter(adapter);
    }

    private void setupProjectTypeSpinner() {
        String[] projectTypes = {
                "Select project type",
                "Final Year Project",
                "Academic Project",
                "Mobile App",
                "Web Application",
                "Software Development Project",
                "AI Project",
                "Machine Learning Project",
                "Data Analysis Project",
                "Database Project",
                "Cybersecurity Project",
                "UI UX Design Project",
                "Research Project",
                "Business Project",
                "Marketing Campaign",
                "Other Project"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                projectTypes
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        projectTypeSpinner.setAdapter(adapter);
    }

    private void migrateOldProjectDataIfNeeded() {
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        String oldTitle = preferences.getString("projectTitle", "");
        String projectOneTitle = preferences.getString("project1_title", "");

        if (oldTitle.isEmpty() || !projectOneTitle.isEmpty()) {
            return;
        }

        String oldType = preferences.getString("projectType", "");
        String oldDescription = preferences.getString("projectDescription", "");
        String oldTools = preferences.getString("projectTools", "");
        String oldRole = preferences.getString("projectRole", "");
        String oldLink = preferences.getString("projectLink", "");
        String oldLearning = preferences.getString("projectLearning", "");

        int oldQuality = calculateSingleProjectQuality(
                oldTitle,
                oldType,
                oldDescription,
                "",
                oldTools,
                "",
                oldRole,
                oldLink,
                oldLearning
        );

        SharedPreferences.Editor editor = preferences.edit();

        editor.putString("project1_title", oldTitle);
        editor.putString("project1_type", oldType);
        editor.putString("project1_description", oldDescription);
        editor.putString("project1_problemSolved", "");
        editor.putString("project1_tools", oldTools);
        editor.putString("project1_keyFeatures", "");
        editor.putString("project1_role", oldRole);
        editor.putString("project1_link", oldLink);
        editor.putString("project1_learning", oldLearning);
        editor.putInt("project1_quality", oldQuality);

        editor.apply();
    }

    private void loadSavedProfessionalLinks() {
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        githubLinkInput.setText(preferences.getString("githubLink", ""));
        linkedinLinkInput.setText(preferences.getString("linkedinLink", ""));
        portfolioWebsiteInput.setText(preferences.getString("portfolioWebsite", ""));
    }

    private void loadSelectedProject(int projectIndex) {
        isLoadingProject = true;

        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String prefix = getProjectPrefix(projectIndex);

        String title = preferences.getString(prefix + "title", "");
        String type = preferences.getString(prefix + "type", "");
        String description = preferences.getString(prefix + "description", "");
        String purpose = preferences.getString(prefix + "problemSolved", "");
        String tools = preferences.getString(prefix + "tools", "");
        String keyFeatures = preferences.getString(prefix + "keyFeatures", "");
        String role = preferences.getString(prefix + "role", "");
        String link = preferences.getString(prefix + "link", "");
        String learning = preferences.getString(prefix + "learning", "");

        projectTitleInput.setText(title);
        projectDescriptionInput.setText(description);
        problemSolvedInput.setText(purpose);
        projectToolsInput.setText(tools);
        keyFeaturesInput.setText(keyFeatures);
        projectRoleInput.setText(role);
        projectLinkInput.setText(link);
        projectLearningInput.setText(learning);

        setSpinnerSelection(projectTypeSpinner, type);

        isLoadingProject = false;
    }

    private void selectBestProjectSlotFromLocalCache() {
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        int bestProjectIndex = findNextEmptyProjectIndex(preferences);
        selectProjectSlotAndLoad(bestProjectIndex);
    }

    private int findNextEmptyProjectIndex(SharedPreferences preferences) {
        for (int i = 1; i <= MAX_PROJECTS; i++) {
            if (!isProjectSaved(preferences, i)) {
                return i;
            }
        }

        // If all 5 projects are saved, open Project 1 for editing.
        return 1;
    }

    private void selectProjectSlotAndLoad(int projectIndex) {
        if (projectIndex < 1) {
            projectIndex = 1;
        }

        if (projectIndex > MAX_PROJECTS) {
            projectIndex = MAX_PROJECTS;
        }

        isLoadingProject = true;
        projectNumberSpinner.setSelection(projectIndex - 1);
        isLoadingProject = false;

        loadSelectedProject(projectIndex);
    }

    private void setSpinnerSelection(Spinner spinner, String savedValue) {
        if (savedValue == null || savedValue.isEmpty()) {
            spinner.setSelection(0);
            return;
        }

        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equals(savedValue)) {
                spinner.setSelection(i);
                return;
            }
        }

        spinner.setSelection(0);
    }

    private int getCurrentProjectIndex() {
        return projectNumberSpinner.getSelectedItemPosition() + 1;
    }

    private String getProjectPrefix(int projectIndex) {
        return "project" + projectIndex + "_";
    }

    private void saveProject() {
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String targetRole = preferences.getString("targetRole", "General Graduate Role");

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            CustomToast.showError(this, "Please login again before saving portfolio");
            return;
        }

        int projectIndex = getCurrentProjectIndex();
        String prefix = getProjectPrefix(projectIndex);

        String projectTitle = projectTitleInput.getText().toString().trim();
        String projectType = projectTypeSpinner.getSelectedItem().toString();
        String projectDescription = projectDescriptionInput.getText().toString().trim();
        String projectPurpose = problemSolvedInput.getText().toString().trim();
        String projectTools = projectToolsInput.getText().toString().trim();
        String keyFeatures = keyFeaturesInput.getText().toString().trim();
        String projectRole = projectRoleInput.getText().toString().trim();
        String projectLink = projectLinkInput.getText().toString().trim();
        String projectLearning = projectLearningInput.getText().toString().trim();

        String githubLink = githubLinkInput.getText().toString().trim();
        String linkedinLink = linkedinLinkInput.getText().toString().trim();
        String portfolioWebsite = portfolioWebsiteInput.getText().toString().trim();

        if (projectTitle.isEmpty()) {
            projectTitleInput.setError("Please enter your project title");
            projectTitleInput.requestFocus();
            return;
        }

        if (projectType.equals("Select project type")) {
            CustomToast.showInfo(this, "Please select project type");
            return;
        }

        if (projectDescription.isEmpty()) {
            projectDescriptionInput.setError("Please enter project description");
            projectDescriptionInput.requestFocus();
            return;
        }

        if (projectPurpose.isEmpty()) {
            problemSolvedInput.setError("Please explain the project purpose or user need");
            problemSolvedInput.requestFocus();
            return;
        }

        if (projectTools.isEmpty()) {
            projectToolsInput.setError("Please enter tools or technologies used");
            projectToolsInput.requestFocus();
            return;
        }

        if (countCommaSeparatedItems(projectTools) < 2) {
            projectToolsInput.setError("Please enter at least 2 tools separated by commas");
            projectToolsInput.requestFocus();
            return;
        }

        if (keyFeatures.isEmpty()) {
            keyFeaturesInput.setError("Please enter key features built");
            keyFeaturesInput.requestFocus();
            return;
        }

        if (countLineSeparatedItems(keyFeatures) < 2) {
            keyFeaturesInput.setError("Please enter at least 2 features on separate lines");
            keyFeaturesInput.requestFocus();
            return;
        }

        if (projectRole.isEmpty()) {
            projectRoleInput.setError("Please explain your role");
            projectRoleInput.requestFocus();
            return;
        }

        if (!githubLink.isEmpty() && !isGithubLink(githubLink)) {
            githubLinkInput.setError("Please enter a valid GitHub link");
            githubLinkInput.requestFocus();
            return;
        }

        if (!linkedinLink.isEmpty() && !isLinkedInLink(linkedinLink)) {
            linkedinLinkInput.setError("Please enter a valid LinkedIn link");
            linkedinLinkInput.requestFocus();
            return;
        }

        if (!portfolioWebsite.isEmpty() && !isValidWebLink(portfolioWebsite)) {
            portfolioWebsiteInput.setError("Please enter a valid portfolio website link");
            portfolioWebsiteInput.requestFocus();
            return;
        }

        // Project evidence link is optional.
        // If the student enters it, then the app validates the link format.
        if (!projectLink.isEmpty() && !isValidWebLink(projectLink)) {
            projectLinkInput.setError("Please enter a valid project evidence link or leave it empty");
            projectLinkInput.requestFocus();
            return;
        }

        int selectedProjectQuality = calculateSingleProjectQuality(
                projectTitle,
                projectType,
                projectDescription,
                projectPurpose,
                projectTools,
                keyFeatures,
                projectRole,
                projectLink,
                projectLearning
        );

        String githubUsername = extractGithubUsername(githubLink);

        SharedPreferences.Editor editor = preferences.edit();

        editor.putString(prefix + "title", projectTitle);
        editor.putString(prefix + "type", projectType);
        editor.putString(prefix + "description", projectDescription);
        editor.putString(prefix + "problemSolved", projectPurpose);
        editor.putString(prefix + "tools", projectTools);
        editor.putString(prefix + "keyFeatures", keyFeatures);
        editor.putString(prefix + "role", projectRole);
        editor.putString(prefix + "link", projectLink);
        editor.putString(prefix + "learning", projectLearning);
        editor.putInt(prefix + "quality", selectedProjectQuality);

        editor.putString("githubLink", githubLink);
        editor.putString("linkedinLink", linkedinLink);
        editor.putString("portfolioWebsite", portfolioWebsite);
        editor.putString("githubUsername", githubUsername);

        // Old compatibility keys for Dashboard / previous version.
        editor.putString("projectTitle", projectTitle);
        editor.putString("projectType", projectType);
        editor.putString("projectDescription", projectDescription);
        editor.putString("projectTools", projectTools);
        editor.putString("projectRole", projectRole);
        editor.putString("projectLink", projectLink);
        editor.putString("projectLearning", projectLearning);

        editor.commit();

        int savedProjectCount = countSavedProjects(preferences);
        int portfolioProgress = calculateOverallPortfolioProgress(preferences, targetRole);
        String displayStatus = getDisplayPortfolioStatus(portfolioProgress, savedProjectCount);
        String savedProjectsSummary = buildSavedProjectsSummary(preferences);
        String linkAnalysisSummary = generatePortfolioAnalysisSummary(
                preferences,
                targetRole,
                portfolioProgress,
                displayStatus
        );

        int profileCompletion = preferences.getInt("profileCompletion", 0);
        int skillMatch = preferences.getInt(
                "skillMatch",
                preferences.getInt("claimedSkillCoverage", 0)
        );
        int cvReadiness = preferences.getInt("cvReadiness", 0);
        int interviewReadiness = preferences.getInt("interviewReadiness", 0);

        int careerReadiness = calculateCareerReadiness(
                profileCompletion,
                skillMatch,
                portfolioProgress,
                cvReadiness,
                interviewReadiness
        );

        SharedPreferences.Editor finalEditor = preferences.edit();

        finalEditor.putInt("projectCount", savedProjectCount);
        finalEditor.putInt("portfolioProgress", portfolioProgress);
        finalEditor.putInt("portfolioEvidenceScore", portfolioProgress);
        finalEditor.putString("portfolioEvidenceStatus", displayStatus);
        finalEditor.putString("savedProjectsSummary", savedProjectsSummary);
        finalEditor.putString("linkAnalysisSummary", linkAnalysisSummary);
        finalEditor.putBoolean("portfolioStarted", savedProjectCount > 0);
        finalEditor.putInt("careerReadiness", careerReadiness);

        finalEditor.apply();

        updateOverallPortfolioDisplay();

        Map<String, Object> projectData = buildProjectFirestoreMap(
                projectIndex,
                projectTitle,
                projectType,
                projectDescription,
                projectPurpose,
                projectTools,
                keyFeatures,
                projectRole,
                projectLink,
                projectLearning,
                selectedProjectQuality
        );

        Map<String, Object> summaryData = buildPortfolioSummaryFirestoreMap(
                preferences,
                savedProjectCount,
                portfolioProgress,
                displayStatus,
                savedProjectsSummary,
                linkAnalysisSummary,
                githubLink,
                linkedinLink,
                portfolioWebsite,
                githubUsername,
                careerReadiness
        );

        saveProjectButton.setEnabled(false);
        saveProjectButton.setText("Saving...");

        saveProjectToFirestore(
                currentUser.getUid(),
                projectIndex,
                projectData,
                summaryData
        );
    }

    private Map<String, Object> buildProjectFirestoreMap(
            int projectIndex,
            String projectTitle,
            String projectType,
            String projectDescription,
            String projectPurpose,
            String projectTools,
            String keyFeatures,
            String projectRole,
            String projectLink,
            String projectLearning,
            int selectedProjectQuality
    ) {
        Map<String, Object> projectData = new HashMap<>();

        projectData.put("projectIndex", projectIndex);
        projectData.put("title", projectTitle);
        projectData.put("type", projectType);
        projectData.put("description", projectDescription);
        projectData.put("problemSolved", projectPurpose);
        projectData.put("tools", projectTools);
        projectData.put("keyFeatures", keyFeatures);
        projectData.put("role", projectRole);
        projectData.put("link", projectLink);
        projectData.put("learning", projectLearning);
        projectData.put("quality", selectedProjectQuality);
        projectData.put("updatedAt", FieldValue.serverTimestamp());

        return projectData;
    }

    private Map<String, Object> buildPortfolioSummaryFirestoreMap(
            SharedPreferences preferences,
            int savedProjectCount,
            int portfolioProgress,
            String displayStatus,
            String savedProjectsSummary,
            String linkAnalysisSummary,
            String githubLink,
            String linkedinLink,
            String portfolioWebsite,
            String githubUsername,
            int careerReadiness
    ) {
        Map<String, Object> summaryData = new HashMap<>();

        summaryData.put("projectCount", savedProjectCount);
        summaryData.put("portfolioProgress", portfolioProgress);
        summaryData.put("portfolioEvidenceScore", portfolioProgress);
        summaryData.put("portfolioEvidenceStatus", displayStatus);
        summaryData.put("savedProjectsSummary", savedProjectsSummary);
        summaryData.put("linkAnalysisSummary", linkAnalysisSummary);
        summaryData.put("portfolioStarted", savedProjectCount > 0);

        summaryData.put("githubLink", githubLink);
        summaryData.put("linkedinLink", linkedinLink);
        summaryData.put("portfolioWebsite", portfolioWebsite);
        summaryData.put("githubUsername", githubUsername);

        summaryData.put("careerReadiness", careerReadiness);
        summaryData.put("targetRole", preferences.getString("targetRole", "General Graduate Role"));
        summaryData.put("updatedAt", FieldValue.serverTimestamp());

        return summaryData;
    }

    private void saveProjectToFirestore(
            String uid,
            int projectIndex,
            Map<String, Object> projectData,
            Map<String, Object> summaryData
    ) {
        firestore.collection("users")
                .document(uid)
                .collection("portfolioProjects")
                .document("project" + projectIndex)
                .set(projectData, SetOptions.merge())
                .addOnSuccessListener(unused -> savePortfolioSummaryToFirestore(uid, projectIndex, summaryData))
                .addOnFailureListener(e -> {
                    saveProjectButton.setEnabled(true);
                    saveProjectButton.setText("Save Project Evidence");

                    CustomToast.showError(
                            this,
                            "Project saved locally, but cloud save failed"
                    );
                });
    }

    private void savePortfolioSummaryToFirestore(
            String uid,
            int projectIndex,
            Map<String, Object> summaryData
    ) {
        firestore.collection("users")
                .document(uid)
                .set(summaryData, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    saveProjectButton.setEnabled(true);
                    saveProjectButton.setText("Save Project Evidence");

                    CustomToast.showSuccess(
                            this,
                            "Project " + projectIndex + " saved successfully"
                    );

                    moveToBestProjectSlotAfterSave(projectIndex);
                })
                .addOnFailureListener(e -> {
                    saveProjectButton.setEnabled(true);
                    saveProjectButton.setText("Save Project Evidence");

                    CustomToast.showError(
                            this,
                            "Project saved, but summary update failed"
                    );
                });
    }

    private void moveToBestProjectSlotAfterSave(int savedProjectIndex) {
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        int savedProjectCount = countSavedProjects(preferences);

        if (savedProjectCount >= MAX_PROJECTS) {
            updateOverallPortfolioDisplay();
            CustomToast.showInfo(
                    this,
                    "All 5 project slots are completed. You can edit saved projects."
            );
            return;
        }

        int nextEmptyProjectIndex = findNextEmptyProjectIndex(preferences);

        selectProjectSlotAndLoad(nextEmptyProjectIndex);
        updateOverallPortfolioDisplay();

        CustomToast.showInfo(
                this,
                "Now you can add Project " + nextEmptyProjectIndex
        );
    }

    private void loadPortfolioFromFirestore() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            return;
        }

        String uid = currentUser.getUid();

        firestore.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    cachePortfolioSummaryFromFirestore(documentSnapshot);

                    firestore.collection("users")
                            .document(uid)
                            .collection("portfolioProjects")
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
                                SharedPreferences.Editor editor = preferences.edit();

                                for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                                    int projectIndex = getProjectIndexFromDocumentId(document.getId());

                                    if (projectIndex >= 1 && projectIndex <= MAX_PROJECTS) {
                                        cacheProjectFromFirestore(editor, document, projectIndex);
                                    }
                                }

                                editor.commit();

                                loadSavedProfessionalLinks();

                                // After Firestore refresh, open the next empty project slot.
                                // Example: if Project 1, 2, and 3 are saved, open Project 4.
                                selectBestProjectSlotFromLocalCache();

                                updateOverallPortfolioDisplay();
                            })
                            .addOnFailureListener(e -> CustomToast.showInfo(
                                    this,
                                    "Using local portfolio cache"
                            ));
                })
                .addOnFailureListener(e -> CustomToast.showInfo(
                        this,
                        "Using local portfolio cache"
                ));
    }

    private void cachePortfolioSummaryFromFirestore(DocumentSnapshot documentSnapshot) {
        if (documentSnapshot == null || !documentSnapshot.exists()) {
            return;
        }

        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        if (documentSnapshot.contains("githubLink")) {
            editor.putString("githubLink", getStringValue(documentSnapshot, "githubLink"));
        }

        if (documentSnapshot.contains("linkedinLink")) {
            editor.putString("linkedinLink", getStringValue(documentSnapshot, "linkedinLink"));
        }

        if (documentSnapshot.contains("portfolioWebsite")) {
            editor.putString("portfolioWebsite", getStringValue(documentSnapshot, "portfolioWebsite"));
        }

        if (documentSnapshot.contains("githubUsername")) {
            editor.putString("githubUsername", getStringValue(documentSnapshot, "githubUsername"));
        }

        if (documentSnapshot.contains("projectCount")) {
            editor.putInt("projectCount", getIntValue(documentSnapshot, "projectCount"));
        }

        if (documentSnapshot.contains("portfolioProgress")) {
            editor.putInt("portfolioProgress", getIntValue(documentSnapshot, "portfolioProgress"));
        }

        if (documentSnapshot.contains("portfolioEvidenceScore")) {
            editor.putInt("portfolioEvidenceScore", getIntValue(documentSnapshot, "portfolioEvidenceScore"));
        }

        if (documentSnapshot.contains("portfolioEvidenceStatus")) {
            editor.putString("portfolioEvidenceStatus", getStringValue(documentSnapshot, "portfolioEvidenceStatus"));
        }

        if (documentSnapshot.contains("savedProjectsSummary")) {
            editor.putString("savedProjectsSummary", getStringValue(documentSnapshot, "savedProjectsSummary"));
        }

        if (documentSnapshot.contains("linkAnalysisSummary")) {
            editor.putString("linkAnalysisSummary", getStringValue(documentSnapshot, "linkAnalysisSummary"));
        }

        if (documentSnapshot.contains("portfolioStarted")) {
            Boolean started = documentSnapshot.getBoolean("portfolioStarted");
            editor.putBoolean("portfolioStarted", started != null && started);
        }

        if (documentSnapshot.contains("careerReadiness")) {
            editor.putInt("careerReadiness", getIntValue(documentSnapshot, "careerReadiness"));
        }

        editor.apply();
    }

    private void cacheProjectFromFirestore(
            SharedPreferences.Editor editor,
            DocumentSnapshot document,
            int projectIndex
    ) {
        String prefix = getProjectPrefix(projectIndex);

        editor.putString(prefix + "title", getStringValue(document, "title"));
        editor.putString(prefix + "type", getStringValue(document, "type"));
        editor.putString(prefix + "description", getStringValue(document, "description"));
        editor.putString(prefix + "problemSolved", getStringValue(document, "problemSolved"));
        editor.putString(prefix + "tools", getStringValue(document, "tools"));
        editor.putString(prefix + "keyFeatures", getStringValue(document, "keyFeatures"));
        editor.putString(prefix + "role", getStringValue(document, "role"));
        editor.putString(prefix + "link", getStringValue(document, "link"));
        editor.putString(prefix + "learning", getStringValue(document, "learning"));
        editor.putInt(prefix + "quality", getIntValue(document, "quality"));
    }

    private int getProjectIndexFromDocumentId(String documentId) {
        if (documentId == null || !documentId.startsWith("project")) {
            return -1;
        }

        try {
            return Integer.parseInt(documentId.replace("project", ""));
        } catch (NumberFormatException e) {
            return -1;
        }
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

    private void clearSelectedProject() {
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            CustomToast.showError(this, "Please login again before clearing portfolio");
            return;
        }

        int projectIndex = getCurrentProjectIndex();
        String prefix = getProjectPrefix(projectIndex);

        SharedPreferences.Editor editor = preferences.edit();

        editor.remove(prefix + "title");
        editor.remove(prefix + "type");
        editor.remove(prefix + "description");
        editor.remove(prefix + "problemSolved");
        editor.remove(prefix + "tools");
        editor.remove(prefix + "keyFeatures");
        editor.remove(prefix + "role");
        editor.remove(prefix + "link");
        editor.remove(prefix + "learning");
        editor.remove(prefix + "quality");

        if (projectIndex == 1) {
            editor.remove("projectTitle");
            editor.remove("projectType");
            editor.remove("projectDescription");
            editor.remove("projectTools");
            editor.remove("projectRole");
            editor.remove("projectLink");
            editor.remove("projectLearning");
        }

        editor.commit();

        int savedProjectCount = countSavedProjects(preferences);
        String targetRole = preferences.getString("targetRole", "General Graduate Role");
        int portfolioProgress = calculateOverallPortfolioProgress(preferences, targetRole);
        String displayStatus = getDisplayPortfolioStatus(portfolioProgress, savedProjectCount);
        String savedProjectsSummary = buildSavedProjectsSummary(preferences);
        String linkAnalysisSummary = generatePortfolioAnalysisSummary(
                preferences,
                targetRole,
                portfolioProgress,
                displayStatus
        );

        int profileCompletion = preferences.getInt("profileCompletion", 0);
        int skillMatch = preferences.getInt(
                "skillMatch",
                preferences.getInt("claimedSkillCoverage", 0)
        );
        int cvReadiness = preferences.getInt("cvReadiness", 0);
        int interviewReadiness = preferences.getInt("interviewReadiness", 0);

        int careerReadiness = calculateCareerReadiness(
                profileCompletion,
                skillMatch,
                portfolioProgress,
                cvReadiness,
                interviewReadiness
        );

        SharedPreferences.Editor summaryEditor = preferences.edit();
        summaryEditor.putInt("projectCount", savedProjectCount);
        summaryEditor.putInt("portfolioProgress", portfolioProgress);
        summaryEditor.putInt("portfolioEvidenceScore", portfolioProgress);
        summaryEditor.putString("portfolioEvidenceStatus", displayStatus);
        summaryEditor.putString("savedProjectsSummary", savedProjectsSummary);
        summaryEditor.putString("linkAnalysisSummary", linkAnalysisSummary);
        summaryEditor.putBoolean("portfolioStarted", savedProjectCount > 0);
        summaryEditor.putInt("careerReadiness", careerReadiness);
        summaryEditor.apply();

        loadSelectedProject(projectIndex);
        updateOverallPortfolioDisplay();

        Map<String, Object> summaryData = buildPortfolioSummaryFirestoreMap(
                preferences,
                savedProjectCount,
                portfolioProgress,
                displayStatus,
                savedProjectsSummary,
                linkAnalysisSummary,
                preferences.getString("githubLink", ""),
                preferences.getString("linkedinLink", ""),
                preferences.getString("portfolioWebsite", ""),
                preferences.getString("githubUsername", ""),
                careerReadiness
        );

        clearProjectButton.setEnabled(false);
        clearProjectButton.setText("Clearing...");

        firestore.collection("users")
                .document(currentUser.getUid())
                .collection("portfolioProjects")
                .document("project" + projectIndex)
                .delete()
                .addOnSuccessListener(unused -> updatePortfolioSummaryAfterClear(
                        currentUser.getUid(),
                        summaryData
                ))
                .addOnFailureListener(e -> {
                    clearProjectButton.setEnabled(true);
                    clearProjectButton.setText("Clear Selected Project");
                    CustomToast.showError(this, "Cloud clear failed");
                });
    }

    private void updatePortfolioSummaryAfterClear(
            String uid,
            Map<String, Object> summaryData
    ) {
        firestore.collection("users")
                .document(uid)
                .set(summaryData, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    clearProjectButton.setEnabled(true);
                    clearProjectButton.setText("Clear Selected Project");

                    CustomToast.showSuccess(this, "Selected project cleared");

                    // After clearing, open the best next empty project slot.
                    selectBestProjectSlotFromLocalCache();
                    updateOverallPortfolioDisplay();
                })
                .addOnFailureListener(e -> {
                    clearProjectButton.setEnabled(true);
                    clearProjectButton.setText("Clear Selected Project");
                    CustomToast.showError(this, "Project cleared locally, but summary update failed");
                });
    }

    private int calculateSingleProjectQuality(
            String projectTitle,
            String projectType,
            String projectDescription,
            String projectPurpose,
            String projectTools,
            String keyFeatures,
            String projectRole,
            String projectLink,
            String projectLearning
    ) {
        int quality = 0;

        int toolsCount = countCommaSeparatedItems(projectTools);
        int featuresCount = countLineSeparatedItems(keyFeatures);

        if (!projectTitle.isEmpty()) quality += 8;
        if (!projectType.equals("Select project type")) quality += 7;

        if (projectDescription.length() >= 80) {
            quality += 12;
        } else if (!projectDescription.isEmpty()) {
            quality += 7;
        }

        if (projectPurpose.length() >= 50) {
            quality += 12;
        } else if (!projectPurpose.isEmpty()) {
            quality += 8;
        }

        if (toolsCount >= 3) {
            quality += 13;
        } else if (toolsCount >= 2) {
            quality += 9;
        }

        if (featuresCount >= 3) {
            quality += 13;
        } else if (featuresCount >= 2) {
            quality += 9;
        }

        if (!projectRole.isEmpty()) quality += 10;

        // Optional evidence link gives extra evidence, but it is not compulsory.
        if (!projectLink.isEmpty()) quality += 10;

        if (projectLearning.length() >= 40) {
            quality += 10;
        } else if (!projectLearning.isEmpty()) {
            quality += 5;
        }

        return boundScore(quality);
    }

    private int calculateOverallPortfolioProgress(
            SharedPreferences preferences,
            String targetRole
    ) {
        int savedProjectCount = countSavedProjects(preferences);

        if (savedProjectCount == 0) {
            return 0;
        }

        int professionalLinkScore = calculateProfessionalLinkScore(preferences, targetRole);

        int baseScore;

        if (savedProjectCount == 1) {
            baseScore = 25;
        } else if (savedProjectCount == 2) {
            baseScore = 40;
        } else if (savedProjectCount == 3) {
            baseScore = 55;
        } else if (savedProjectCount == 4) {
            baseScore = 65;
        } else {
            baseScore = 70;
        }

        int totalQuality = 0;

        for (int i = 1; i <= MAX_PROJECTS; i++) {
            if (isProjectSaved(preferences, i)) {
                totalQuality += preferences.getInt(getProjectPrefix(i) + "quality", 0);
            }
        }

        int averageQuality = totalQuality / savedProjectCount;
        int qualityContribution = (averageQuality * 20) / 100;
        int roleFitContribution = calculateRoleFitScore(preferences, targetRole);

        int totalProgress = baseScore +
                qualityContribution +
                professionalLinkScore +
                roleFitContribution;

        return boundScore(totalProgress);
    }

    private int calculateProfessionalLinkScore(
            SharedPreferences preferences,
            String targetRole
    ) {
        String githubLink = preferences.getString("githubLink", "");
        String linkedinLink = preferences.getString("linkedinLink", "");
        String portfolioWebsite = preferences.getString("portfolioWebsite", "");

        boolean developerRole = isDeveloperRole(targetRole);
        boolean hasProjectLink = hasAnyProjectLink(preferences);

        int score = 0;

        if (developerRole) {
            if (!githubLink.isEmpty()) score += 10;
            if (!linkedinLink.isEmpty()) score += 4;
            if (!portfolioWebsite.isEmpty()) score += 4;
            if (hasProjectLink) score += 2;
        } else {
            if (!githubLink.isEmpty()) score += 3;
            if (!linkedinLink.isEmpty()) score += 7;
            if (!portfolioWebsite.isEmpty()) score += 7;
            if (hasProjectLink) score += 3;
        }

        return Math.min(score, 20);
    }

    private int calculateRoleFitScore(
            SharedPreferences preferences,
            String targetRole
    ) {
        String allProjectText = buildAllProjectText(preferences).toLowerCase();
        String[] keywords;

        if (targetRole.equals("Mobile App Developer")) {
            keywords = new String[]{
                    "android", "java", "kotlin", "xml", "firebase",
                    "api", "mobile", "testing", "debugging", "github"
            };
        } else if (targetRole.equals("Software Developer")) {
            keywords = new String[]{
                    "java", "python", "database", "sql", "api",
                    "github", "testing", "debugging", "backend", "frontend"
            };
        } else if (targetRole.equals("Data Analyst")) {
            keywords = new String[]{
                    "excel", "sql", "power bi", "python", "data",
                    "dashboard", "visualisation", "visualization", "report", "analysis"
            };
        } else if (targetRole.equals("Marketing Executive")) {
            keywords = new String[]{
                    "marketing", "campaign", "social media", "content", "canva",
                    "analytics", "seo", "audience", "brand", "report"
            };
        } else if (targetRole.equals("UI UX Designer")) {
            keywords = new String[]{
                    "figma", "prototype", "wireframe", "ui", "ux",
                    "design", "user research", "usability", "accessibility", "mockup"
            };
        } else {
            keywords = new String[]{
                    "research", "analysis", "report", "communication", "presentation",
                    "documentation", "team", "planning", "management", "problem"
            };
        }

        int matches = 0;

        for (String keyword : keywords) {
            if (allProjectText.contains(keyword)) {
                matches++;
            }
        }

        return Math.min(matches * 2, 10);
    }

    private String buildAllProjectText(SharedPreferences preferences) {
        StringBuilder builder = new StringBuilder();

        for (int i = 1; i <= MAX_PROJECTS; i++) {
            String prefix = getProjectPrefix(i);

            builder.append(preferences.getString(prefix + "title", "")).append(" ");
            builder.append(preferences.getString(prefix + "type", "")).append(" ");
            builder.append(preferences.getString(prefix + "description", "")).append(" ");
            builder.append(preferences.getString(prefix + "problemSolved", "")).append(" ");
            builder.append(preferences.getString(prefix + "tools", "")).append(" ");
            builder.append(preferences.getString(prefix + "keyFeatures", "")).append(" ");
            builder.append(preferences.getString(prefix + "role", "")).append(" ");
            builder.append(preferences.getString(prefix + "learning", "")).append(" ");
        }

        return builder.toString();
    }

    private int countSavedProjects(SharedPreferences preferences) {
        int count = 0;

        for (int i = 1; i <= MAX_PROJECTS; i++) {
            if (isProjectSaved(preferences, i)) {
                count++;
            }
        }

        return count;
    }

    private boolean isProjectSaved(SharedPreferences preferences, int projectIndex) {
        return !preferences.getString(getProjectPrefix(projectIndex) + "title", "").isEmpty();
    }

    private boolean hasAnyProjectLink(SharedPreferences preferences) {
        for (int i = 1; i <= MAX_PROJECTS; i++) {
            if (!preferences.getString(getProjectPrefix(i) + "link", "").isEmpty()) {
                return true;
            }
        }

        return false;
    }

    private String buildSavedProjectsSummary(SharedPreferences preferences) {
        StringBuilder summary = new StringBuilder();
        int savedCount = countSavedProjects(preferences);

        summary.append("Saved Projects: ")
                .append(savedCount)
                .append("/")
                .append(MAX_PROJECTS)
                .append("\n\n");

        for (int i = 1; i <= MAX_PROJECTS; i++) {
            String title = preferences.getString(getProjectPrefix(i) + "title", "");
            String type = preferences.getString(getProjectPrefix(i) + "type", "");

            if (!title.isEmpty()) {
                summary.append("✓ Project ")
                        .append(i)
                        .append(": ")
                        .append(title);

                if (!type.isEmpty() && !type.equals("Select project type")) {
                    summary.append(" — ").append(type);
                }

                summary.append("\n");
            } else {
                summary.append("○ Project ")
                        .append(i)
                        .append(": Not added yet\n");
            }
        }

        return summary.toString();
    }

    private String generatePortfolioAnalysisSummary(
            SharedPreferences preferences,
            String targetRole,
            int portfolioProgress,
            String displayStatus
    ) {
        StringBuilder result = new StringBuilder();

        int savedProjectCount = countSavedProjects(preferences);
        int detectedTechnologyCount = countAllTechnologies(preferences);
        int detectedFeatureCount = countAllFeatures(preferences);

        String githubLink = preferences.getString("githubLink", "");
        String linkedinLink = preferences.getString("linkedinLink", "");
        String portfolioWebsite = preferences.getString("portfolioWebsite", "");
        String githubUsername = extractGithubUsername(githubLink);

        result.append("Portfolio Evidence Status: ")
                .append(displayStatus)
                .append("\n\n");

        result.append("Project Evidence Review:\n");

        if (savedProjectCount == 0) {
            result.append("✗ No project evidence added yet.\n");
        } else if (savedProjectCount == 1) {
            result.append("✓ 1 project added. Add more projects to show stronger evidence.\n");
        } else {
            result.append("✓ ")
                    .append(savedProjectCount)
                    .append(" projects added. This gives better portfolio evidence.\n");
        }

        result.append("✓ Detected Technologies: ")
                .append(detectedTechnologyCount)
                .append("\n");

        result.append("✓ Detected Key Features: ")
                .append(detectedFeatureCount)
                .append("\n");

        if (hasAnyProjectLink(preferences)) {
            result.append("✓ Optional project evidence link detected.\n");
        } else {
            result.append("○ Optional project evidence link not added yet.\n");
        }

        result.append("\nProfessional Links:\n");

        if (!githubLink.isEmpty()) {
            result.append("✓ GitHub link detected");

            if (!githubUsername.isEmpty()) {
                result.append(" — Username: ").append(githubUsername);
            }

            result.append("\n");
        } else {
            result.append("✗ GitHub link missing.\n");
        }

        if (!linkedinLink.isEmpty()) {
            result.append("✓ LinkedIn profile link detected.\n");
        } else {
            result.append("✗ LinkedIn profile link missing.\n");
        }

        if (!portfolioWebsite.isEmpty()) {
            result.append("✓ Portfolio website link detected.\n");
        } else {
            result.append("✗ Portfolio website link missing.\n");
        }

        result.append("\nNext Step:\n");
        result.append(getPortfolioNextStep(savedProjectCount, portfolioProgress, targetRole, githubLink));

        return result.toString();
    }

    private int countCommaSeparatedItems(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0;
        }

        String[] items = value.split(",");
        int count = 0;

        for (String item : items) {
            if (!item.trim().isEmpty()) {
                count++;
            }
        }

        return count;
    }

    private int countLineSeparatedItems(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0;
        }

        String[] items = value.split("\\n");
        int count = 0;

        for (String item : items) {
            if (!item.trim().isEmpty()) {
                count++;
            }
        }

        return count;
    }

    private int countAllTechnologies(SharedPreferences preferences) {
        int total = 0;

        for (int i = 1; i <= MAX_PROJECTS; i++) {
            String tools = preferences.getString(getProjectPrefix(i) + "tools", "");
            total += countCommaSeparatedItems(tools);
        }

        return total;
    }

    private int countAllFeatures(SharedPreferences preferences) {
        int total = 0;

        for (int i = 1; i <= MAX_PROJECTS; i++) {
            String features = preferences.getString(getProjectPrefix(i) + "keyFeatures", "");
            total += countLineSeparatedItems(features);
        }

        return total;
    }

    private String getDisplayPortfolioStatus(int portfolioProgress, int projectCount) {
        if (projectCount == 0) {
            return "Not Started";
        }

        return getPortfolioStatus(portfolioProgress);
    }

    private String getPortfolioStatus(int portfolioProgress) {
        if (portfolioProgress == 0) {
            return "Not Started";
        } else if (portfolioProgress < 35) {
            return "Basic Evidence";
        } else if (portfolioProgress < 60) {
            return "Developing Evidence";
        } else if (portfolioProgress < 80) {
            return "Good Evidence";
        } else {
            return "Strong Evidence";
        }
    }

    private String getPortfolioNextStep(
            int projectCount,
            int portfolioProgress,
            String targetRole,
            String githubLink
    ) {
        if (projectCount == 0) {
            return "Add your first project with technologies and evidence links.";
        }

        if (projectCount < 3) {
            return "Add 2–3 projects to show stronger portfolio evidence.";
        }

        if (isDeveloperRole(targetRole) && githubLink.isEmpty()) {
            return "Add a GitHub profile or repository link for stronger developer evidence.";
        }

        if (portfolioProgress < 60) {
            return "Improve project descriptions, project purpose, tools used and key features.";
        }

        if (portfolioProgress < 80) {
            return "Add stronger proof such as GitHub repositories, live demos or LinkedIn posts.";
        }

        return "Keep your project details and professional links updated.";
    }

    private void updateOverallPortfolioDisplay() {
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        String targetRole = preferences.getString("targetRole", "General Graduate Role");

        int savedProjectCount = countSavedProjects(preferences);
        int portfolioProgress = calculateOverallPortfolioProgress(preferences, targetRole);
        String displayStatus = getDisplayPortfolioStatus(portfolioProgress, savedProjectCount);

        String githubLink = preferences.getString("githubLink", "");
        String savedProjectsSummary = buildSavedProjectsSummary(preferences);
        String linkAnalysisSummary = generatePortfolioAnalysisSummary(
                preferences,
                targetRole,
                portfolioProgress,
                displayStatus
        );

        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("projectCount", savedProjectCount);
        editor.putInt("portfolioProgress", portfolioProgress);
        editor.putInt("portfolioEvidenceScore", portfolioProgress);
        editor.putString("portfolioEvidenceStatus", displayStatus);
        editor.putString("savedProjectsSummary", savedProjectsSummary);
        editor.putString("linkAnalysisSummary", linkAnalysisSummary);
        editor.putBoolean("portfolioStarted", savedProjectCount > 0);
        editor.apply();

        portfolioProgressText.setText("Portfolio Evidence Status: " + displayStatus);
        portfolioEvidenceText.setText("Projects Added: " + savedProjectCount + "/" + MAX_PROJECTS);

        portfolioMessageText.setText(
                "Next Step: " + getPortfolioNextStep(
                        savedProjectCount,
                        portfolioProgress,
                        targetRole,
                        githubLink
                )
        );

        savedProjectsText.setText(savedProjectsSummary);
        linkAnalysisText.setText(linkAnalysisSummary);
    }

    private boolean isGithubLink(String link) {
        String lowerLink = link.toLowerCase();
        return lowerLink.contains("github.com") && isValidWebLink(link);
    }

    private boolean isLinkedInLink(String link) {
        String lowerLink = link.toLowerCase();
        return lowerLink.contains("linkedin.com") && isValidWebLink(link);
    }

    private boolean isValidWebLink(String link) {
        String lowerLink = link.toLowerCase();

        return lowerLink.startsWith("http://")
                || lowerLink.startsWith("https://")
                || lowerLink.startsWith("www.")
                || lowerLink.contains(".com")
                || lowerLink.contains(".net")
                || lowerLink.contains(".org")
                || lowerLink.contains(".io")
                || lowerLink.contains(".dev");
    }

    private String extractGithubUsername(String githubLink) {
        if (githubLink == null || githubLink.trim().isEmpty()) {
            return "";
        }

        String cleanLink = githubLink.toLowerCase()
                .replace("https://", "")
                .replace("http://", "")
                .replace("www.", "");

        if (!cleanLink.contains("github.com/")) {
            return "";
        }

        String[] parts = cleanLink.split("github.com/");

        if (parts.length < 2) {
            return "";
        }

        String usernamePart = parts[1];

        if (usernamePart.contains("/")) {
            usernamePart = usernamePart.substring(0, usernamePart.indexOf("/"));
        }

        if (usernamePart.contains("?")) {
            usernamePart = usernamePart.substring(0, usernamePart.indexOf("?"));
        }

        return usernamePart.trim();
    }

    private boolean isDeveloperRole(String targetRole) {
        return targetRole.equals("Mobile App Developer")
                || targetRole.equals("Software Developer");
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

    private void goToDashboard() {
        Intent intent = new Intent(PortfolioActivity.this, DashboardActivity.class);
        startActivity(intent);
        finish();
    }
}