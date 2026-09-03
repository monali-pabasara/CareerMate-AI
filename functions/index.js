const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const OpenAI = require("openai");

const OPENAI_API_KEY = defineSecret("OPENAI_API_KEY");

exports.generateCareerPlan = onCall(
  {
    secrets: [OPENAI_API_KEY],
    timeoutSeconds: 60,
    memory: "256MiB",
  },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError(
        "unauthenticated",
        "You must be logged in to use AI Career Coach."
      );
    }

    const data = request.data || {};

    const careerSummary = {
      degreeProgramme: safeText(data.degreeProgramme),
      targetRole: safeText(data.targetRole),
      selectedSkills: safeText(data.selectedSkills),
      strongSkills: safeText(data.strongSkills),
      missingSkills: safeText(data.missingSkills),
      careerReadiness: safeNumber(data.careerReadiness),
      profileCompletion: safeNumber(data.profileCompletion),
      skillEvidence: safeNumber(data.skillEvidence),
      cvReadiness: safeNumber(data.cvReadiness),
      portfolioProgress: safeNumber(data.portfolioProgress),
      professionalLinksScore: safeNumber(data.professionalLinksScore),
      suggestedJobRole: safeText(data.suggestedJobRole),
      suggestedJobScore: safeNumber(data.suggestedJobScore),
    };

    if (
      careerSummary.targetRole === "Not selected" ||
      careerSummary.targetRole.trim().length === 0
    ) {
      throw new HttpsError(
        "failed-precondition",
        "Please complete your profile before generating an AI career plan."
      );
    }

    const openai = new OpenAI({
      apiKey: OPENAI_API_KEY.value(),
    });

    const prompt = `
You are an AI Career Coach for a university student career-readiness mobile app.

Create a personalised, practical and supportive career action plan based only on the summary below.

Do not promise employment.
Do not mention private data.
Do not ask for sensitive personal details.
Use simple clear English.
Make the advice suitable for a final-year university student or fresh graduate.
Focus on practical next steps.

Student Career Summary:
Degree Programme: ${careerSummary.degreeProgramme}
Target Role: ${careerSummary.targetRole}
Selected Skills: ${careerSummary.selectedSkills}
Strong Skills: ${careerSummary.strongSkills}
Skills to Improve: ${careerSummary.missingSkills}

Career Readiness Score: ${careerSummary.careerReadiness}%
Profile Completion: ${careerSummary.profileCompletion}%
Skill Evidence: ${careerSummary.skillEvidence}%
CV Readiness: ${careerSummary.cvReadiness}%
Portfolio Evidence: ${careerSummary.portfolioProgress}%
Professional Links Score: ${careerSummary.professionalLinksScore}%

Suggested Job Role: ${careerSummary.suggestedJobRole}
Suggested Job Score: ${careerSummary.suggestedJobScore}%

Return the answer in this exact structure:

AI Career Coach Plan

1. Career Direction
Explain the student's current career direction in 2-3 sentences.

2. Strong Evidence
List 3 strengths based on the available data.

3. Skills to Improve First
List 4 priority improvements.

4. CV Improvement Advice
Give 3 clear CV improvement tips.

5. Portfolio Improvement Advice
Give 3 clear portfolio improvement tips.

6. 7-Day Action Plan
Give Day 1 to Day 7 practical actions.

7. Reminder
End with one short note that this is career guidance and does not guarantee employment.
`;

    try {
      const response = await openai.responses.create({
        model: "gpt-4.1-mini",
        input: prompt,
        max_output_tokens: 900,
      });

      const careerPlan = response.output_text || "";

      if (!careerPlan.trim()) {
        throw new HttpsError(
          "internal",
          "AI response was empty. Please try again."
        );
      }

      return {
        careerPlan: careerPlan.trim(),
      };
    } catch (error) {
      console.error("AI Career Coach error:", error);

      throw new HttpsError(
        "internal",
        "AI Career Coach could not generate a plan right now."
      );
    }
  }
);

function safeText(value) {
  if (value === undefined || value === null) {
    return "";
  }

  return String(value).trim().slice(0, 1200);
}

function safeNumber(value) {
  const numberValue = Number(value);

  if (Number.isNaN(numberValue)) {
    return 0;
  }

  if (numberValue < 0) {
    return 0;
  }

  if (numberValue > 100) {
    return 100;
  }

  return Math.round(numberValue);
}