package com.monali.careermateai;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CvFeedbackActivity extends AppCompatActivity {

    private TextView cvReadinessText, cvMessageText;
    private TextView selectedFileText, cvTextPreviewText;
    private TextView cvFeedbackText, cvSuggestionsText, detectedSectionsText;

    private TextView atsScoreText, impactScoreText, roleMatchScoreText, toneScoreText;
    private ProgressBar atsProgressBar, impactProgressBar, roleMatchProgressBar, toneProgressBar;

    private Button uploadCvFileButton, analyzeCvButton, backToDashboardButton;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    private static final String PREF_NAME = "CareerMateUser";

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(?:\\+?\\d[\\d\\s().-]{6,}\\d)"
    );

    private static final Pattern METRIC_PATTERN = Pattern.compile(
            "\\b\\d+(?:\\.\\d+)?\\s*(?:%|k|m|\\+|users?|screens?|projects?|weeks?|months?|years?|hours?|seconds?|students?|clients?)?\\b"
                    + "|\\b(one|two|three|four|five|six|seven|eight|nine|ten)\\b"
    );

    private static final String[] ACTION_VERBS = {
            "developed", "designed", "created", "implemented", "built",
            "improved", "analysed", "analyzed", "managed", "led",
            "delivered", "optimized", "tested", "launched", "supported",
            "coordinated", "planned", "researched", "presented", "produced"
    };

    private static final String[] OUTCOME_WORDS = {
            "improved", "increased", "reduced", "saved", "boosted",
            "achieved", "delivered", "users", "performance", "efficiency",
            "accuracy", "result", "outcome", "speed", "time", "growth",
            "completed", "launched", "impact"
    };

    private static final String[] GENERIC_WORDS = {
            "hardworking", "hard working", "self motivated", "self-motivated",
            "team player", "fast learner", "passionate individual",
            "dynamic", "go getter", "reputed company", "challenging position",
            "responsible for", "good communication skills", "career oriented",
            "willing to learn"
    };

    private static final String[] CV_DOCUMENT_KEYWORDS = {
            "curriculum vitae", "resume", "cv", "career objective",
            "professional summary", "profile", "education", "skills",
            "technical skills", "experience", "work experience", "projects",
            "certifications", "achievements", "references", "linkedin",
            "github", "portfolio", "email", "phone", "university",
            "degree", "bachelor", "internship"
    };

    private static final String[] NON_CV_DOCUMENT_KEYWORDS = {
            "presentation roadmap", "slide", "case study requirements",
            "network design", "lan/wan", "vlan", "packet tracer",
            "configuration commands", "implementation plan", "selected references",
            "learning reflection", "testing evidence", "topology",
            "firewall", "ids/ips", "acl", "router", "switch",
            "cabling", "addressing", "research center", "student id",
            "cn6003", "computer and network security", "ieee", "tia-568",
            "ipv4 plan", "floor", "same-subnet", "routed subnet"
    };

    private Uri selectedCvUri;
    private String selectedFileName = "";
    private String extractedCvText = "";

    private ActivityResultLauncher<String[]> cvFilePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cv_feedback);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        PDFBoxResourceLoader.init(getApplicationContext());

        cvReadinessText = findViewById(R.id.cvReadinessText);
        cvMessageText = findViewById(R.id.cvMessageText);

        selectedFileText = findViewById(R.id.selectedFileText);
        cvTextPreviewText = findViewById(R.id.cvTextPreviewText);

        cvFeedbackText = findViewById(R.id.cvFeedbackText);
        cvSuggestionsText = findViewById(R.id.cvSuggestionsText);
        detectedSectionsText = findViewById(R.id.detectedSectionsText);

        atsScoreText = findViewById(R.id.atsScoreText);
        impactScoreText = findViewById(R.id.impactScoreText);
        roleMatchScoreText = findViewById(R.id.roleMatchScoreText);
        toneScoreText = findViewById(R.id.toneScoreText);

        atsProgressBar = findViewById(R.id.atsProgressBar);
        impactProgressBar = findViewById(R.id.impactProgressBar);
        roleMatchProgressBar = findViewById(R.id.roleMatchProgressBar);
        toneProgressBar = findViewById(R.id.toneProgressBar);

        uploadCvFileButton = findViewById(R.id.uploadCvFileButton);
        analyzeCvButton = findViewById(R.id.analyzeCvButton);
        backToDashboardButton = findViewById(R.id.backToDashboardButton);

        setupFilePicker();
        loadSavedCvDataFromLocalCache();
        loadCvResultFromFirestore();
        setupButtons();
    }

    private void setupFilePicker() {
        cvFilePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                new ActivityResultCallback<Uri>() {
                    @Override
                    public void onActivityResult(Uri uri) {
                        if (uri != null) {
                            selectedCvUri = uri;
                            selectedFileName = getFileName(uri);

                            selectedFileText.setText("Selected File: " + selectedFileName);

                            if (selectedFileName.toLowerCase().endsWith(".pdf")) {
                                extractTextFromPdf(uri);
                            } else {
                                extractedCvText = "";
                                cvTextPreviewText.setText("This file type is not supported yet. Please upload a PDF CV for this version.");
                                CustomToast.showInfo(CvFeedbackActivity.this, "Please select a PDF file");
                            }
                        }
                    }
                }
        );
    }

    private void setupButtons() {
        uploadCvFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cvFilePickerLauncher.launch(new String[]{"application/pdf"});
            }
        });

        analyzeCvButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                analyzeCv();
            }
        });

        backToDashboardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goToDashboard();
            }
        });
    }

    private void loadSavedCvDataFromLocalCache() {
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        selectedFileName = preferences.getString("cvFileName", "");
        extractedCvText = preferences.getString("cvExtractedText", "");

        boolean cvCompleted = preferences.getBoolean("cvCompleted", false);

        int cvReadiness = preferences.getInt("cvReadiness", 0);
        int atsScore = preferences.getInt("cvAtsScore", 0);
        int impactScore = preferences.getInt("cvImpactScore", 0);
        int roleMatchScore = preferences.getInt("cvRoleMatchScore", 0);
        int evidenceScore = preferences.getInt("cvToneScore", 0);

        String cvFeedback = preferences.getString("cvFeedback", "Rule-based CV summary will appear after analysis.");
        String cvSuggestions = preferences.getString("cvSuggestions", "Top fix checklist will appear after analysis.");
        String detectedSections = preferences.getString("detectedCvSections", "Detected sections will appear after analysis.");

        if (!selectedFileName.isEmpty()) {
            selectedFileText.setText("Selected File: " + selectedFileName);
        }

        if (!extractedCvText.isEmpty()) {
            cvTextPreviewText.setText(getPreviewText(extractedCvText));
        } else if (cvCompleted) {
            cvTextPreviewText.setText("Previous CV analysis is saved. To re-analyse your CV, please upload the PDF again.");
        }

        updateCvReadinessDisplay(cvReadiness);
        updatePillarDisplays(atsScore, impactScore, roleMatchScore, evidenceScore);

        cvFeedbackText.setText(cvFeedback);
        cvSuggestionsText.setText(cvSuggestions);
        detectedSectionsText.setText(detectedSections);
    }

    private void loadCvResultFromFirestore() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            return;
        }

        firestore.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        cacheCvResultFromFirestore(documentSnapshot);
                        loadSavedCvDataFromLocalCache();
                    }
                })
                .addOnFailureListener(e -> CustomToast.showInfo(
                        this,
                        "Using local CV feedback cache"
                ));
    }

    private void cacheCvResultFromFirestore(DocumentSnapshot documentSnapshot) {
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        if (documentSnapshot.contains("cvFileName")) {
            editor.putString("cvFileName", getStringValue(documentSnapshot, "cvFileName"));
        }

        if (documentSnapshot.contains("cvReadiness")) {
            editor.putInt("cvReadiness", getIntValue(documentSnapshot, "cvReadiness"));
        }

        if (documentSnapshot.contains("cvAtsScore")) {
            editor.putInt("cvAtsScore", getIntValue(documentSnapshot, "cvAtsScore"));
        }

        if (documentSnapshot.contains("cvImpactScore")) {
            editor.putInt("cvImpactScore", getIntValue(documentSnapshot, "cvImpactScore"));
        }

        if (documentSnapshot.contains("cvRoleMatchScore")) {
            editor.putInt("cvRoleMatchScore", getIntValue(documentSnapshot, "cvRoleMatchScore"));
        }

        if (documentSnapshot.contains("cvToneScore")) {
            editor.putInt("cvToneScore", getIntValue(documentSnapshot, "cvToneScore"));
        }

        if (documentSnapshot.contains("cvFeedback")) {
            editor.putString("cvFeedback", getStringValue(documentSnapshot, "cvFeedback"));
        }

        if (documentSnapshot.contains("cvSuggestions")) {
            editor.putString("cvSuggestions", getStringValue(documentSnapshot, "cvSuggestions"));
        }

        if (documentSnapshot.contains("detectedCvSections")) {
            editor.putString("detectedCvSections", getStringValue(documentSnapshot, "detectedCvSections"));
        }

        if (documentSnapshot.contains("cvContactDetailsStatus")) {
            editor.putString("cvContactDetailsStatus", getStringValue(documentSnapshot, "cvContactDetailsStatus"));
        }

        if (documentSnapshot.contains("cvPersonalDetailsFeedback")) {
            editor.putString("cvPersonalDetailsFeedback", getStringValue(documentSnapshot, "cvPersonalDetailsFeedback"));
        }

        if (documentSnapshot.contains("cvCompleted")) {
            Boolean completed = documentSnapshot.getBoolean("cvCompleted");
            editor.putBoolean("cvCompleted", completed != null && completed);
        }

        if (documentSnapshot.contains("careerReadiness")) {
            editor.putInt("careerReadiness", getIntValue(documentSnapshot, "careerReadiness"));
        }

        editor.apply();
    }

    private void extractTextFromPdf(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);

            if (inputStream == null) {
                CustomToast.showError(this, "Unable to open selected file");
                return;
            }

            PDDocument document = PDDocument.load(inputStream);
            PDFTextStripper pdfTextStripper = new PDFTextStripper();

            extractedCvText = pdfTextStripper.getText(document).trim();

            document.close();
            inputStream.close();

            if (extractedCvText.isEmpty()) {
                cvTextPreviewText.setText("No readable text found. This PDF may be scanned or image-based.");
                CustomToast.showInfo(this, "No readable text found in PDF");
                return;
            }

            if (!isLikelyCvDocument(extractedCvText)) {
                rejectInvalidCvFile();
                return;
            }

            selectedFileText.setText("Selected File: " + selectedFileName);
            cvTextPreviewText.setText(getPreviewText(extractedCvText));
            saveExtractedCvTextOnly();
            CustomToast.showSuccess(this, "CV text extracted successfully");

        } catch (Exception e) {
            extractedCvText = "";
            cvTextPreviewText.setText("Error reading PDF file. Please try another PDF CV.");
            CustomToast.showError(this, "PDF reading failed");
        }
    }

    private void saveExtractedCvTextOnly() {
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        editor.putString("cvFileName", selectedFileName);

        /*
         * Full CV text is kept only in local cache for analysis.
         * The Firestore save stores CV analysis results instead of storing the full CV text.
         * This reduces unnecessary cloud storage of sensitive CV content.
         */
        editor.putString("cvExtractedText", extractedCvText);
        editor.apply();
    }

    private void analyzeCv() {
        if (extractedCvText == null || extractedCvText.trim().isEmpty()) {
            SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
            boolean cvCompleted = preferences.getBoolean("cvCompleted", false);

            if (cvCompleted) {
                cvTextPreviewText.setText("Previous CV analysis is saved. To re-analyse your CV, please upload the PDF again.");
                CustomToast.showInfo(
                        this,
                        "Previous CV analysis is saved. To re-analyse your CV, please upload the PDF again."
                );
            } else {
                CustomToast.showInfo(
                        this,
                        "Please upload a readable PDF CV first."
                );
            }

            return;
        }

        if (!isLikelyCvDocument(extractedCvText)) {
            rejectInvalidCvFile();
            return;
        }

        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String targetRole = preferences.getString("targetRole", "General Graduate Role");
        String fullName = preferences.getString("fullName", "");

        String cvText = extractedCvText.toLowerCase();

        boolean hasSummary = hasSummarySection(cvText);
        boolean hasEducation = hasEducationSection(cvText);
        boolean hasSkills = hasSkillsSection(cvText);
        boolean hasExperience = hasExperienceSection(cvText);
        boolean hasAchievements = hasAchievementsSection(cvText);

        boolean hasNameOrHeader = hasNameOrHeader(extractedCvText, fullName);
        boolean hasEmail = hasEmailAddress(extractedCvText);
        boolean hasPhone = hasPhoneNumber(extractedCvText);
        boolean hasLinkedIn = hasLinkedInLink(cvText);
        boolean hasGitHub = hasGitHubLink(cvText);
        boolean hasPortfolioLink = hasPortfolioLink(cvText);

        int contactDetailsScore = calculateContactDetailsScore(
                hasNameOrHeader,
                hasEmail,
                hasPhone,
                hasLinkedIn,
                hasGitHub,
                hasPortfolioLink
        );

        boolean hasGoodLength = extractedCvText.length() >= 1000;

        int meaningfulMetricCount = countMeaningfulMetrics(cvText);
        int genericWordCount = countKeywords(cvText, GENERIC_WORDS);

        int coreBasicsScore = calculateCoreBasicsScore(
                hasSummary,
                hasEducation,
                hasSkills,
                hasExperience,
                contactDetailsScore,
                hasGoodLength
        );

        int atsScore = calculateAtsReadabilityScore(
                cvText,
                hasSummary,
                hasEducation,
                hasSkills,
                hasExperience,
                contactDetailsScore,
                hasGoodLength
        );

        int impactScore = calculateStrictImpactScore(cvText, meaningfulMetricCount);
        int evidenceScore = calculatePortfolioEvidenceScore(cvText, targetRole);
        int roleMatchScore = calculateCareerRoleMatchScore(cvText, targetRole, hasExperience, hasSkills);

        int cvReadiness = calculateStrictOverallCvReadiness(
                impactScore,
                evidenceScore,
                roleMatchScore,
                atsScore,
                coreBasicsScore
        );

        cvReadiness = applyStrictScoreCaps(
                cvReadiness,
                meaningfulMetricCount,
                evidenceScore,
                atsScore,
                roleMatchScore,
                genericWordCount,
                targetRole,
                cvText
        );

        String contactDetailsStatus = getContactDetailsStatus(contactDetailsScore);

        String personalDetailsFeedback = generatePersonalDetailsFeedback(
                hasNameOrHeader,
                hasEmail,
                hasPhone,
                hasLinkedIn,
                hasGitHub,
                hasPortfolioLink,
                contactDetailsScore
        );

        String detectedSections = generateDetectedSections(
                hasSummary,
                hasEducation,
                hasSkills,
                hasExperience,
                hasAchievements,
                hasGoodLength,
                meaningfulMetricCount,
                evidenceScore,
                hasNameOrHeader,
                hasEmail,
                hasPhone,
                hasLinkedIn,
                hasGitHub,
                hasPortfolioLink,
                contactDetailsScore
        );

        String feedback = generateRuleBasedSummary(
                cvReadiness,
                atsScore,
                impactScore,
                roleMatchScore,
                evidenceScore,
                contactDetailsScore,
                targetRole
        );

        String suggestions = generateTopFixChecklist(
                cvText,
                targetRole,
                hasSummary,
                hasEducation,
                hasSkills,
                hasExperience,
                hasAchievements,
                hasGoodLength,
                atsScore,
                impactScore,
                roleMatchScore,
                evidenceScore,
                meaningfulMetricCount,
                genericWordCount,
                hasNameOrHeader,
                hasEmail,
                hasPhone,
                hasLinkedIn,
                hasGitHub,
                hasPortfolioLink,
                contactDetailsScore
        );

        saveCvResult(
                cvReadiness,
                atsScore,
                impactScore,
                roleMatchScore,
                evidenceScore,
                contactDetailsStatus,
                personalDetailsFeedback,
                feedback,
                suggestions,
                detectedSections
        );

        updateCvReadinessDisplay(cvReadiness);
        updatePillarDisplays(atsScore, impactScore, roleMatchScore, evidenceScore);

        cvFeedbackText.setText(feedback + "\n\n" + personalDetailsFeedback);
        cvSuggestionsText.setText(suggestions);
        detectedSectionsText.setText(detectedSections);
    }

    private boolean isLikelyCvDocument(String originalText) {
        if (originalText == null || originalText.trim().isEmpty()) {
            return false;
        }

        String text = originalText.toLowerCase();

        if (text.length() < 250) {
            return false;
        }

        boolean hasEmail = hasEmailAddress(originalText);
        boolean hasPhone = hasPhoneNumber(originalText);
        boolean hasSummary = hasSummarySection(text);
        boolean hasEducation = hasEducationSection(text);
        boolean hasSkills = hasSkillsSection(text);
        boolean hasExperience = hasExperienceSection(text);
        boolean hasLinkedIn = hasLinkedInLink(text);
        boolean hasGitHub = hasGitHubLink(text);
        boolean hasPortfolio = hasPortfolioLink(text);

        boolean hasCvTitle =
                text.contains("curriculum vitae")
                        || text.contains("resume")
                        || text.contains("professional summary")
                        || text.contains("career objective");

        boolean hasContactEvidence =
                hasEmail || hasPhone || hasLinkedIn || hasGitHub || hasPortfolio;

        int nonCvSignalCount = countKeywords(text, NON_CV_DOCUMENT_KEYWORDS);

        /*
         * Academic reports and presentation slides may contain words like university,
         * project, testing and references. These words alone should not pass as a CV.
         */
        if (nonCvSignalCount >= 4 && !hasCvTitle) {
            return false;
        }

        /*
         * A real CV should normally contain contact details or professional profile evidence.
         */
        if (!hasContactEvidence && !hasCvTitle) {
            return false;
        }

        int cvSectionCount = 0;

        if (hasSummary) cvSectionCount++;
        if (hasEducation) cvSectionCount++;
        if (hasSkills) cvSectionCount++;
        if (hasExperience) cvSectionCount++;

        /*
         * A valid CV should contain at least two CV-like sections.
         */
        if (cvSectionCount < 2) {
            return false;
        }

        /*
         * A CV should contain either skills or experience/project evidence.
         */
        if (!hasSkills && !hasExperience) {
            return false;
        }

        int cvKeywordCount = countKeywords(text, CV_DOCUMENT_KEYWORDS);

        int cvScore = 0;

        if (hasEmail) cvScore += 20;
        if (hasPhone) cvScore += 15;
        if (hasLinkedIn) cvScore += 10;
        if (hasGitHub) cvScore += 10;
        if (hasPortfolio) cvScore += 8;
        if (hasSummary) cvScore += 10;
        if (hasEducation) cvScore += 15;
        if (hasSkills) cvScore += 15;
        if (hasExperience) cvScore += 15;

        if (cvKeywordCount >= 6) {
            cvScore += 15;
        } else if (cvKeywordCount >= 3) {
            cvScore += 8;
        }

        return cvScore >= 45;
    }

    private void rejectInvalidCvFile() {
        extractedCvText = "";
        selectedCvUri = null;

        selectedFileText.setText("Rejected File: " + selectedFileName);
        cvTextPreviewText.setText("This file does not look like a CV. Please upload a valid CV PDF with contact details, education, skills, experience, projects or professional profile links.");

        updateCvReadinessDisplay(0);
        updatePillarDisplays(0, 0, 0, 0);

        cvFeedbackText.setText("No CV analysis available because the uploaded file was not recognised as a valid CV.");
        cvSuggestionsText.setText("Please upload a real CV PDF. A valid CV should include contact details, education, skills, experience or project evidence.");
        detectedSectionsText.setText("No valid CV sections detected.");

        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        editor.remove("cvExtractedText");
        editor.putString("cvFileName", "");
        editor.putBoolean("cvCompleted", false);
        editor.putInt("cvReadiness", 0);
        editor.putInt("cvAtsScore", 0);
        editor.putInt("cvImpactScore", 0);
        editor.putInt("cvRoleMatchScore", 0);
        editor.putInt("cvToneScore", 0);
        editor.putString("cvFeedback", "No CV analysis available.");
        editor.putString("cvSuggestions", "Please upload a valid CV PDF.");
        editor.putString("detectedCvSections", "No valid CV sections detected.");
        editor.apply();

        resetInvalidCvResultInFirestore();

        CustomToast.showError(this, "This file does not look like a CV.");
    }

    private void resetInvalidCvResultInFirestore() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            return;
        }

        Map<String, Object> cvData = new HashMap<>();

        cvData.put("cvCompleted", false);
        cvData.put("cvFileName", "");

        cvData.put("cvReadiness", 0);
        cvData.put("cvAtsScore", 0);
        cvData.put("cvImpactScore", 0);
        cvData.put("cvRoleMatchScore", 0);
        cvData.put("cvToneScore", 0);

        cvData.put("cvFeedback", "No CV analysis available.");
        cvData.put("cvSuggestions", "Please upload a valid CV PDF.");
        cvData.put("detectedCvSections", "No valid CV sections detected.");

        cvData.put("updatedAt", FieldValue.serverTimestamp());

        firestore.collection("users")
                .document(currentUser.getUid())
                .set(cvData, SetOptions.merge());
    }

    private int calculateStrictOverallCvReadiness(
            int impactScore,
            int evidenceScore,
            int roleMatchScore,
            int atsScore,
            int coreBasicsScore
    ) {
        int readiness =
                (impactScore * 30) / 100 +
                        (evidenceScore * 25) / 100 +
                        (roleMatchScore * 20) / 100 +
                        (atsScore * 15) / 100 +
                        (coreBasicsScore * 10) / 100;

        return boundScore(readiness);
    }

    private int applyStrictScoreCaps(
            int currentScore,
            int meaningfulMetricCount,
            int evidenceScore,
            int atsScore,
            int roleMatchScore,
            int genericWordCount,
            String targetRole,
            String text
    ) {
        int finalScore = currentScore;

        if (meaningfulMetricCount == 0 && finalScore > 55) {
            finalScore = 55;
        }

        if (evidenceScore < 30 && finalScore > 50) {
            finalScore = 50;
        }

        if (atsScore < 40 && finalScore > 45) {
            finalScore = 45;
        }

        if (roleMatchScore < 35 && finalScore > 60) {
            finalScore = 60;
        }

        if (genericWordCount >= 3 && finalScore > 55) {
            finalScore = 55;
        }

        if (isDeveloperRole(targetRole) && !text.contains("github") && finalScore > 65) {
            finalScore = 65;
        }

        if (extractedCvText.length() < 300 && finalScore > 45) {
            finalScore = 45;
        }

        return boundScore(finalScore);
    }

    private int calculateCoreBasicsScore(
            boolean hasSummary,
            boolean hasEducation,
            boolean hasSkills,
            boolean hasExperience,
            int contactDetailsScore,
            boolean hasGoodLength
    ) {
        int score = 0;

        if (hasSummary) score += 15;
        if (hasEducation) score += 20;
        if (hasSkills) score += 20;
        if (hasExperience) score += 20;

        score += (contactDetailsScore * 15) / 100;

        if (hasGoodLength) score += 10;

        return boundScore(score);
    }

    private int calculateAtsReadabilityScore(
            String text,
            boolean hasSummary,
            boolean hasEducation,
            boolean hasSkills,
            boolean hasExperience,
            int contactDetailsScore,
            boolean hasGoodLength
    ) {
        int score = 0;

        boolean hasReadableHeadings = hasEducation && hasSkills && hasExperience;
        boolean hasEnoughPlainText = text.length() >= 600;

        boolean hasManyLayoutSymbols = countChar(text, '|') > 10
                || countChar(text, '□') > 3
                || countChar(text, '•') > 35;

        boolean hasVeryShortLines = countRegex(text, "\\n.{1,3}\\n") > 10;

        if (hasEnoughPlainText) score += 25;
        if (hasReadableHeadings) score += 25;
        if (contactDetailsScore >= 50) score += 15;
        if (hasSummary || hasEducation) score += 10;
        if (hasGoodLength) score += 10;

        if (!hasManyLayoutSymbols && !hasVeryShortLines) {
            score += 15;
        } else {
            score -= 20;
        }

        return boundScore(score);
    }

    private int calculateStrictImpactScore(String text, int meaningfulMetricCount) {
        int score = 0;

        int actionVerbCount = countKeywords(text, ACTION_VERBS);
        int outcomeWordCount = countKeywords(text, OUTCOME_WORDS);

        if (meaningfulMetricCount >= 4) {
            score += 55;
        } else if (meaningfulMetricCount >= 2) {
            score += 40;
        } else if (meaningfulMetricCount == 1) {
            score += 22;
        }

        if (actionVerbCount >= 6) {
            score += 20;
        } else if (actionVerbCount >= 3) {
            score += 14;
        } else if (actionVerbCount >= 1) {
            score += 6;
        }

        if (outcomeWordCount >= 4) {
            score += 15;
        } else if (outcomeWordCount >= 2) {
            score += 10;
        } else if (outcomeWordCount >= 1) {
            score += 5;
        }

        if (hasExperienceSection(text)) {
            score += 10;
        }

        if (meaningfulMetricCount == 0 && score > 35) {
            score = 35;
        }

        return boundScore(score);
    }

    private int calculatePortfolioEvidenceScore(String text, String targetRole) {
        int score = 0;

        boolean hasGitHub = text.contains("github.com") || text.contains("github");
        boolean hasLinkedIn = text.contains("linkedin.com") || text.contains("linkedin");
        boolean hasPortfolio = text.contains("portfolio") || text.contains("behance")
                || text.contains("dribbble") || text.contains("website")
                || text.contains("http://") || text.contains("https://");
        boolean hasTools = hasToolEvidence(text);
        boolean hasProject = hasExperienceSection(text);

        if (hasGitHub) {
            score += isDeveloperRole(targetRole) ? 40 : 25;
        }

        if (hasLinkedIn) {
            score += 15;
        }

        if (hasPortfolio) {
            score += 20;
        }

        if (hasTools) {
            score += 15;
        }

        if (hasProject) {
            score += 10;
        }

        return boundScore(score);
    }

    private boolean hasToolEvidence(String text) {
        String[] tools = {
                "java", "android", "kotlin", "firebase", "figma", "sql",
                "python", "excel", "power bi", "tableau", "canva",
                "react", "html", "css", "javascript", "github",
                "android studio", "api", "database"
        };

        return countKeywords(text, tools) >= 2;
    }

    private int calculateCareerRoleMatchScore(
            String text,
            String targetRole,
            boolean hasExperience,
            boolean hasSkills
    ) {
        String[] keywords = getRoleKeywords(targetRole);

        int matchedKeywords = 0;

        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase())) {
                matchedKeywords++;
            }
        }

        int score = 0;

        if (keywords.length > 0) {
            score += (matchedKeywords * 75) / keywords.length;
        }

        if (hasSkills) score += 10;
        if (hasExperience) score += 10;

        if (text.contains("github") || text.contains("portfolio") || text.contains("linkedin")) {
            score += 5;
        }

        return boundScore(score);
    }

    private int calculateContactDetailsScore(
            boolean hasNameOrHeader,
            boolean hasEmail,
            boolean hasPhone,
            boolean hasLinkedIn,
            boolean hasGitHub,
            boolean hasPortfolioLink
    ) {
        int score = 0;

        if (hasNameOrHeader) score += 20;
        if (hasEmail) score += 25;
        if (hasPhone) score += 25;
        if (hasLinkedIn) score += 15;

        if (hasGitHub || hasPortfolioLink) {
            score += 15;
        }

        return boundScore(score);
    }

    private boolean hasNameOrHeader(String originalText, String savedFullName) {
        String lowerText = originalText.toLowerCase();

        if (savedFullName != null && !savedFullName.trim().isEmpty()) {
            if (lowerText.contains(savedFullName.toLowerCase().trim())) {
                return true;
            }
        }

        String[] lines = originalText.split("\\n");

        for (int i = 0; i < lines.length && i < 5; i++) {
            String cleanLine = lines[i].trim();

            if (cleanLine.length() >= 5
                    && cleanLine.length() <= 60
                    && cleanLine.matches(".*[A-Za-z].*")
                    && !cleanLine.contains("@")
                    && !cleanLine.toLowerCase().contains("curriculum")
                    && !cleanLine.toLowerCase().contains("resume")
                    && !cleanLine.toLowerCase().contains("cv")) {

                String[] words = cleanLine.split("\\s+");

                if (words.length >= 2) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean hasEmailAddress(String originalText) {
        Matcher matcher = EMAIL_PATTERN.matcher(originalText);
        return matcher.find();
    }

    private boolean hasPhoneNumber(String originalText) {
        Matcher matcher = PHONE_PATTERN.matcher(originalText);

        while (matcher.find()) {
            String match = matcher.group();
            String digitsOnly = match.replaceAll("\\D", "");

            if (digitsOnly.length() >= 7 && digitsOnly.length() <= 15) {
                return true;
            }
        }

        return false;
    }

    private boolean hasLinkedInLink(String lowerText) {
        return lowerText.contains("linkedin.com") || lowerText.contains("linkedin");
    }

    private boolean hasGitHubLink(String lowerText) {
        return lowerText.contains("github.com") || lowerText.contains("github");
    }

    private boolean hasPortfolioLink(String lowerText) {
        return lowerText.contains("portfolio")
                || lowerText.contains("website")
                || lowerText.contains("behance")
                || lowerText.contains("dribbble")
                || lowerText.contains(".dev")
                || lowerText.contains(".io");
    }

    private String getContactDetailsStatus(int contactDetailsScore) {
        if (contactDetailsScore < 40) {
            return "Weak Contact Details";
        } else if (contactDetailsScore < 70) {
            return "Basic Contact Details";
        } else if (contactDetailsScore < 90) {
            return "Good Contact Details";
        } else {
            return "Strong Contact Details";
        }
    }

    private String generatePersonalDetailsFeedback(
            boolean hasNameOrHeader,
            boolean hasEmail,
            boolean hasPhone,
            boolean hasLinkedIn,
            boolean hasGitHub,
            boolean hasPortfolioLink,
            int contactDetailsScore
    ) {
        StringBuilder feedback = new StringBuilder();

        feedback.append("Personal / Contact Details Review: ")
                .append(getContactDetailsStatus(contactDetailsScore))
                .append("\n");

        feedback.append(hasNameOrHeader ? "✓ " : "✗ ").append("Name or clear CV header\n");
        feedback.append(hasEmail ? "✓ " : "✗ ").append("Email address\n");
        feedback.append(hasPhone ? "✓ " : "✗ ").append("Phone number\n");
        feedback.append(hasLinkedIn ? "✓ " : "✗ ").append("LinkedIn profile\n");
        feedback.append(hasGitHub ? "✓ " : "✗ ").append("GitHub profile\n");
        feedback.append(hasPortfolioLink ? "✓ " : "✗ ").append("Portfolio or personal website\n");
        feedback.append("○ CV photo is optional and depends on the country, company and job application requirement.");

        return feedback.toString().trim();
    }

    private int countMeaningfulMetrics(String text) {
        int count = 0;

        Matcher matcher = METRIC_PATTERN.matcher(text);

        while (matcher.find()) {
            String metric = matcher.group();
            int start = Math.max(0, matcher.start() - 45);
            int end = Math.min(text.length(), matcher.end() + 45);
            String nearbyText = text.substring(start, end);

            if (isLikelyPhoneNumberOrYear(metric, nearbyText)) {
                continue;
            }

            if (containsAny(nearbyText, OUTCOME_WORDS)
                    || nearbyText.contains("screens")
                    || nearbyText.contains("users")
                    || nearbyText.contains("projects")
                    || nearbyText.contains("weeks")
                    || nearbyText.contains("months")
                    || nearbyText.contains("performance")
                    || nearbyText.contains("accuracy")
                    || nearbyText.contains("reduced")
                    || nearbyText.contains("increased")
                    || metric.contains("%")) {
                count++;
            }
        }

        return count;
    }

    private boolean isLikelyPhoneNumberOrYear(String metric, String nearbyText) {
        String digitsOnly = metric.replaceAll("\\D", "");

        if (digitsOnly.length() >= 7 && !metric.contains("%")) {
            return true;
        }

        if (digitsOnly.length() == 4) {
            try {
                int year = Integer.parseInt(digitsOnly);

                if (year >= 1900 && year <= 2099
                        && !nearbyText.contains("years")
                        && !nearbyText.contains("months")
                        && !nearbyText.contains("weeks")
                        && !nearbyText.contains("users")
                        && !nearbyText.contains("screens")) {
                    return true;
                }
            } catch (Exception ignored) {
            }
        }

        return false;
    }

    private boolean containsAny(String text, String[] keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    private boolean isDeveloperRole(String targetRole) {
        return targetRole.equals("Mobile App Developer")
                || targetRole.equals("Software Developer");
    }

    private String[] getRoleKeywords(String targetRole) {
        if (targetRole.equals("Mobile App Developer")) {
            return new String[]{
                    "android", "java", "kotlin", "xml", "firebase",
                    "mobile app", "api", "ui", "github", "debugging",
                    "testing", "android studio"
            };

        } else if (targetRole.equals("Software Developer")) {
            return new String[]{
                    "java", "python", "sql", "database", "api",
                    "github", "debugging", "testing", "software",
                    "programming", "problem solving"
            };

        } else if (targetRole.equals("Data Analyst")) {
            return new String[]{
                    "excel", "sql", "python", "data analysis", "dashboard",
                    "visualisation", "visualization", "report", "statistics",
                    "power bi", "tableau"
            };

        } else if (targetRole.equals("Business Analyst")) {
            return new String[]{
                    "business analysis", "requirements", "stakeholder",
                    "excel", "report", "process", "documentation",
                    "data analysis", "presentation", "communication"
            };

        } else if (targetRole.equals("Marketing Executive")) {
            return new String[]{
                    "marketing", "campaign", "social media", "content",
                    "seo", "digital marketing", "analytics", "branding",
                    "canva", "communication"
            };

        } else if (targetRole.equals("UI UX Designer")) {
            return new String[]{
                    "figma", "ui", "ux", "prototype", "wireframe",
                    "user research", "design", "usability", "canva",
                    "portfolio"
            };

        } else if (targetRole.equals("Project Coordinator")) {
            return new String[]{
                    "project", "coordination", "planning", "timeline",
                    "stakeholder", "communication", "teamwork", "report",
                    "leadership", "risk"
            };
        }

        return new String[]{
                "communication", "teamwork", "problem solving",
                "project", "leadership", "report", "presentation",
                "skills", "experience"
        };
    }

    private boolean hasSummarySection(String text) {
        return text.contains("summary")
                || text.contains("profile")
                || text.contains("objective")
                || text.contains("about me")
                || text.contains("career objective");
    }

    private boolean hasEducationSection(String text) {
        return text.contains("education")
                || text.contains("university")
                || text.contains("college")
                || text.contains("bsc")
                || text.contains("bachelor")
                || text.contains("degree")
                || text.contains("diploma");
    }

    private boolean hasSkillsSection(String text) {
        return text.contains("skills")
                || text.contains("technical skills")
                || text.contains("java")
                || text.contains("python")
                || text.contains("sql")
                || text.contains("android")
                || text.contains("firebase")
                || text.contains("communication");
    }

    private boolean hasExperienceSection(String text) {
        return text.contains("experience")
                || text.contains("projects")
                || text.contains("project")
                || text.contains("internship")
                || text.contains("developed")
                || text.contains("designed")
                || text.contains("implemented")
                || text.contains("created");
    }

    private boolean hasAchievementsSection(String text) {
        return text.contains("achievement")
                || text.contains("achievements")
                || text.contains("certification")
                || text.contains("certificate")
                || text.contains("award")
                || text.contains("honour")
                || text.contains("first class");
    }

    private String generateRuleBasedSummary(
            int cvReadiness,
            int atsScore,
            int impactScore,
            int roleMatchScore,
            int evidenceScore,
            int contactDetailsScore,
            String targetRole
    ) {
        String weakestPillar = getWeakestPillarName(
                atsScore,
                impactScore,
                roleMatchScore,
                evidenceScore,
                contactDetailsScore
        );

        if (cvReadiness < 50) {
            return "Your CV has a basic structure, but it is not yet strong for a competitive graduate job market. Focus first on " + weakestPillar + " and add stronger proof for your target role: " + targetRole + ".";
        } else if (cvReadiness < 70) {
            return "Your CV has useful information, but it still needs stronger evidence, clearer role match, contact clarity and measurable project results. The most important area to fix first is " + weakestPillar + ".";
        } else if (cvReadiness < 85) {
            return "Your CV is developing well and includes several important career sections. To make it stronger internationally, improve " + weakestPillar + " and add more measurable evidence.";
        } else {
            return "Your CV looks strong and career-ready in many areas. Your next improvement is to polish " + weakestPillar + " so recruiters can quickly see your evidence and value.";
        }
    }

    private String generateTopFixChecklist(
            String text,
            String targetRole,
            boolean hasSummary,
            boolean hasEducation,
            boolean hasSkills,
            boolean hasExperience,
            boolean hasAchievements,
            boolean hasGoodLength,
            int atsScore,
            int impactScore,
            int roleMatchScore,
            int evidenceScore,
            int meaningfulMetricCount,
            int genericWordCount,
            boolean hasNameOrHeader,
            boolean hasEmail,
            boolean hasPhone,
            boolean hasLinkedIn,
            boolean hasGitHub,
            boolean hasPortfolioLink,
            int contactDetailsScore
    ) {
        StringBuilder checklist = new StringBuilder();

        if (!hasNameOrHeader) {
            checklist.append("[ ] Add your full name clearly at the top of the CV (+5 points)\n");
        }

        if (!hasEmail) {
            checklist.append("[ ] Add a professional email address (+6 points)\n");
        }

        if (!hasPhone) {
            checklist.append("[ ] Add a phone number with country code if applying internationally (+6 points)\n");
        }

        if (!hasLinkedIn) {
            checklist.append("[ ] Add a LinkedIn profile link if available (+4 points)\n");
        }

        if (isDeveloperRole(targetRole) && !hasGitHub) {
            checklist.append("[ ] Add a GitHub link because recruiters expect project evidence for developer roles (+8 points)\n");
        }

        if (!hasPortfolioLink && isDeveloperRole(targetRole)) {
            checklist.append("[ ] Add a portfolio website or project link if available (+5 points)\n");
        }

        if (!hasSummary) {
            checklist.append("[ ] Add a short professional summary targeted to ").append(targetRole).append(" (+8 points)\n");
        }

        if (!hasEducation) {
            checklist.append("[ ] Add an Education heading with degree, university and academic year (+8 points)\n");
        }

        if (!hasSkills) {
            checklist.append("[ ] Add a clear Skills section with technical and soft skills (+10 points)\n");
        }

        if (!hasExperience) {
            checklist.append("[ ] Add project, internship or work experience details (+12 points)\n");
        }

        if (!hasAchievements) {
            checklist.append("[ ] Add achievements, certifications or academic results if available (+5 points)\n");
        }

        if (!hasGoodLength) {
            checklist.append("[ ] Add more useful project and experience details so the CV has enough content (+5 points)\n");
        }

        if (meaningfulMetricCount == 0 || impactScore < 50) {
            checklist.append("[ ] Add measurable results to your project, such as users, screens built, time saved, accuracy, performance or percentage improvement (+10 points)\n");
        }

        if (evidenceScore < 50) {
            checklist.append("[ ] Add GitHub, LinkedIn, portfolio or project link evidence where possible (+8 points)\n");
        }

        if (roleMatchScore < 60) {
            checklist.append("[ ] Add more keywords and project evidence related to ").append(targetRole).append(" (+10 points)\n");
        }

        if (atsScore < 70) {
            checklist.append("[ ] Use simple headings such as Education, Skills, Projects and Experience for better ATS readability (+8 points)\n");
        }

        if (genericWordCount >= 2) {
            checklist.append("[ ] Replace generic phrases like hardworking, team player or reputed company with specific evidence (+6 points)\n");
        }

        checklist.append("[ ] Use action verbs such as developed, designed, implemented, analysed and improved\n");
        checklist.append("[ ] Keep the CV clear, short and easy to scan on both ATS and human view\n");
        checklist.append("[ ] CV photo is optional. Add it only if it is suitable for the country, company or job application requirement.");

        return checklist.toString().trim();
    }

    private String generateDetectedSections(
            boolean hasSummary,
            boolean hasEducation,
            boolean hasSkills,
            boolean hasExperience,
            boolean hasAchievements,
            boolean hasGoodLength,
            int meaningfulMetricCount,
            int evidenceScore,
            boolean hasNameOrHeader,
            boolean hasEmail,
            boolean hasPhone,
            boolean hasLinkedIn,
            boolean hasGitHub,
            boolean hasPortfolioLink,
            int contactDetailsScore
    ) {
        StringBuilder result = new StringBuilder();

        result.append(hasNameOrHeader ? "✓ " : "✗ ").append("Name / CV Header\n");
        result.append(hasEmail ? "✓ " : "✗ ").append("Email Address\n");
        result.append(hasPhone ? "✓ " : "✗ ").append("Phone Number\n");
        result.append(hasLinkedIn ? "✓ " : "✗ ").append("LinkedIn Link\n");
        result.append(hasGitHub ? "✓ " : "✗ ").append("GitHub Link\n");
        result.append(hasPortfolioLink ? "✓ " : "✗ ").append("Portfolio / Website Link\n");
        result.append(contactDetailsScore >= 70 ? "✓ " : "✗ ").append("Overall Contact Details Strength\n\n");

        result.append(hasSummary ? "✓ " : "✗ ").append("Professional Summary\n");
        result.append(hasEducation ? "✓ " : "✗ ").append("Education\n");
        result.append(hasSkills ? "✓ " : "✗ ").append("Skills\n");
        result.append(hasExperience ? "✓ " : "✗ ").append("Experience / Projects\n");
        result.append(hasAchievements ? "✓ " : "✗ ").append("Achievements / Certifications\n");
        result.append(hasGoodLength ? "✓ " : "✗ ").append("Enough CV Content\n");
        result.append(meaningfulMetricCount > 0 ? "✓ " : "✗ ").append("Measurable Impact Evidence\n");
        result.append(evidenceScore >= 40 ? "✓ " : "✗ ").append("Portfolio / Link Evidence\n");
        result.append("○ CV Photo: Optional depending on country and employer requirement");

        return result.toString();
    }

    private String getWeakestPillarName(
            int atsScore,
            int impactScore,
            int roleMatchScore,
            int evidenceScore,
            int contactDetailsScore
    ) {
        int weakestScore = atsScore;
        String weakestName = "ATS Readability";

        if (impactScore < weakestScore) {
            weakestScore = impactScore;
            weakestName = "Impact & Numbers";
        }

        if (roleMatchScore < weakestScore) {
            weakestScore = roleMatchScore;
            weakestName = "Career Role Match";
        }

        if (evidenceScore < weakestScore) {
            weakestScore = evidenceScore;
            weakestName = "Portfolio Evidence";
        }

        if (contactDetailsScore < weakestScore) {
            weakestName = "Personal / Contact Details";
        }

        return weakestName;
    }

    private void saveCvResult(
            int cvReadiness,
            int atsScore,
            int impactScore,
            int roleMatchScore,
            int evidenceScore,
            String contactDetailsStatus,
            String personalDetailsFeedback,
            String feedback,
            String suggestions,
            String detectedSections
    ) {
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        editor.putString("cvFileName", selectedFileName);

        editor.putInt("cvReadiness", cvReadiness);
        editor.putInt("cvAtsScore", atsScore);
        editor.putInt("cvImpactScore", impactScore);
        editor.putInt("cvRoleMatchScore", roleMatchScore);

        // Reusing old storage key to avoid changing XML and saved data structure.
        editor.putInt("cvToneScore", evidenceScore);

        editor.putString("cvContactDetailsStatus", contactDetailsStatus);
        editor.putString("cvPersonalDetailsFeedback", personalDetailsFeedback);

        editor.putString("cvFeedback", feedback + "\n\n" + personalDetailsFeedback);
        editor.putString("cvSuggestions", suggestions);
        editor.putString("detectedCvSections", detectedSections);
        editor.putBoolean("cvCompleted", true);

        int profileCompletion = preferences.getInt("profileCompletion", 0);
        int skillMatch = preferences.getInt(
                "skillMatch",
                preferences.getInt("claimedSkillCoverage", 0)
        );
        int portfolioProgress = preferences.getInt("portfolioProgress", 0);
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

        saveCvResultToFirestore(
                cvReadiness,
                atsScore,
                impactScore,
                roleMatchScore,
                evidenceScore,
                contactDetailsStatus,
                personalDetailsFeedback,
                feedback + "\n\n" + personalDetailsFeedback,
                suggestions,
                detectedSections,
                careerReadiness
        );
    }

    private void saveCvResultToFirestore(
            int cvReadiness,
            int atsScore,
            int impactScore,
            int roleMatchScore,
            int evidenceScore,
            String contactDetailsStatus,
            String personalDetailsFeedback,
            String feedback,
            String suggestions,
            String detectedSections,
            int careerReadiness
    ) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            CustomToast.showError(this, "CV analysis saved locally. Please login again for cloud save.");
            return;
        }

        Map<String, Object> cvData = new HashMap<>();

        cvData.put("cvCompleted", true);
        cvData.put("cvFileName", selectedFileName);

        cvData.put("cvReadiness", cvReadiness);
        cvData.put("cvAtsScore", atsScore);
        cvData.put("cvImpactScore", impactScore);
        cvData.put("cvRoleMatchScore", roleMatchScore);
        cvData.put("cvToneScore", evidenceScore);

        cvData.put("cvContactDetailsStatus", contactDetailsStatus);
        cvData.put("cvPersonalDetailsFeedback", personalDetailsFeedback);

        cvData.put("cvFeedback", feedback);
        cvData.put("cvSuggestions", suggestions);
        cvData.put("detectedCvSections", detectedSections);

        cvData.put("careerReadiness", careerReadiness);
        cvData.put("updatedAt", FieldValue.serverTimestamp());

        analyzeCvButton.setEnabled(false);
        analyzeCvButton.setText("Saving...");

        firestore.collection("users")
                .document(currentUser.getUid())
                .set(cvData, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    analyzeCvButton.setEnabled(true);
                    analyzeCvButton.setText("Analyze CV");
                    CustomToast.showSuccess(this, "CV analysis saved successfully");
                })
                .addOnFailureListener(e -> {
                    analyzeCvButton.setEnabled(true);
                    analyzeCvButton.setText("Analyze CV");
                    CustomToast.showError(this, "CV saved locally, but cloud save failed");
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

    private void updateCvReadinessDisplay(int cvReadiness) {
        cvReadinessText.setText("CV Readiness: " + cvReadiness + "/100");

        if (cvReadiness == 0) {
            cvMessageText.setText("Upload and analyze your CV to check your readiness.");
        } else if (cvReadiness < 50) {
            cvMessageText.setText("Your CV needs stronger evidence, measurable impact, contact clarity and target-role wording.");
        } else if (cvReadiness < 70) {
            cvMessageText.setText("Your CV is developing, but it still needs clearer proof and stronger relevance.");
        } else if (cvReadiness < 85) {
            cvMessageText.setText("Your CV is good. Improve measurable impact and portfolio evidence.");
        } else {
            cvMessageText.setText("Strong CV readiness. Keep polishing it for international job applications.");
        }
    }

    private void updatePillarDisplays(
            int atsScore,
            int impactScore,
            int roleMatchScore,
            int evidenceScore
    ) {
        setPillarDisplay(
                atsScoreText,
                atsProgressBar,
                "ATS Readability",
                atsScore
        );

        setPillarDisplay(
                impactScoreText,
                impactProgressBar,
                "Impact & Numbers",
                impactScore
        );

        setPillarDisplay(
                roleMatchScoreText,
                roleMatchProgressBar,
                "Career Role Match",
                roleMatchScore
        );

        setPillarDisplay(
                toneScoreText,
                toneProgressBar,
                "Portfolio Evidence",
                evidenceScore
        );
    }

    private void setPillarDisplay(
            TextView scoreText,
            ProgressBar progressBar,
            String title,
            int score
    ) {
        String status = getStatusLabel(score);
        int color = getColorForScore(score);

        scoreText.setText(title + ": " + score + "% - " + status);
        scoreText.setTextColor(color);

        progressBar.setProgress(score);
        progressBar.setProgressTintList(ColorStateList.valueOf(color));
    }

    private String getStatusLabel(int score) {
        if (score == 0) {
            return "Not analyzed";
        } else if (score < 50) {
            return "Critical Action Needed";
        } else if (score < 70) {
            return "Needs Improvement";
        } else if (score < 85) {
            return "Good";
        } else {
            return "Excellent";
        }
    }

    private int getColorForScore(int score) {
        if (score == 0) {
            return Color.parseColor("#8FA5C0");
        } else if (score < 50) {
            return Color.parseColor("#D64545");
        } else if (score < 70) {
            return Color.parseColor("#E68A00");
        } else {
            return Color.parseColor("#1F8A5B");
        }
    }

    private int countKeywords(String text, String[] keywords) {
        int count = 0;

        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase())) {
                count++;
            }
        }

        return count;
    }

    private int countRegex(String text, String regex) {
        int count = 0;

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            count++;
        }

        return count;
    }

    private int countChar(String text, char targetChar) {
        int count = 0;

        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == targetChar) {
                count++;
            }
        }

        return count;
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

    private String getFileName(Uri uri) {
        String fileName = "Selected CV";

        Cursor cursor = getContentResolver().query(uri, null, null, null, null);

        if (cursor != null) {
            try {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);

                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    fileName = cursor.getString(nameIndex);
                }
            } finally {
                cursor.close();
            }
        }

        return fileName;
    }

    private String getPreviewText(String fullText) {
        if (fullText.length() > 1500) {
            return fullText.substring(0, 1500) + "\n\n... Preview shortened. Full CV text saved locally for analysis.";
        }

        return fullText;
    }

    private void goToDashboard() {
        Intent intent = new Intent(CvFeedbackActivity.this, DashboardActivity.class);
        startActivity(intent);
        finish();
    }
}