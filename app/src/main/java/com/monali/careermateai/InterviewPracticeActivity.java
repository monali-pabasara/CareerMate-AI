package com.monali.careermateai;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InterviewPracticeActivity extends AppCompatActivity {

    private TextView questionCounterText, questionText, feedbackText, answerPreviewText;
    private EditText answerInput;
    private Button submitAnswerButton, nextQuestionButton, backToDashboardButton;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    private static final String PREF_NAME = "CareerMateUser";

    private final List<String> interviewQuestions = new ArrayList<>();

    private String degreeProgramme;
    private String targetRole;
    private String selectedSkills;
    private String missingSkills;
    private String suggestedJobRole;

    private int cvReadiness;
    private int portfolioProgress;
    private int skillMatch;
    private int currentQuestionIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_interview_practice);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        questionCounterText = findViewById(R.id.questionCounterText);
        questionText = findViewById(R.id.questionText);
        feedbackText = findViewById(R.id.feedbackText);
        answerPreviewText = findViewById(R.id.answerPreviewText);

        answerInput = findViewById(R.id.answerInput);

        submitAnswerButton = findViewById(R.id.submitAnswerButton);
        nextQuestionButton = findViewById(R.id.nextQuestionButton);
        backToDashboardButton = findViewById(R.id.backToDashboardButton);

        loadUserCareerDataFromLocalCache();
        buildInterviewQuestions();
        showCurrentQuestion();
        loadLastInterviewResultFromLocalCache();
        loadInterviewResultFromFirestore();

        submitAnswerButton.setOnClickListener(view -> checkAnswer());
        nextQuestionButton.setOnClickListener(view -> showNextQuestion());

        backToDashboardButton.setOnClickListener(view -> {
            Intent intent = new Intent(InterviewPracticeActivity.this, DashboardActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void loadUserCareerDataFromLocalCache() {
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        degreeProgramme = preferences.getString("degreeProgramme", "Not selected");
        targetRole = preferences.getString("targetRole", "Not selected");
        selectedSkills = preferences.getString("selectedSkills", "");
        missingSkills = preferences.getString("missingSkills", "");
        suggestedJobRole = preferences.getString("suggestedJobRole", "");

        cvReadiness = preferences.getInt("cvReadiness", 0);
        portfolioProgress = preferences.getInt("portfolioProgress", 0);
        skillMatch = preferences.getInt(
                "skillMatch",
                preferences.getInt("claimedSkillCoverage", 0)
        );

        if (suggestedJobRole == null || suggestedJobRole.trim().isEmpty()) {
            suggestedJobRole = targetRole;
        }
    }

    private void loadInterviewResultFromFirestore() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            return;
        }

        firestore.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        cacheInterviewDataFromFirestore(documentSnapshot);

                        loadUserCareerDataFromLocalCache();
                        buildInterviewQuestions();
                        showCurrentQuestion();
                        loadLastInterviewResultFromLocalCache();
                    }
                })
                .addOnFailureListener(e -> CustomToast.showInfo(
                        this,
                        "Using local interview practice cache"
                ));
    }

    private void cacheInterviewDataFromFirestore(DocumentSnapshot documentSnapshot) {
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

        if (documentSnapshot.contains("suggestedJobRole")) {
            editor.putString("suggestedJobRole", getStringValue(documentSnapshot, "suggestedJobRole"));
        }

        if (documentSnapshot.contains("cvReadiness")) {
            editor.putInt("cvReadiness", getIntValue(documentSnapshot, "cvReadiness"));
        }

        if (documentSnapshot.contains("portfolioProgress")) {
            editor.putInt("portfolioProgress", getIntValue(documentSnapshot, "portfolioProgress"));
        }

        if (documentSnapshot.contains("skillMatch")) {
            editor.putInt("skillMatch", getIntValue(documentSnapshot, "skillMatch"));
        }

        if (documentSnapshot.contains("interviewCompleted")) {
            Boolean completed = documentSnapshot.getBoolean("interviewCompleted");
            editor.putBoolean("interviewCompleted", completed != null && completed);
        }

        if (documentSnapshot.contains("interviewPracticeOnly")) {
            Boolean practiceOnly = documentSnapshot.getBoolean("interviewPracticeOnly");
            editor.putBoolean("interviewPracticeOnly", practiceOnly != null && practiceOnly);
        }

        if (documentSnapshot.contains("lastInterviewQuestion")) {
            editor.putString("lastInterviewQuestion", getStringValue(documentSnapshot, "lastInterviewQuestion"));
        }

        if (documentSnapshot.contains("lastInterviewAnswer")) {
            editor.putString("lastInterviewAnswer", getStringValue(documentSnapshot, "lastInterviewAnswer"));
        }

        if (documentSnapshot.contains("lastInterviewFeedback")) {
            editor.putString("lastInterviewFeedback", getStringValue(documentSnapshot, "lastInterviewFeedback"));
        }

        if (documentSnapshot.contains("interviewStrengths")) {
            editor.putString("interviewStrengths", getStringValue(documentSnapshot, "interviewStrengths"));
        }

        if (documentSnapshot.contains("interviewImprovements")) {
            editor.putString("interviewImprovements", getStringValue(documentSnapshot, "interviewImprovements"));
        }

        if (documentSnapshot.contains("sampleImprovedAnswer")) {
            editor.putString("sampleImprovedAnswer", getStringValue(documentSnapshot, "sampleImprovedAnswer"));
        }

        if (documentSnapshot.contains("interviewPracticeTip")) {
            editor.putString("interviewPracticeTip", getStringValue(documentSnapshot, "interviewPracticeTip"));
        }

        /*
         * Interview Practice is now practice-only.
         * It should not add marks to dashboard career readiness.
         */
        editor.putInt("interviewReadiness", 0);

        editor.apply();
    }

    private void buildInterviewQuestions() {
        interviewQuestions.clear();

        interviewQuestions.add("Tell me about yourself and your interest in " + targetRole + ".");
        interviewQuestions.add("Why do you want to work as a " + targetRole + "?");

        if (targetRole.equals("Mobile App Developer")) {
            interviewQuestions.add("Describe one mobile app project you have worked on.");
            interviewQuestions.add("How do you design a mobile app screen that is easy for users to understand?");
            interviewQuestions.add("How do you debug an Android app when something is not working?");
            interviewQuestions.add("What Android tools, languages or frameworks have you used?");

        } else if (targetRole.equals("Software Developer")) {
            interviewQuestions.add("Describe one software project you have worked on.");
            interviewQuestions.add("How do you solve programming problems?");
            interviewQuestions.add("How do you use GitHub or an online portfolio to show your coding work?");
            interviewQuestions.add("How do you test and improve your code?");

        } else if (targetRole.equals("Data Analyst")) {
            interviewQuestions.add("Describe how you would analyse a dataset.");
            interviewQuestions.add("What tools would you use for data analysis and reporting?");
            interviewQuestions.add("How would you explain data insights to a non-technical person?");
            interviewQuestions.add("How do you make sure your data analysis is accurate?");

        } else if (targetRole.equals("Business Analyst")) {
            interviewQuestions.add("How would you understand a business problem before suggesting a solution?");
            interviewQuestions.add("How do you communicate requirements to technical and non-technical people?");
            interviewQuestions.add("Describe a time you used analysis to make a better decision.");
            interviewQuestions.add("How would you collect requirements from users or stakeholders?");

        } else if (targetRole.equals("Marketing Executive")) {
            interviewQuestions.add("How would you plan a simple digital marketing campaign?");
            interviewQuestions.add("How would you measure if a marketing campaign is successful?");
            interviewQuestions.add("What social media or design skills can help you in marketing?");
            interviewQuestions.add("How would you understand a target audience?");

        } else if (targetRole.equals("UI UX Designer")) {
            interviewQuestions.add("How do you design a user-friendly app or website screen?");
            interviewQuestions.add("How do you use tools like Figma or Canva in your design process?");
            interviewQuestions.add("How do you improve a design after receiving user feedback?");
            interviewQuestions.add("How do you understand user needs before designing?");

        } else if (targetRole.equals("Project Coordinator")) {
            interviewQuestions.add("How do you manage tasks and deadlines in a project?");
            interviewQuestions.add("How do you communicate with team members during a project?");
            interviewQuestions.add("How would you handle a project delay?");
            interviewQuestions.add("How do you organise project tasks and responsibilities?");

        } else {
            interviewQuestions.add("Describe one university project or experience that shows your skills.");
            interviewQuestions.add("What are your strongest skills as a graduate?");
            interviewQuestions.add("What skills do you still need to improve?");
            interviewQuestions.add("How do you prepare yourself for graduate job opportunities?");
        }

        if (skillMatch < 70) {
            interviewQuestions.add("What skills are you currently improving for your target role?");
        } else {
            interviewQuestions.add("How do your current skills make you suitable for your target role?");
        }

        if (cvReadiness == 0) {
            interviewQuestions.add("How are you planning to improve your CV before applying for jobs?");
        } else {
            interviewQuestions.add("How does your CV show that you are suitable for " + targetRole + "?");
        }

        if (portfolioProgress < 70) {
            interviewQuestions.add("How will you improve your portfolio to support your job applications?");
        } else {
            interviewQuestions.add("How does your portfolio prove your practical skills?");
        }

        interviewQuestions.add("Why should a company hire you for a " + suggestedJobRole + " role?");

        while (interviewQuestions.size() > 10) {
            interviewQuestions.remove(interviewQuestions.size() - 1);
        }

        while (interviewQuestions.size() < 10) {
            interviewQuestions.add("What makes you a suitable candidate for your chosen career path?");
        }
    }

    private void showCurrentQuestion() {
        if (interviewQuestions.isEmpty()) {
            questionCounterText.setText("Question 0 of 0");
            questionText.setText("No interview questions available yet.");
            return;
        }

        questionCounterText.setText("Question " + (currentQuestionIndex + 1) + " of " + interviewQuestions.size());
        questionText.setText(interviewQuestions.get(currentQuestionIndex));

        answerInput.setText("");
        feedbackText.setText("Type your answer and tap Check My Answer to receive strengths, improvement points and a sample answer.");
    }

    private void showNextQuestion() {
        if (interviewQuestions.isEmpty()) {
            return;
        }

        currentQuestionIndex++;

        if (currentQuestionIndex >= interviewQuestions.size()) {
            currentQuestionIndex = 0;
            CustomToast.showInfo(this, "You reached the end. Starting again from question 1.");
        }

        showCurrentQuestion();
    }

    private void checkAnswer() {
        String answer = answerInput.getText().toString().trim();

        if (answer.isEmpty()) {
            answerInput.setError("Please type your interview answer");
            answerInput.requestFocus();
            return;
        }

        if (answer.length() < 25) {
            answerInput.setError("Please write a little more detail");
            answerInput.requestFocus();
            CustomToast.showInfo(this, "Please write a little more detail.");
            return;
        }

        String currentQuestion = interviewQuestions.get(currentQuestionIndex);

        String strengths = generateInterviewStrengths(answer);
        String improvements = generateInterviewImprovements(answer);
        String sampleImprovedAnswer = generateSampleImprovedAnswer(currentQuestion, answer);
        String practiceTip = generatePracticeTip(answer);

        String feedback = generatePracticeFeedback(
                strengths,
                improvements,
                sampleImprovedAnswer,
                practiceTip
        );

        feedbackText.setText(feedback);

        answerPreviewText.setText(
                "Last Question:\n" + currentQuestion +
                        "\n\nYour Answer:\n" + answer
        );

        saveInterviewResult(
                currentQuestion,
                answer,
                feedback,
                strengths,
                improvements,
                sampleImprovedAnswer,
                practiceTip
        );
    }

    private String generateInterviewStrengths(String answer) {
        String lowerAnswer = answer.toLowerCase();
        StringBuilder strengths = new StringBuilder();

        if (answer.length() >= 70) {
            strengths.append("• You gave a reasonably detailed answer.\n");
        }

        if (containsTargetRoleKeyword(lowerAnswer)) {
            strengths.append("• You connected your answer to your target role.\n");
        }

        if (containsSelectedSkill(lowerAnswer)) {
            strengths.append("• You mentioned a relevant skill from your profile.\n");
        }

        if (containsProjectOrExperience(lowerAnswer)) {
            strengths.append("• You included project or practical experience evidence.\n");
        }

        if (containsProfessionalWord(lowerAnswer)) {
            strengths.append("• Your answer includes professional wording.\n");
        }

        if (strengths.length() == 0) {
            strengths.append("• You attempted the answer, which is a good start.\n");
        }

        return strengths.toString().trim();
    }

    private String generateInterviewImprovements(String answer) {
        String lowerAnswer = answer.toLowerCase();
        StringBuilder improvements = new StringBuilder();

        if (answer.length() < 70) {
            improvements.append("• Add more detail. Interview answers should not be too short.\n");
        }

        if (!containsTargetRoleKeyword(lowerAnswer)) {
            improvements.append("• Mention your target role or career interest clearly.\n");
        }

        if (!containsSelectedSkill(lowerAnswer)) {
            improvements.append("• Mention at least one relevant skill from your profile.\n");
        }

        if (!containsProjectOrExperience(lowerAnswer)) {
            improvements.append("• Add one project, coursework, internship or practical example.\n");
        }

        improvements.append("• Structure your answer with career interest, project example, tools used, your contribution and what you learned.");

        return improvements.toString().trim();
    }

    private String generateSampleImprovedAnswer(String currentQuestion, String answer) {
        String role = targetRole == null || targetRole.equals("Not selected")
                ? "my chosen career role"
                : targetRole;

        String projectExample = getProjectExampleForRole(role);
        String toolsExample = getToolsExampleForRole(role);

        if (currentQuestion.toLowerCase().contains("tell me about yourself")) {
            return "I am a " + degreeProgramme + " student interested in becoming a " + role + ". "
                    + "I enjoy learning practical skills and applying them through academic projects. "
                    + "For example, I worked on " + projectExample + ", where I used " + toolsExample + ". "
                    + "Through this project, I improved my problem-solving, technical and communication skills. "
                    + "I am now focusing on improving my CV, portfolio and interview confidence so I can apply for graduate-level roles.";
        }

        if (currentQuestion.toLowerCase().contains("why do you want")) {
            return "I want to work as a " + role + " because I enjoy solving real problems and building useful solutions. "
                    + "During my studies, I worked on " + projectExample + ", which helped me understand how technical knowledge can be used in practical situations. "
                    + "I used " + toolsExample + " and learned how important testing, clear communication and continuous improvement are. "
                    + "This role matches my interests, skills and long-term career goal.";
        }

        if (currentQuestion.toLowerCase().contains("project")) {
            return "One project I worked on was " + projectExample + ". "
                    + "The purpose of the project was to solve a practical user problem and improve my technical skills. "
                    + "I used " + toolsExample + " to design, implement and test the main features. "
                    + "My contribution included creating the system flow, developing key functions, checking errors and improving the user experience. "
                    + "This project helped me build stronger evidence for a " + role + " role.";
        }

        return "A stronger answer could be: I am interested in " + role + " because it matches my skills and career goals. "
                + "In one of my academic projects, I worked on " + projectExample + " using " + toolsExample + ". "
                + "My role was to understand the problem, build or analyse the solution, test the result and improve it based on feedback. "
                + "This experience helped me improve my technical skills, problem-solving and confidence. "
                + "Going forward, I want to continue improving my portfolio and practical experience for graduate job opportunities.";
    }

    private String generatePracticeTip(String answer) {
        return "Use this simple structure: career interest + project example + tools used + your contribution + what you learned.";
    }

    private String generatePracticeFeedback(
            String strengths,
            String improvements,
            String sampleImprovedAnswer,
            String practiceTip
    ) {
        StringBuilder feedback = new StringBuilder();

        feedback.append("Strengths:\n");
        feedback.append(strengths).append("\n\n");

        feedback.append("Points to Improve:\n");
        feedback.append(improvements).append("\n\n");

        feedback.append("Sample Answer:\n");
        feedback.append(sampleImprovedAnswer).append("\n\n");

        feedback.append("Tip:\n");
        feedback.append(practiceTip);

        return feedback.toString();
    }

    private String getProjectExampleForRole(String role) {
        if (role.equals("Mobile App Developer")) {
            return "an Android mobile app project";
        } else if (role.equals("Software Developer")) {
            return "a software development project";
        } else if (role.equals("Data Analyst")) {
            return "a data analysis project";
        } else if (role.equals("Business Analyst")) {
            return "a business analysis case study";
        } else if (role.equals("Marketing Executive")) {
            return "a digital marketing or campaign project";
        } else if (role.equals("UI UX Designer")) {
            return "a user interface or user experience design project";
        } else if (role.equals("Project Coordinator")) {
            return "a project planning or coordination activity";
        } else {
            return "a university project";
        }
    }

    private String getToolsExampleForRole(String role) {
        if (role.equals("Mobile App Developer")) {
            return "Java, XML, Android Studio and Firebase";
        } else if (role.equals("Software Developer")) {
            return "Java, SQL, database concepts and debugging tools";
        } else if (role.equals("Data Analyst")) {
            return "Excel, SQL, Python or data visualisation tools";
        } else if (role.equals("Business Analyst")) {
            return "Excel, documentation, process analysis and presentation tools";
        } else if (role.equals("Marketing Executive")) {
            return "Canva, social media platforms, content planning and basic analytics";
        } else if (role.equals("UI UX Designer")) {
            return "Figma, Canva, wireframes and prototype design";
        } else if (role.equals("Project Coordinator")) {
            return "task planning, documentation, communication and progress tracking tools";
        } else {
            return "relevant academic and professional tools";
        }
    }

    private boolean containsTargetRoleKeyword(String lowerAnswer) {
        if (targetRole == null || targetRole.equals("Not selected")) {
            return false;
        }

        String lowerRole = targetRole.toLowerCase();

        if (lowerAnswer.contains(lowerRole)) {
            return true;
        }

        String[] roleWords = lowerRole.split(" ");

        for (String word : roleWords) {
            if (word.length() > 3 && lowerAnswer.contains(word)) {
                return true;
            }
        }

        return false;
    }

    private boolean containsSelectedSkill(String lowerAnswer) {
        if (selectedSkills == null || selectedSkills.trim().isEmpty()) {
            return false;
        }

        String[] skills = selectedSkills.split(",");

        for (String skill : skills) {
            String cleanSkill = skill.trim().toLowerCase();

            if (!cleanSkill.isEmpty() && lowerAnswer.contains(cleanSkill)) {
                return true;
            }

            String[] skillWords = cleanSkill.split(" ");

            for (String word : skillWords) {
                if (word.length() > 4 && lowerAnswer.contains(word)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean containsProjectOrExperience(String lowerAnswer) {
        return lowerAnswer.contains("project") ||
                lowerAnswer.contains("experience") ||
                lowerAnswer.contains("developed") ||
                lowerAnswer.contains("built") ||
                lowerAnswer.contains("created") ||
                lowerAnswer.contains("designed") ||
                lowerAnswer.contains("worked");
    }

    private boolean containsProfessionalWord(String lowerAnswer) {
        return lowerAnswer.contains("improved") ||
                lowerAnswer.contains("learned") ||
                lowerAnswer.contains("solved") ||
                lowerAnswer.contains("responsible") ||
                lowerAnswer.contains("team") ||
                lowerAnswer.contains("communication") ||
                lowerAnswer.contains("goal") ||
                lowerAnswer.contains("users") ||
                lowerAnswer.contains("problem");
    }

    private void saveInterviewResult(
            String question,
            String answer,
            String feedback,
            String strengths,
            String improvements,
            String sampleImprovedAnswer,
            String practiceTip
    ) {
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        editor.putBoolean("interviewCompleted", true);
        editor.putBoolean("interviewPracticeOnly", true);

        /*
         * Interview Practice is now a practice-only feature.
         * It gives guidance but does not contribute marks to Dashboard readiness.
         */
        editor.putInt("interviewReadiness", 0);

        editor.putString("lastInterviewQuestion", question);
        editor.putString("lastInterviewAnswer", answer);
        editor.putString("lastInterviewFeedback", feedback);
        editor.putString("interviewStrengths", strengths);
        editor.putString("interviewImprovements", improvements);
        editor.putString("sampleImprovedAnswer", sampleImprovedAnswer);
        editor.putString("interviewPracticeTip", practiceTip);

        editor.apply();

        saveInterviewResultToFirestore(
                question,
                answer,
                feedback,
                strengths,
                improvements,
                sampleImprovedAnswer,
                practiceTip
        );
    }

    private void saveInterviewResultToFirestore(
            String question,
            String answer,
            String feedback,
            String strengths,
            String improvements,
            String sampleImprovedAnswer,
            String practiceTip
    ) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            CustomToast.showError(this, "Interview feedback saved locally. Please login again for cloud save.");
            return;
        }

        Map<String, Object> interviewData = new HashMap<>();

        interviewData.put("interviewCompleted", true);
        interviewData.put("interviewPracticeOnly", true);

        /*
         * Saved as 0 to reset older score-based interview results.
         */
        interviewData.put("interviewReadiness", 0);

        interviewData.put("lastInterviewQuestion", question);
        interviewData.put("lastInterviewAnswer", answer);
        interviewData.put("lastInterviewFeedback", feedback);
        interviewData.put("interviewStrengths", strengths);
        interviewData.put("interviewImprovements", improvements);
        interviewData.put("sampleImprovedAnswer", sampleImprovedAnswer);
        interviewData.put("interviewPracticeTip", practiceTip);

        interviewData.put("targetRole", targetRole);
        interviewData.put("degreeProgramme", degreeProgramme);
        interviewData.put("updatedAt", FieldValue.serverTimestamp());

        submitAnswerButton.setEnabled(false);
        submitAnswerButton.setText("Saving...");

        firestore.collection("users")
                .document(currentUser.getUid())
                .set(interviewData, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    submitAnswerButton.setEnabled(true);
                    submitAnswerButton.setText("Check My Answer");
                    CustomToast.showSuccess(this, "Interview feedback saved successfully");
                })
                .addOnFailureListener(e -> {
                    submitAnswerButton.setEnabled(true);
                    submitAnswerButton.setText("Check My Answer");
                    CustomToast.showError(this, "Interview saved locally, but cloud save failed");
                });
    }

    private void loadLastInterviewResultFromLocalCache() {
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        boolean interviewCompleted = preferences.getBoolean("interviewCompleted", false);
        String lastQuestion = preferences.getString("lastInterviewQuestion", "");
        String lastAnswer = preferences.getString("lastInterviewAnswer", "");
        String lastFeedback = preferences.getString("lastInterviewFeedback", "");

        if (interviewCompleted) {
            answerPreviewText.setText(
                    "Last Question:\n" + lastQuestion +
                            "\n\nYour Answer:\n" + lastAnswer
            );

            if (!lastFeedback.trim().isEmpty()) {
                feedbackText.setText(lastFeedback);
            }

        } else {
            answerPreviewText.setText("Last saved answer will appear here.");
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
}