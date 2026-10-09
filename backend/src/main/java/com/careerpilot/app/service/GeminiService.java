package com.careerpilot.app.service;

import com.careerpilot.app.dto.ApplicationIntelligenceDto;
import com.careerpilot.app.dto.CareerAnalyticsDto;
import com.careerpilot.app.dto.CareerAssistantDto;
import com.careerpilot.app.dto.CareerRoadmapDto;
import com.careerpilot.app.dto.LearningRecommendationDto;
import com.careerpilot.app.dto.JobPreparationDto;
import com.careerpilot.app.dto.gemini.InterviewEvaluationDto;
import com.careerpilot.app.dto.gemini.InterviewFinalEvaluationDto;
import com.careerpilot.app.dto.gemini.InterviewQuestionDto;
import com.careerpilot.app.dto.response.JobMatchDto;
import com.careerpilot.app.dto.response.LearningPlanStepDto;
import com.careerpilot.app.dto.response.ResumeAnalysisDto;
import com.careerpilot.app.dto.response.SkillGapDto;
import com.careerpilot.app.dto.response.SkillGapGeminiDto;
import com.careerpilot.app.entity.Job;
import com.careerpilot.app.exception.AIServiceException;
import com.careerpilot.app.exception.GeminiRateLimitException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class GeminiService {

    @Value("${app.gemini.api-key:}")
    private String apiKey;

    @Value("${app.gemini.model:gemini-3.6-flash}")
    private String model;

    private RestTemplate restTemplate = createRestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private static RestTemplate createRestTemplate() {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15000); // 15s connection timeout
        factory.setReadTimeout(60000);    // 60s read timeout for AI generation
        return new RestTemplate(factory);
    }

    private static final String GEMINI_API_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";

    @PostConstruct
    public void init() {
        boolean keyConfigured = apiKey != null && !apiKey.trim().isEmpty();
        String safeModel = getSanitizedModelName();
        log.info("Gemini model: {}", safeModel);
        log.info("Gemini API key configured: {}", keyConfigured);
    }

    public String getSanitizedModelName() {
        if (model == null || model.trim().isEmpty()) {
            return "gemini-3.6-flash";
        }
        String cleaned = model.trim();
        if (cleaned.startsWith("models/")) {
            cleaned = cleaned.substring(7);
        }
        return cleaned;
    }

    /**
     * Diagnostic method that makes a simple test request to verify API key, model availability, and endpoint response.
     */
    public String testSimpleGeminiCall() {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new AIServiceException("Gemini API configuration is missing. Please set the GEMINI_API_KEY environment variable on the backend.");
        }

        String safeModel = getSanitizedModelName();
        String url = GEMINI_API_BASE_URL + safeModel + ":generateContent";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", apiKey.trim());

        Map<String, Object> part = Map.of("text", "Reply with exactly: GEMINI_OK");
        Map<String, Object> content = Map.of("parts", Collections.singletonList(part));
        Map<String, Object> requestBody = Map.of("contents", Collections.singletonList(content));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            log.info("Executing diagnostic GEMINI_OK test request using model: {}", safeModel);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String reply = extractTextFromGeminiResponse(response.getBody());
                log.info("Diagnostic GEMINI_OK test succeeded! Reply: {}", reply);
                return reply;
            } else {
                throw new AIServiceException("Gemini test returned unexpected HTTP status: " + response.getStatusCode());
            }
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            if (isRateLimitError(e)) {
                Integer retrySeconds = extractRetryAfterSeconds(e);
                log.warn("Gemini diagnostic test rate-limited (429/RESOURCE_EXHAUSTED). Retry-After: {}s", retrySeconds);
                throw new GeminiRateLimitException("Gemini usage limit reached. Please wait and try again later.", retrySeconds);
            }
            String safeError = parseSafeGeminiError(e);
            log.error("Gemini diagnostic test failed: {} | Safe details: {}", e.getStatusCode(), safeError);
            throw new AIServiceException("Gemini diagnostic error (" + e.getStatusCode().value() + "): " + safeError);
        } catch (GeminiRateLimitException e) {
            throw e;
        } catch (Exception e) {
            log.error("Gemini diagnostic test unexpected error: {}", e.getMessage());
            throw new AIServiceException("Gemini diagnostic failed: " + e.getMessage());
        }
    }

    public ResumeAnalysisDto analyzeResumeText(String extractedText, String targetRole, String profileInfo) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.error("Gemini API key is not configured");
            throw new AIServiceException("Gemini API configuration is missing. Please set the GEMINI_API_KEY environment variable on the backend.");
        }

        if (extractedText == null || extractedText.trim().isEmpty()) {
            throw new AIServiceException("Resume contains no extractable text for AI analysis.");
        }

        String prompt = buildResumeAnalysisPrompt(extractedText, targetRole, profileInfo);
        String jsonResponseStr = callGeminiApi(prompt);
        return parseAndValidateAnalysisJson(jsonResponseStr);
    }

    private String callGeminiApi(String promptText) {
        String safeModel = getSanitizedModelName();
        String url = GEMINI_API_BASE_URL + safeModel + ":generateContent";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", apiKey.trim());

        // Construct request payload matching Gemini REST API spec
        Map<String, Object> requestBody = new HashMap<>();

        Map<String, Object> part = new HashMap<>();
        part.put("text", promptText);

        Map<String, Object> content = new HashMap<>();
        content.put("parts", Collections.singletonList(part));

        requestBody.put("contents", Collections.singletonList(content));

        // Generation Config for JSON response mode
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("response_mime_type", "application/json");
        generationConfig.put("temperature", 0.2); // Low temperature for deterministic analysis
        requestBody.put("generationConfig", generationConfig);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        int maxRetries = 2; // Only for temporary 503/502/network hiccups
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.info("Sending request to Gemini API (model: {}, attempt: {}/{})", safeModel, attempt, maxRetries);
                ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    return extractTextFromGeminiResponse(response.getBody());
                } else {
                    throw new AIServiceException("AI analysis service returned an unexpected response status: " + response.getStatusCode());
                }

            } catch (HttpClientErrorException.TooManyRequests e) {
                // Rate limit / Quota exceeded -> Never retry repeatedly (avoid retry storms)
                Integer retrySeconds = extractRetryAfterSeconds(e);
                log.warn("Gemini API 429 Too Many Requests (model: {}). Quota exceeded. Retry-After: {}s", safeModel, retrySeconds);
                throw new GeminiRateLimitException("Gemini usage limit reached. Please wait and try again later.", retrySeconds);
            } catch (HttpClientErrorException | HttpServerErrorException e) {
                // Check if the response body indicates rate limit / quota exhaustion even on other status codes
                if (isRateLimitError(e)) {
                    Integer retrySeconds = extractRetryAfterSeconds(e);
                    log.warn("Gemini API quota exhausted ({}/RESOURCE_EXHAUSTED). Retry-After: {}s", e.getStatusCode().value(), retrySeconds);
                    throw new GeminiRateLimitException("Gemini usage limit reached. Please wait and try again later.", retrySeconds);
                }

                // Error Classification
                int statusCode = e.getStatusCode().value();
                String safeError = parseSafeGeminiError(e);

                if (statusCode == 401) {
                    log.error("Gemini API 401 Unauthorized - invalid API key.");
                    throw new AIServiceException("AI service authentication failed. Please verify API configuration.", HttpStatus.BAD_GATEWAY);
                } else if (statusCode == 403) {
                    log.error("Gemini API 403 Forbidden - permission/project restricted: {}", safeError);
                    throw new AIServiceException("AI service access is restricted. Please check API permissions.", HttpStatus.BAD_GATEWAY);
                } else if (statusCode == 404) {
                    log.error("Gemini API 404 Not Found for model '{}': {}", safeModel, safeError);
                    throw new AIServiceException("AI service model '" + safeModel + "' was not found.", HttpStatus.BAD_GATEWAY);
                } else if (statusCode == 400) {
                    log.error("Gemini API 400 Bad Request: {}", safeError);
                    throw new AIServiceException("AI service request was invalid. Please try again with adjusted input.", HttpStatus.BAD_GATEWAY);
                } else if (statusCode == 503 || statusCode == 502) {
                    log.warn("Gemini API {} Service Unavailable (attempt {}/{}): {}", statusCode, attempt, maxRetries, safeError);
                    if (attempt < maxRetries) {
                        try {
                            log.info("Backing off for 1500 ms before retrying transient 503/502...");
                            Thread.sleep(1500L);
                            continue;
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    }
                    throw new AIServiceException("AI service is temporarily unavailable. Please try again shortly.", HttpStatus.SERVICE_UNAVAILABLE);
                } else if (statusCode >= 500) {
                    log.error("Gemini API {} Server Error: {}", statusCode, safeError);
                    throw new AIServiceException("AI service encountered an internal error. Please try again later.", HttpStatus.BAD_GATEWAY);
                } else {
                    log.error("Gemini API HTTP error ({}): {}", statusCode, safeError);
                    throw new AIServiceException("AI service error (" + statusCode + "): " + safeError, HttpStatus.BAD_GATEWAY);
                }
            } catch (ResourceAccessException e) {
                log.error("Network timeout connecting to Gemini API: {}", e.getMessage());
                throw new AIServiceException("Network timeout communicating with AI service. Please try again.", HttpStatus.GATEWAY_TIMEOUT, e);
            } catch (GeminiRateLimitException | AIServiceException e) {
                throw e;
            } catch (Exception e) {
                log.error("Unexpected error during Gemini API call: {}", e.getMessage(), e);
                throw new AIServiceException("An error occurred while communicating with the AI service.", HttpStatus.BAD_GATEWAY, e);
            }
        }
        throw new AIServiceException("AI service request failed after retry attempt.", HttpStatus.BAD_GATEWAY);
    }

    public boolean isRateLimitError(org.springframework.web.client.HttpStatusCodeException e) {
        if (e.getStatusCode().value() == 429) {
            return true;
        }
        try {
            String body = e.getResponseBodyAsString();
            if (body != null && !body.isBlank()) {
                String lower = body.toLowerCase();
                if (lower.contains("resource_exhausted") ||
                    lower.contains("rate_limit_exceeded") ||
                    lower.contains("quota exceeded") ||
                    lower.contains("free_tier_requests") ||
                    lower.contains("too many requests")) {
                    return true;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    public Integer extractRetryAfterSeconds(org.springframework.web.client.HttpStatusCodeException e) {
        // 1. Check HTTP Header "Retry-After"
        try {
            if (e.getResponseHeaders() != null) {
                String header = e.getResponseHeaders().getFirst("Retry-After");
                if (header != null && !header.isBlank()) {
                    try {
                        return Integer.parseInt(header.trim());
                    } catch (NumberFormatException ignored) {}
                }
            }
        } catch (Exception ignored) {}

        // 2. Check JSON response body for retryDelay in details / metadata / message
        try {
            String responseBody = e.getResponseBodyAsString();
            if (responseBody != null && !responseBody.isBlank()) {
                JsonNode root = objectMapper.readTree(responseBody);
                if (root.has("error")) {
                    JsonNode errorNode = root.get("error");
                    if (errorNode.has("details") && errorNode.get("details").isArray()) {
                        for (JsonNode detail : errorNode.get("details")) {
                            if (detail.has("retryDelay")) {
                                Integer secs = parseDurationToSeconds(detail.get("retryDelay").asText());
                                if (secs != null) return secs;
                            }
                            if (detail.has("metadata") && detail.get("metadata").has("retryDelay")) {
                                Integer secs = parseDurationToSeconds(detail.get("metadata").get("retryDelay").asText());
                                if (secs != null) return secs;
                            }
                        }
                    }
                    if (errorNode.has("message")) {
                        String msg = errorNode.get("message").asText();
                        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("retry after (\\d+(\\.\\d+)?)s?", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(msg);
                        if (matcher.find()) {
                            double val = Double.parseDouble(matcher.group(1));
                            return (int) Math.ceil(val);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        return null;
    }

    private Integer parseDurationToSeconds(String durationStr) {
        if (durationStr == null || durationStr.isBlank()) return null;
        try {
            String clean = durationStr.trim().toLowerCase().replaceAll("[^0-9.]", "");
            if (!clean.isEmpty()) {
                double val = Double.parseDouble(clean);
                return (int) Math.ceil(val);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String parseSafeGeminiError(org.springframework.web.client.HttpStatusCodeException e) {
        try {
            String responseBody = e.getResponseBodyAsString();
            if (responseBody != null && !responseBody.trim().isEmpty()) {
                JsonNode root = objectMapper.readTree(responseBody);
                if (root.has("error") && root.get("error").has("message")) {
                    String msg = root.get("error").get("message").asText();
                    // Ensure no secrets exist in message
                    return msg.replaceAll("key=[^&\\s]+", "key=HIDDEN")
                              .replaceAll("Bearer\\s+[^\\s]+", "Bearer HIDDEN");
                }
            }
        } catch (Exception parseEx) {
            // Ignore parse errors and fallback to status text
        }
        return e.getStatusText();
    }

    @SuppressWarnings("unchecked")
    private String extractTextFromGeminiResponse(Map responseMap) {
        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseMap.get("candidates");
            if (candidates == null || candidates.isEmpty()) {
                throw new AIServiceException("AI service returned no response candidates.");
            }

            Map<String, Object> candidate = candidates.get(0);
            Map<String, Object> content = (Map<String, Object>) candidate.get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");

            if (parts == null || parts.isEmpty()) {
                throw new AIServiceException("AI service returned empty response parts.");
            }

            return (String) parts.get(0).get("text");
        } catch (Exception e) {
            log.error("Failed to parse Gemini response structure: {}", e.getMessage());
            throw new AIServiceException("Failed to read output from AI service response.");
        }
    }

    private ResumeAnalysisDto parseAndValidateAnalysisJson(String jsonText) {
        if (jsonText == null || jsonText.trim().isEmpty()) {
            throw new AIServiceException("AI service returned empty text response.");
        }

        // Clean any markdown code block fences if present (e.g. ```json ... ```)
        String cleanedJson = jsonText.trim();
        if (cleanedJson.startsWith("```json")) {
            cleanedJson = cleanedJson.substring(7);
        } else if (cleanedJson.startsWith("```")) {
            cleanedJson = cleanedJson.substring(3);
        }
        if (cleanedJson.endsWith("```")) {
            cleanedJson = cleanedJson.substring(0, cleanedJson.length() - 3);
        }
        cleanedJson = cleanedJson.trim();

        try {
            ResumeAnalysisDto dto = objectMapper.readValue(cleanedJson, ResumeAnalysisDto.class);

            // Ensure scores are within 0-100 range
            if (dto.getOverallScore() == null) dto.setOverallScore(70);
            else dto.setOverallScore(Math.max(0, Math.min(100, dto.getOverallScore())));

            if (dto.getAtsScore() == null) dto.setAtsScore(70);
            else dto.setAtsScore(Math.max(0, Math.min(100, dto.getAtsScore())));

            return dto;
        } catch (Exception e) {
            log.error("Failed to parse AI JSON response: {}. Raw output: {}", e.getMessage(), jsonText);
            throw new AIServiceException("AI service produced a malformed analysis output. Please re-run the analysis.");
        }
    }

    private String buildResumeAnalysisPrompt(String extractedText, String targetRole, String profileInfo) {
        String roleContext = (targetRole != null && !targetRole.trim().isEmpty())
                ? targetRole.trim()
                : "Software Developer";

        String userProfileContext = (profileInfo != null && !profileInfo.trim().isEmpty())
                ? profileInfo.trim()
                : "Not provided";

        return """
                You are an expert AI Career Coach and Resume Reviewer for tech and software engineering placements.
                Analyze the following resume text specifically for the target role: "%s".
                
                USER PROFILE CONTEXT:
                %s
                
                EXTRACTED RESUME TEXT:
                \"\"\"
                %s
                \"\"\"
                
                CRITICAL INSTRUCTIONS:
                1. Analyze the resume based ONLY on the supplied information above.
                2. Do NOT invent work experience, projects, skills, certifications, or education that are missing.
                3. If information is missing, explicitly note that it is missing in the section analysis.
                4. Calculate two scores (0 to 100 integer scale):
                   - overallScore: AI-based overall quality score of the resume content, structure, and impact.
                   - atsScore: AI-based ATS compatibility score for target role formatting and key terms.
                5. Categorize detected skills into standard technical categories (e.g. "Programming Languages", "Frameworks", "Databases", "Tools", "Other").
                6. Identify missing skills critical or beneficial for the target role ("%s") that are not found in the resume text, assigning priority (HIGH, MEDIUM, LOW) and a brief reason.
                7. Provide concise, actionable suggestions for improving the resume.
                8. Output valid JSON strictly adhering to the JSON schema below.
                
                JSON SCHEMA TO RETURN:
                {
                  "overallScore": 82,
                  "atsScore": 78,
                  "summary": "High-level summary of candidate qualifications and resume strength.",
                  "strengths": ["Strong Java foundation", "Good project outcomes"],
                  "weaknesses": ["Missing metrics", "No certifications listed"],
                  "detectedSkills": [
                    {
                      "name": "Java",
                      "category": "Programming Languages",
                      "confidence": 0.95
                    }
                  ],
                  "missingSkills": [
                    {
                      "name": "Spring Boot",
                      "reason": "Highly required for backend Java roles",
                      "priority": "HIGH"
                    }
                  ],
                  "suggestions": ["Add measurable metrics to projects", "Include GitHub profile link"],
                  "sectionAnalysis": {
                    "summary": "Analysis of resume professional summary.",
                    "education": "Analysis of education section.",
                    "skills": "Analysis of technical skills section.",
                    "projects": "Analysis of projects section.",
                    "experience": "Analysis of experience section.",
                    "certifications": "Analysis of certifications section."
                  }
                }
                
                Return ONLY the JSON object.
                """.formatted(roleContext, userProfileContext, extractedText, roleContext);
    }

    public JobMatchDto analyzeJobMatch(String candidateProfileJson, Job job) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.error("Gemini API key is not configured");
            throw new AIServiceException("Gemini API configuration is missing. Please set the GEMINI_API_KEY environment variable on the backend.");
        }

        if (job == null) {
            throw new AIServiceException("Job vacancy information is missing.");
        }

        String prompt = buildJobMatchPrompt(candidateProfileJson, job);
        String jsonResponseStr = callGeminiApi(prompt);
        return parseAndValidateJobMatchJson(jsonResponseStr);
    }

    private String buildJobMatchPrompt(String candidateProfileJson, Job job) {
        String title = job.getTitle() != null ? job.getTitle() : "Software Engineer";
        String company = job.getCompany() != null ? job.getCompany() : "Company";
        String location = job.getLocation() != null ? job.getLocation() : "Not specified";
        String jobType = job.getJobType() != null ? job.getJobType() : "Full-time";
        String experience = job.getExperienceLevel() != null ? job.getExperienceLevel() : "Not specified";
        String reqSkills = job.getRequiredSkills() != null ? job.getRequiredSkills() : "Not specified";
        String prefSkills = job.getPreferredSkills() != null ? job.getPreferredSkills() : "None";
        String description = job.getDescription() != null ? job.getDescription() : "No description provided";

        return """
                You are an expert AI Career Matcher and Recruitment Specialist for tech hiring.
                Compare the following candidate profile against the specified job vacancy.
                
                CANDIDATE PROFILE:
                \"\"\"
                %s
                \"\"\"
                
                JOB VACANCY DETAILS:
                - Title: %s
                - Company: %s
                - Location: %s
                - Job Type: %s
                - Required Experience Level: %s
                - Required Skills: %s
                - Preferred Skills: %s
                - Job Description:
                %s
                
                CRITICAL INSTRUCTIONS:
                1. Evaluate how well the candidate matches the job requirements across technical skills, experience level, and education.
                2. Calculate four integer scores (0 to 100 scale):
                   - overallScore: Weighted overall match score reflecting true candidate fit.
                   - skillScore: How closely candidate's technical skills match the job's required and preferred skills.
                   - experienceScore: How well candidate's experience level matches this role.
                   - educationScore: How well candidate's education matches typical expectations for this role.
                3. List specific matchingSkills that the candidate possesses and the job requires or prefers.
                4. List specific missingSkills that the job requires or prefers but the candidate does not have.
                5. Choose exactly ONE recommendation value from:
                   - "STRONG_MATCH" (overallScore 80-100)
                   - "GOOD_MATCH" (overallScore 65-79)
                   - "PARTIAL_MATCH" (overallScore 50-64)
                   - "LOW_MATCH" (overallScore 0-49)
                6. Write a concise, professional explanation (2-3 sentences) summarizing candidate fit, key strengths for the role, and any notable gaps.
                7. Return strictly valid JSON adhering to the JSON schema below.
                
                JSON SCHEMA TO RETURN:
                {
                  "overallScore": 87,
                  "skillScore": 92,
                  "experienceScore": 85,
                  "educationScore": 100,
                  "matchingSkills": [
                    "Java",
                    "Spring Boot",
                    "MySQL"
                  ],
                  "missingSkills": [
                    "Docker",
                    "AWS"
                  ],
                  "recommendation": "STRONG_MATCH",
                  "explanation": "The candidate has strong Java and Spring Boot foundations well-suited for this role, with minor gaps in cloud deployment tools."
                }
                
                Return ONLY the JSON object.
                """.formatted(candidateProfileJson, title, company, location, jobType, experience, reqSkills, prefSkills, description);
    }

    private JobMatchDto parseAndValidateJobMatchJson(String jsonText) {
        if (jsonText == null || jsonText.trim().isEmpty()) {
            throw new AIServiceException("AI service returned empty match response.");
        }

        // Clean markdown code blocks if present
        String cleanedJson = jsonText.trim();
        if (cleanedJson.startsWith("```json")) {
            cleanedJson = cleanedJson.substring(7);
        } else if (cleanedJson.startsWith("```")) {
            cleanedJson = cleanedJson.substring(3);
        }
        if (cleanedJson.endsWith("```")) {
            cleanedJson = cleanedJson.substring(0, cleanedJson.length() - 3);
        }
        cleanedJson = cleanedJson.trim();

        try {
            JobMatchDto dto = objectMapper.readValue(cleanedJson, JobMatchDto.class);

            // Clamp and sanitize scores (0 to 100)
            if (dto.getOverallScore() == null) dto.setOverallScore(60);
            else dto.setOverallScore(Math.max(0, Math.min(100, dto.getOverallScore())));

            if (dto.getSkillScore() == null) dto.setSkillScore(60);
            else dto.setSkillScore(Math.max(0, Math.min(100, dto.getSkillScore())));

            if (dto.getExperienceScore() == null) dto.setExperienceScore(60);
            else dto.setExperienceScore(Math.max(0, Math.min(100, dto.getExperienceScore())));

            if (dto.getEducationScore() == null) dto.setEducationScore(70);
            else dto.setEducationScore(Math.max(0, Math.min(100, dto.getEducationScore())));

            // Ensure lists are non-null
            if (dto.getMatchingSkills() == null) {
                dto.setMatchingSkills(Collections.emptyList());
            }
            if (dto.getMissingSkills() == null) {
                dto.setMissingSkills(Collections.emptyList());
            }

            // Ensure recommendation is one of allowed enum values
            String rec = dto.getRecommendation();
            if (rec == null || (!rec.equals("STRONG_MATCH") && !rec.equals("GOOD_MATCH")
                    && !rec.equals("PARTIAL_MATCH") && !rec.equals("LOW_MATCH"))) {
                int score = dto.getOverallScore();
                if (score >= 80) dto.setRecommendation("STRONG_MATCH");
                else if (score >= 65) dto.setRecommendation("GOOD_MATCH");
                else if (score >= 50) dto.setRecommendation("PARTIAL_MATCH");
                else dto.setRecommendation("LOW_MATCH");
            }

            if (dto.getExplanation() == null || dto.getExplanation().trim().isEmpty()) {
                dto.setExplanation("Candidate profile matched with an overall compatibility score of " + dto.getOverallScore() + "%.");
            }

            return dto;
        } catch (Exception e) {
            log.error("Failed to parse AI Job Match JSON response: {}. Raw output: {}", e.getMessage(), jsonText);
            throw new AIServiceException("AI service produced a malformed job match output. Please retry the match analysis.");
        }
    }

    public SkillGapGeminiDto analyzeSkillGap(String candidateProfileJson, String targetRole, Job job) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.error("Gemini API key is not configured");
            throw new AIServiceException("Gemini API configuration is missing. Please set the GEMINI_API_KEY environment variable on the backend.");
        }

        if ((targetRole == null || targetRole.trim().isEmpty()) && job == null) {
            throw new AIServiceException("Target role or target job is required for skill gap analysis.");
        }

        String prompt = buildSkillGapPrompt(candidateProfileJson, targetRole, job);
        String jsonResponseStr = callGeminiApi(prompt);
        return parseAndValidateSkillGapJson(jsonResponseStr);
    }

    private String buildSkillGapPrompt(String candidateProfileJson, String targetRole, Job job) {
        String effectiveRole = targetRole != null && !targetRole.trim().isEmpty()
                ? targetRole.trim()
                : (job != null ? job.getTitle() : "Software Developer");

        String jobDetails = "None provided (general target role analysis).";
        if (job != null) {
            jobDetails = """
                    - Job Title: %s
                    - Company: %s
                    - Required Skills: %s
                    - Preferred Skills: %s
                    - Experience Level: %s
                    - Description: %s
                    """.formatted(
                    job.getTitle(),
                    job.getCompany(),
                    job.getRequiredSkills() != null ? job.getRequiredSkills() : "N/A",
                    job.getPreferredSkills() != null ? job.getPreferredSkills() : "N/A",
                    job.getExperienceLevel() != null ? job.getExperienceLevel() : "N/A",
                    job.getDescription() != null ? job.getDescription() : "N/A"
            );
        }

        return """
                You are an expert AI Career Coach, Principal Engineer, and Tech Curriculum Designer.
                Perform an in-depth Skill Gap Analysis and build a personalized step-by-step learning plan for the candidate aiming for the target role: "%s".
                
                CANDIDATE PROFILE & SKILLS:
                \"\"\"
                %s
                \"\"\"
                
                TARGET JOB DETAILS (IF APPLICABLE):
                %s
                
                CRITICAL INSTRUCTIONS:
                1. Determine the candidate's current capabilities vs industry benchmarks for "%s".
                2. Calculate two integer metrics (0 to 100):
                   - readinessScore: Candidate's overall readiness to perform effectively in this role.
                   - skillCoverageScore: Percentage of mandatory technical skills candidate already possesses.
                3. Provide a strategic, encouraging AI summary (2-3 sentences) evaluating readiness and key priorities.
                4. List:
                   - currentSkills: Specific relevant skills candidate already has.
                   - requiredSkills: Full list of essential and expected modern industry skills for this role.
                   - missingSkills: Core technical skills required that candidate lacks or is weak in.
                5. Detail the skill gaps in the "skillGaps" array (at least 3-6 gaps for comprehensive guidance):
                   - skillName: Name of the missing or incomplete skill (e.g. "Docker & Containerization").
                   - priority: "HIGH", "MEDIUM", or "LOW" (HIGH = essential blocker for hiring).
                   - currentLevel: "None", "Beginner", "Intermediate", or "Advanced".
                   - requiredLevel: "Beginner", "Intermediate", or "Advanced".
                   - importance: "Critical", "High", "Medium", or "Low".
                   - reason: Why this skill is critical for "%s" day-to-day responsibilities.
                   - recommendedTechnologies: 2-4 specific frameworks, tools, or libraries (e.g. ["Docker", "Docker Compose", "Dockerfile"]).
                   - practiceProjects: 1-2 practical, resume-worthy project ideas demonstrating this skill.
                   - estimatedLearningHours: Realistic integer hours to reach required level (e.g. 20, 35).
                   - difficulty: "Beginner", "Intermediate", or "Advanced".
                   - learningOrder: Sequential integer order (1, 2, 3...) to prioritize learning.
                6. Generate a personalized, chronological "learningPlan" roadmap:
                   - step: Integer step (1, 2, 3...).
                   - phaseTitle: Phase title (e.g. "Phase 1: Backend Persistence & Optimization").
                   - skillName: Primary skill / subject area for this phase.
                   - description: Concrete action plan of what candidate will learn and master.
                   - estimatedHours: Total hours for this phase.
                   - keyTopics: 3-5 core subtopics to cover.
                   - milestoneProject: Clear milestone/capstone project confirming readiness before moving to next phase.
                7. Return strictly valid JSON adhering to the JSON schema below.
                
                JSON SCHEMA TO RETURN:
                {
                  "readinessScore": 75,
                  "skillCoverageScore": 70,
                  "summary": "Candidate has solid core programming and database fundamentals, but needs hands-on experience in containerization and cloud deployments to be fully job-ready.",
                  "currentSkills": ["Java", "Spring Boot", "MySQL", "REST API", "Git"],
                  "requiredSkills": ["Java", "Spring Boot", "MySQL", "REST API", "Docker", "AWS", "JUnit", "Redis"],
                  "missingSkills": ["Docker", "AWS", "JUnit", "Redis"],
                  "skillGaps": [
                    {
                      "skillName": "Docker & Containerization",
                      "priority": "HIGH",
                      "currentLevel": "None",
                      "requiredLevel": "Intermediate",
                      "importance": "Critical",
                      "reason": "Modern Java microservices are deployed as containerized services in CI/CD pipelines.",
                      "recommendedTechnologies": ["Docker", "Docker Compose", "Dockerfile"],
                      "practiceProjects": ["Containerize a Spring Boot + MySQL multi-container application"],
                      "estimatedLearningHours": 20,
                      "difficulty": "Intermediate",
                      "learningOrder": 1
                    }
                  ],
                  "learningPlan": [
                    {
                      "step": 1,
                      "phaseTitle": "Phase 1: Automated Testing & Code Quality",
                      "skillName": "JUnit 5 & Mockito",
                      "description": "Master unit and integration testing for Spring Boot controllers and services.",
                      "estimatedHours": 15,
                      "keyTopics": ["Unit Testing", "Mockito Mocking", "Spring Boot Test", "Test Coverage"],
                      "milestoneProject": "Write comprehensive test suite with high code coverage for an existing REST API"
                    }
                  ]
                }
                
                Return ONLY the JSON object.
                """.formatted(effectiveRole, candidateProfileJson, jobDetails, effectiveRole, effectiveRole);
    }

    private SkillGapGeminiDto parseAndValidateSkillGapJson(String jsonText) {
        if (jsonText == null || jsonText.trim().isEmpty()) {
            throw new AIServiceException("AI service returned empty skill gap response.");
        }

        String cleanedJson = jsonText.trim();
        if (cleanedJson.startsWith("```json")) {
            cleanedJson = cleanedJson.substring(7);
        } else if (cleanedJson.startsWith("```")) {
            cleanedJson = cleanedJson.substring(3);
        }
        if (cleanedJson.endsWith("```")) {
            cleanedJson = cleanedJson.substring(0, cleanedJson.length() - 3);
        }
        cleanedJson = cleanedJson.trim();

        try {
            SkillGapGeminiDto dto = objectMapper.readValue(cleanedJson, SkillGapGeminiDto.class);

            // Clamp scores
            if (dto.getReadinessScore() == null) dto.setReadinessScore(65);
            else dto.setReadinessScore(Math.max(0, Math.min(100, dto.getReadinessScore())));

            if (dto.getSkillCoverageScore() == null) dto.setSkillCoverageScore(60);
            else dto.setSkillCoverageScore(Math.max(0, Math.min(100, dto.getSkillCoverageScore())));

            if (dto.getSummary() == null || dto.getSummary().trim().isEmpty()) {
                dto.setSummary("Skill gap analysis calculated with an overall readiness score of " + dto.getReadinessScore() + "%.");
            }

            if (dto.getCurrentSkills() == null) dto.setCurrentSkills(Collections.emptyList());
            if (dto.getRequiredSkills() == null) dto.setRequiredSkills(Collections.emptyList());
            if (dto.getMissingSkills() == null) dto.setMissingSkills(Collections.emptyList());

            if (dto.getSkillGaps() == null) {
                dto.setSkillGaps(Collections.emptyList());
            } else {
                int order = 1;
                for (SkillGapDto gap : dto.getSkillGaps()) {
                    if (gap.getPriority() == null || (!gap.getPriority().equalsIgnoreCase("HIGH")
                            && !gap.getPriority().equalsIgnoreCase("MEDIUM")
                            && !gap.getPriority().equalsIgnoreCase("LOW"))) {
                        gap.setPriority("MEDIUM");
                    } else {
                        gap.setPriority(gap.getPriority().toUpperCase());
                    }

                    if (gap.getCurrentLevel() == null) gap.setCurrentLevel("None");
                    if (gap.getRequiredLevel() == null) gap.setRequiredLevel("Intermediate");
                    if (gap.getImportance() == null) gap.setImportance("High");
                    if (gap.getDifficulty() == null) gap.setDifficulty("Intermediate");
                    if (gap.getEstimatedLearningHours() == null || gap.getEstimatedLearningHours() <= 0) gap.setEstimatedLearningHours(20);
                    if (gap.getRecommendedTechnologies() == null) gap.setRecommendedTechnologies(Collections.emptyList());
                    if (gap.getPracticeProjects() == null) gap.setPracticeProjects(Collections.emptyList());
                    if (gap.getLearningOrder() == null || gap.getLearningOrder() <= 0) gap.setLearningOrder(order++);
                }
            }

            if (dto.getLearningPlan() == null) {
                dto.setLearningPlan(Collections.emptyList());
            } else {
                int step = 1;
                for (LearningPlanStepDto planStep : dto.getLearningPlan()) {
                    if (planStep.getStep() == null || planStep.getStep() <= 0) planStep.setStep(step++);
                    if (planStep.getKeyTopics() == null) planStep.setKeyTopics(Collections.emptyList());
                    if (planStep.getEstimatedHours() == null || planStep.getEstimatedHours() <= 0) planStep.setEstimatedHours(20);
                }
            }

            return dto;
        } catch (Exception e) {
            log.error("Failed to parse AI Skill Gap JSON response: {}. Raw output: {}", e.getMessage(), jsonText);
            throw new AIServiceException("AI service produced a malformed skill gap output. Please retry the analysis.");
        }
    }

    public CareerRoadmapDto analyzeCareerRoadmap(String candidateProfileJson) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.error("Gemini API key is not configured");
            throw new AIServiceException("Gemini API configuration is missing. Please set the GEMINI_API_KEY environment variable on the backend.");
        }

        if (candidateProfileJson == null || candidateProfileJson.trim().isEmpty()) {
            throw new AIServiceException("Candidate profile context is required for career roadmap generation.");
        }

        String prompt = buildAnalyzeCareerRoadmapPrompt(candidateProfileJson);
        String jsonResponseStr = callGeminiApi(prompt);
        return parseAndValidatePhase6RoadmapJson(jsonResponseStr);
    }

    private String buildAnalyzeCareerRoadmapPrompt(String candidateProfileJson) {
        String template = """
                You are a Senior Career Advisor, Principal Architect, and Tech Curriculum Specialist.
                Analyze the following candidate profile, verified skills, skill gaps, job matches, and interview records to build a personalized Career Roadmap and Learning Plan.

                CANDIDATE DATA:
                \"\"\"
                {{CANDIDATE_DATA}}
                \"\"\"

                CRITICAL INSTRUCTIONS:
                1. Act as a senior career advisor focused on practical placement readiness.
                2. Do not invent skills unsupported by candidate data unless explicitly recommended as a gap.
                3. Prioritize skills based on target job role and user's actual job matches.
                4. Recommend realistic projects for a student / fresher / entry-level candidate.
                5. Calculate overallReadinessScore (integer 0 to 100).
                6. Create:
                   - targetRole: Target career role (e.g. "Java Backend Developer").
                   - currentLevel: Current proficiency/experience level (e.g. "Fresher / Entry Level").
                   - overallReadinessScore: Integer 0 to 100.
                   - summary: Concise 2-3 sentence strategic roadmap summary.
                   - currentSkills: List of verified current candidate skills (strings).
                   - skillGaps: List of missing/gap skills (strings).
                   - recommendedSkills: List of recommended technologies/skills to acquire (strings).
                   - roadmap: List of 3-5 learning phases. Each phase has:
                     * phase: Integer phase number (1, 2, 3...)
                     * title: Phase title (e.g., "Phase 1: Testing & Code Quality")
                     * skills: List of specific skills learned in this phase
                     * topics: List of core technical topics covered
                     * projects: List of practical mini-projects or assignments
                     * goal: Clear actionable goal for this phase
                   - projectRecommendations: List of 2-4 comprehensive, resume-worthy capstone projects (strings).
                   - jobReadinessSummary: Concise assessment of placement readiness and timeline to apply.
                   - learningRecommendations: List of 3-6 prioritized skill learning recommendations:
                     * skill: Skill name
                     * priority: Exactly "HIGH", "MEDIUM", or "LOW"
                     * reason: Why this skill is critical for placement
                     * learningObjective: What the candidate should achieve
                     * recommendedTopics: List of specific subtopics (strings)
                     * projectIdea: Concrete project idea demonstrating this skill
                     * estimatedEffort: Estimated time effort (e.g. "2-3 weeks (15 hours)")
                7. Avoid generic motivational text.
                8. Return strictly valid JSON adhering to the JSON schema below. No Markdown. No ```json blocks.

                JSON SCHEMA TO RETURN:
                {
                  "targetRole": "Java Backend Developer",
                  "currentLevel": "Fresher / Entry Level",
                  "overallReadinessScore": 75,
                  "summary": "The candidate has solid Java and Spring Boot fundamentals with high potential. Focused mastery of containerization and microservices architecture will prepare them for placement.",
                  "currentSkills": ["Java", "Spring Boot", "MySQL", "REST API", "Git"],
                  "skillGaps": ["Docker", "AWS", "JUnit 5", "Redis"],
                  "recommendedSkills": ["Docker", "Kubernetes", "Redis", "Kafka", "AWS"],
                  "roadmap": [
                    {
                      "phase": 1,
                      "title": "Phase 1: Automated Testing & Code Quality",
                      "skills": ["JUnit 5", "Mockito", "Testcontainers"],
                      "topics": ["Unit Testing", "Mocking dependencies", "Spring Boot Test"],
                      "projects": ["Comprehensive test suite for REST API"],
                      "goal": "Master test-driven development and code quality standards."
                    },
                    {
                      "phase": 2,
                      "title": "Phase 2: Database Caching & Performance",
                      "skills": ["Redis", "MySQL Indexing", "Connection Pooling"],
                      "topics": ["Redis Cache-Aside", "Query Optimization", "HikariCP"],
                      "projects": ["High-throughput cached product catalog"],
                      "goal": "Build scalable, high-performance database interactions."
                    },
                    {
                      "phase": 3,
                      "title": "Phase 3: Containerization & Cloud Deployment",
                      "skills": ["Docker", "Docker Compose", "AWS Basics"],
                      "topics": ["Dockerfile creation", "Multi-container orchestration", "EC2 deployment"],
                      "projects": ["Full containerized Spring Boot + MySQL + Redis stack"],
                      "goal": "Deploy production-ready containerized backend applications."
                    }
                  ],
                  "projectRecommendations": [
                    "Full Stack Microservices Platform with Spring Boot, Redis Caching, and Docker",
                    "E-Commerce Backend with Secure JWT Auth and Payment Gateway Integration"
                  ],
                  "jobReadinessSummary": "Candidate is 75% ready for entry-level Java backend roles and can start applying within 4 weeks of focused preparation.",
                  "learningRecommendations": [
                    {
                      "skill": "Docker & Containerization",
                      "priority": "HIGH",
                      "reason": "Crucial for modern backend deployments and CI/CD pipelines.",
                      "learningObjective": "Build Dockerfiles, manage multi-container applications with Docker Compose.",
                      "recommendedTopics": ["Dockerfile", "Docker Compose", "Multi-stage builds", "Volume mapping"],
                      "projectIdea": "Containerize Spring Boot + MySQL application with Docker Compose",
                      "estimatedEffort": "1-2 weeks (12 hours)"
                    },
                    {
                      "skill": "Redis Caching",
                      "priority": "MEDIUM",
                      "reason": "Improves application throughput and reduces database query latency.",
                      "learningObjective": "Implement cache-aside pattern with Spring Cache and Redis.",
                      "recommendedTopics": ["Spring Cache Annotations", "Redis Data Structures", "TTL & Eviction"],
                      "projectIdea": "Add Redis caching to a product catalog REST API",
                      "estimatedEffort": "1 week (8 hours)"
                    }
                  ]
                }

                Return ONLY the JSON object.
                """;
        return template.replace("{{CANDIDATE_DATA}}", candidateProfileJson);
    }

    private CareerRoadmapDto parseAndValidatePhase6RoadmapJson(String jsonText) {
        if (jsonText == null || jsonText.trim().isEmpty()) {
            throw new AIServiceException("AI service returned empty career roadmap response.");
        }

        String cleanedJson = jsonText.trim();
        if (cleanedJson.startsWith("```json")) {
            cleanedJson = cleanedJson.substring(7);
        } else if (cleanedJson.startsWith("```")) {
            cleanedJson = cleanedJson.substring(3);
        }
        if (cleanedJson.endsWith("```")) {
            cleanedJson = cleanedJson.substring(0, cleanedJson.length() - 3);
        }
        cleanedJson = cleanedJson.trim();

        try {
            CareerRoadmapDto dto = objectMapper.readValue(cleanedJson, CareerRoadmapDto.class);

            // Validate & clamp overallReadinessScore
            if (dto.getOverallReadinessScore() == null) {
                dto.setOverallReadinessScore(70);
            } else {
                dto.setOverallReadinessScore(Math.max(0, Math.min(100, dto.getOverallReadinessScore())));
            }

            if (dto.getTargetRole() == null || dto.getTargetRole().trim().isEmpty()) {
                dto.setTargetRole("Software Developer");
            }
            if (dto.getCurrentLevel() == null || dto.getCurrentLevel().trim().isEmpty()) {
                dto.setCurrentLevel("Fresher / Entry Level");
            }
            if (dto.getSummary() == null || dto.getSummary().trim().isEmpty()) {
                dto.setSummary("Personalized AI career roadmap for " + dto.getTargetRole() + ".");
            }
            if (dto.getJobReadinessSummary() == null || dto.getJobReadinessSummary().trim().isEmpty()) {
                dto.setJobReadinessSummary("Placement readiness score: " + dto.getOverallReadinessScore() + "%.");
            }

            if (dto.getCurrentSkills() == null) dto.setCurrentSkills(Collections.emptyList());
            if (dto.getSkillGaps() == null) dto.setSkillGaps(Collections.emptyList());
            if (dto.getRecommendedSkills() == null) dto.setRecommendedSkills(Collections.emptyList());
            if (dto.getProjectRecommendations() == null) dto.setProjectRecommendations(Collections.emptyList());

            // Validate roadmap phases
            if (dto.getRoadmap() == null || dto.getRoadmap().isEmpty()) {
                dto.setRoadmap(List.of(
                        CareerRoadmapDto.RoadmapPhaseDto.builder()
                                .phase(1)
                                .title("Phase 1: Core Foundation & Technical Mastery")
                                .skills(dto.getRecommendedSkills().isEmpty() ? List.of("Core Programming", "Data Structures") : dto.getRecommendedSkills())
                                .topics(List.of("Core Concepts", "Algorithms", "Best Practices"))
                                .projects(List.of("Foundation Capstone Project"))
                                .goal("Establish core technical competencies required for " + dto.getTargetRole() + ".")
                                .build()
                ));
            } else {
                int pNum = 1;
                for (CareerRoadmapDto.RoadmapPhaseDto phase : dto.getRoadmap()) {
                    if (phase.getPhase() == null || phase.getPhase() <= 0) phase.setPhase(pNum++);
                    if (phase.getTitle() == null || phase.getTitle().trim().isEmpty()) phase.setTitle("Phase " + phase.getPhase() + ": Skill Mastery");
                    if (phase.getSkills() == null) phase.setSkills(Collections.emptyList());
                    if (phase.getTopics() == null) phase.setTopics(Collections.emptyList());
                    if (phase.getProjects() == null) phase.setProjects(Collections.emptyList());
                    if (phase.getGoal() == null || phase.getGoal().trim().isEmpty()) phase.setGoal("Achieve placement readiness for this phase.");
                }
            }

            // Validate learningRecommendations
            if (dto.getLearningRecommendations() == null) {
                dto.setLearningRecommendations(Collections.emptyList());
            } else {
                for (LearningRecommendationDto rec : dto.getLearningRecommendations()) {
                    if (rec.getSkill() == null || rec.getSkill().trim().isEmpty()) rec.setSkill("Technical Skill");
                    if (rec.getPriority() == null || (!rec.getPriority().equalsIgnoreCase("HIGH")
                            && !rec.getPriority().equalsIgnoreCase("MEDIUM")
                            && !rec.getPriority().equalsIgnoreCase("LOW"))) {
                        rec.setPriority("MEDIUM");
                    } else {
                        rec.setPriority(rec.getPriority().toUpperCase());
                    }
                    if (rec.getReason() == null) rec.setReason("Important skill for " + dto.getTargetRole());
                    if (rec.getLearningObjective() == null) rec.setLearningObjective("Gain hands-on proficiency in " + rec.getSkill());
                    if (rec.getRecommendedTopics() == null) rec.setRecommendedTopics(Collections.emptyList());
                    if (rec.getProjectIdea() == null) rec.setProjectIdea("Build a practical project using " + rec.getSkill());
                    if (rec.getEstimatedEffort() == null) rec.setEstimatedEffort("1-2 weeks");
                }
            }

            return dto;
        } catch (Exception e) {
            log.error("Failed to parse Phase 6 AI Career Roadmap JSON response: {}. Raw output: {}", e.getMessage(), jsonText);
            throw new AIServiceException("AI service produced a malformed career roadmap output. Please retry.");
        }
    }

    public JobPreparationDto analyzeJobPreparation(
            String candidateJson,
            Job job,
            String jobMatchJson,
            String careerRoadmapJson) {

        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.error("Gemini API key is not configured");
            throw new AIServiceException("Gemini API configuration is missing. Please set the GEMINI_API_KEY environment variable on the backend.");
        }

        if (job == null) {
            throw new AIServiceException("Job details are required for job preparation analysis.");
        }

        Map<String, Object> jobMap = new HashMap<>();
        jobMap.put("id", job.getId());
        jobMap.put("title", job.getTitle());
        jobMap.put("company", job.getCompany());
        jobMap.put("location", job.getLocation());
        jobMap.put("experienceLevel", job.getExperienceLevel());
        jobMap.put("requiredSkills", job.getRequiredSkills());
        jobMap.put("description", job.getDescription());
        String jobJson = toJson(jobMap);

        String prompt = buildJobPreparationPrompt(candidateJson, jobJson, jobMatchJson, careerRoadmapJson);
        String jsonResponseStr = callGeminiApi(prompt);
        return parseAndValidateJobPreparationJson(jsonResponseStr, job.getTitle());
    }

    private String buildJobPreparationPrompt(String candidateJson, String jobJson, String jobMatchJson, String careerRoadmapJson) {
        String template = """
                You are an Expert Technical Recruiter, Principal Engineering Interviewer, and Career Coach.
                Create a highly actionable, personalized AI Job Preparation & Interview Plan for a candidate aspiring to interview for the specified job.

                CANDIDATE BACKGROUND & VERIFIED SKILLS:
                \"\"\"
                {{CANDIDATE_DATA}}
                \"\"\"

                TARGET JOB OPENING:
                \"\"\"
                {{JOB_DETAILS}}
                \"\"\"

                AI JOB MATCH ANALYSIS:
                \"\"\"
                {{JOB_MATCH}}
                \"\"\"

                CAREER ROADMAP & LEARNING PROGRESS:
                \"\"\"
                {{CAREER_ROADMAP}}
                \"\"\"

                CRITICAL INSTRUCTIONS:
                1. Prioritize actual missing skills and qualifications required by the selected job.
                2. Focus on practical placement and interview preparation tailored specifically to this company and role.
                3. Do not generate generic advice. Ensure concrete, actionable topics, tasks, and questions.
                4. Return strictly valid JSON adhering to the JSON schema below. No Markdown. No ```json blocks.
                5. Schema fields:
                   - readinessScore: Integer 0 to 100 representing readiness for this specific job.
                   - preparationLevel: Exactly "BEGINNER", "INTERMEDIATE", or "ADVANCED".
                   - summary: Executive 2-3 sentence overview of candidate's fit and prioritized preparation focus.
                   - strengths: List of candidate's verified strengths matching this job (strings).
                   - skillGaps: List of critical missing skills or technologies for this job (strings).
                   - technicalTopics: List of core technical concepts/frameworks to revise (strings).
                   - interviewTopics: List of common interview domains/topics for this role (strings).
                   - codingTopics: List of DSA / problem solving / live coding patterns to practice (strings).
                   - projectTopics: List of talking points and enhancements for candidate projects (strings).
                   - behavioralTopics: List of situational / leadership / STAR format topics (strings).
                   - preparationPlan: 7 to 14 days of structured preparation. Each day must specify:
                     * day (Integer: 1, 2, 3...)
                     * title (String: e.g., "Day 1: Spring Boot Internals & Dependency Injection")
                     * focus (String: core focus for the day)
                     * topics (List of strings)
                     * tasks (List of actionable tasks/exercises)
                     * practiceQuestions (List of specific interview questions to practice)
                   - recommendedQuestions: 4 to 8 high-yield interview questions across domains. Each question has:
                     * question (String)
                     * type (Exactly "TECHNICAL", "CODING", "BEHAVIORAL", or "PROJECT")
                     * suggestedAnswer (String: high-level bullet points or model answer structure)
                     * keyConcept (String: key concept being evaluated)

                JSON SCHEMA TO RETURN:
                {
                  "readinessScore": 75,
                  "preparationLevel": "INTERMEDIATE",
                  "summary": "Candidate matches 75% of core requirements for the target role. Concentrating on core technical depth, database caching, and live coding DSA will maximize placement success.",
                  "strengths": ["Core Programming", "REST API Development", "Database Modeling"],
                  "skillGaps": ["Containerization", "Distributed Caching", "API Security"],
                  "technicalTopics": ["Framework Architecture", "Transaction Propagation", "Database Indexing", "Caching Strategies"],
                  "interviewTopics": ["System Design Basics", "API Security & JWT", "Concurrency & Threading", "Clean Code & Refactoring"],
                  "codingTopics": ["Two Pointers", "Hash Maps & Sets", "Binary Search", "Sliding Window", "Trees & Graphs"],
                  "projectTopics": ["Explain project architecture & trade-offs", "How do you handle authentication and session management?"],
                  "behavioralTopics": ["Tell me about a challenging bug you fixed", "How you handle tight delivery deadlines"],
                  "preparationPlan": [
                    {
                      "day": 1,
                      "title": "Day 1: Core Fundamentals & Language Internals",
                      "focus": "Data structures, memory model, and language features",
                      "topics": ["HashMap Internals", "Collections Framework", "Garbage Collection"],
                      "tasks": ["Solve 3 LeetCode problems on HashMaps", "Review language concurrency basics"],
                      "practiceQuestions": ["Explain HashMap collision handling", "What are fail-fast vs fail-safe iterators?"]
                    },
                    {
                      "day": 2,
                      "title": "Day 2: Framework & API Architecture",
                      "focus": "RESTful conventions, Dependency Injection, and Security",
                      "topics": ["Dependency Injection", "REST Best Practices", "JWT Filter Chain"],
                      "tasks": ["Implement a secured sample endpoint", "Review HTTP status codes"],
                      "practiceQuestions": ["What is the difference between @Component and @Bean?", "How does authentication middleware work?"]
                    }
                  ],
                  "recommendedQuestions": [
                    {
                      "question": "How do you handle concurrency and race conditions in a high-traffic backend service?",
                      "type": "TECHNICAL",
                      "suggestedAnswer": "Use atomic variables, synchronization blocks, optimistic/pessimistic database locking, or distributed locks via Redis depending on the scope.",
                      "keyConcept": "Concurrency & Distributed Locking"
                    }
                  ]
                }

                Return ONLY the JSON object.
                """;
        return template.replace("{{CANDIDATE_DATA}}", candidateJson != null ? candidateJson : "{}")
                       .replace("{{JOB_DETAILS}}", jobJson != null ? jobJson : "{}")
                       .replace("{{JOB_MATCH}}", jobMatchJson != null ? jobMatchJson : "{}")
                       .replace("{{CAREER_ROADMAP}}", careerRoadmapJson != null ? careerRoadmapJson : "{}");
    }

    private JobPreparationDto parseAndValidateJobPreparationJson(String jsonText, String jobTitle) {
        if (jsonText == null || jsonText.trim().isEmpty()) {
            throw new AIServiceException("AI service returned empty job preparation response.");
        }

        String cleanedJson = jsonText.trim();
        if (cleanedJson.startsWith("```json")) {
            cleanedJson = cleanedJson.substring(7);
        } else if (cleanedJson.startsWith("```")) {
            cleanedJson = cleanedJson.substring(3);
        }
        if (cleanedJson.endsWith("```")) {
            cleanedJson = cleanedJson.substring(0, cleanedJson.length() - 3);
        }
        cleanedJson = cleanedJson.trim();

        try {
            JobPreparationDto dto = objectMapper.readValue(cleanedJson, JobPreparationDto.class);

            // Clamp readinessScore
            if (dto.getReadinessScore() == null) {
                dto.setReadinessScore(70);
            } else {
                dto.setReadinessScore(Math.max(0, Math.min(100, dto.getReadinessScore())));
            }

            // Normalize preparationLevel
            if (dto.getPreparationLevel() == null || (!dto.getPreparationLevel().equalsIgnoreCase("BEGINNER")
                    && !dto.getPreparationLevel().equalsIgnoreCase("INTERMEDIATE")
                    && !dto.getPreparationLevel().equalsIgnoreCase("ADVANCED"))) {
                dto.setPreparationLevel(dto.getReadinessScore() >= 80 ? "ADVANCED" : dto.getReadinessScore() >= 55 ? "INTERMEDIATE" : "BEGINNER");
            } else {
                dto.setPreparationLevel(dto.getPreparationLevel().toUpperCase());
            }

            if (dto.getSummary() == null || dto.getSummary().trim().isEmpty()) {
                dto.setSummary("Personalized job preparation plan for " + (jobTitle != null ? jobTitle : "target role") + ".");
            }

            if (dto.getStrengths() == null) dto.setStrengths(Collections.emptyList());
            if (dto.getSkillGaps() == null) dto.setSkillGaps(Collections.emptyList());
            if (dto.getTechnicalTopics() == null) dto.setTechnicalTopics(Collections.emptyList());
            if (dto.getInterviewTopics() == null) dto.setInterviewTopics(Collections.emptyList());
            if (dto.getCodingTopics() == null) dto.setCodingTopics(Collections.emptyList());
            if (dto.getProjectTopics() == null) dto.setProjectTopics(Collections.emptyList());
            if (dto.getBehavioralTopics() == null) dto.setBehavioralTopics(Collections.emptyList());

            // Validate preparationPlan
            if (dto.getPreparationPlan() == null || dto.getPreparationPlan().isEmpty()) {
                dto.setPreparationPlan(List.of(
                        JobPreparationDto.PreparationDayDto.builder()
                                .day(1)
                                .title("Day 1: Technical & Core Foundations")
                                .focus("Master core technical concepts required for " + (jobTitle != null ? jobTitle : "the role"))
                                .topics(List.of("Core Concepts", "Problem Solving", "Architecture"))
                                .tasks(List.of("Revise foundational concepts", "Solve 3 technical interview problems"))
                                .practiceQuestions(List.of("Explain the core architecture of your primary language / stack."))
                                .build()
                ));
            } else {
                int dayNum = 1;
                for (JobPreparationDto.PreparationDayDto day : dto.getPreparationPlan()) {
                    if (day.getDay() == null || day.getDay() <= 0) day.setDay(dayNum++);
                    if (day.getTitle() == null || day.getTitle().trim().isEmpty()) day.setTitle("Day " + day.getDay() + ": Focus & Revision");
                    if (day.getFocus() == null || day.getFocus().trim().isEmpty()) day.setFocus("Comprehensive preparation for " + (jobTitle != null ? jobTitle : "target job"));
                    if (day.getTopics() == null) day.setTopics(Collections.emptyList());
                    if (day.getTasks() == null) day.setTasks(Collections.emptyList());
                    if (day.getPracticeQuestions() == null) day.setPracticeQuestions(Collections.emptyList());
                }
            }

            // Validate recommendedQuestions
            if (dto.getRecommendedQuestions() == null) {
                dto.setRecommendedQuestions(Collections.emptyList());
            } else {
                for (JobPreparationDto.RecommendedQuestionDto q : dto.getRecommendedQuestions()) {
                    if (q.getQuestion() == null || q.getQuestion().trim().isEmpty()) q.setQuestion("Explain a key technical challenge you encountered in past projects.");
                    if (q.getType() == null || (!q.getType().equalsIgnoreCase("TECHNICAL")
                            && !q.getType().equalsIgnoreCase("CODING")
                            && !q.getType().equalsIgnoreCase("BEHAVIORAL")
                            && !q.getType().equalsIgnoreCase("PROJECT"))) {
                        q.setType("TECHNICAL");
                    } else {
                        q.setType(q.getType().toUpperCase());
                    }
                    if (q.getSuggestedAnswer() == null) q.setSuggestedAnswer("Structure your response using the STAR technique or clear technical principles.");
                    if (q.getKeyConcept() == null) q.setKeyConcept("Core Technical Problem Solving");
                }
            }

            return dto;
        } catch (Exception e) {
            log.error("Failed to parse AI Job Preparation JSON response: {}. Raw output: {}", e.getMessage(), jsonText);
            throw new AIServiceException("AI service produced a malformed job preparation output. Please retry.");
        }
    }

    public CareerRoadmapDto generateCareerRoadmap(
            String candidateProfileJson,
            String skillGapJson,
            String targetRole,
            Integer durationWeeks) {

        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.error("Gemini API key is not configured");
            throw new AIServiceException("Gemini API configuration is missing. Please set the GEMINI_API_KEY environment variable on the backend.");
        }

        if (targetRole == null || targetRole.trim().isEmpty()) {
            throw new AIServiceException("Target role is required for career roadmap generation.");
        }

        int effectiveDuration = (durationWeeks != null && durationWeeks >= 2 && durationWeeks <= 52) ? durationWeeks : 12;

        String prompt = buildCareerRoadmapPrompt(candidateProfileJson, skillGapJson, targetRole.trim(), effectiveDuration);
        String jsonResponseStr = callGeminiApi(prompt);
        return parseAndValidateRoadmapJson(jsonResponseStr, targetRole.trim(), effectiveDuration);
    }

    private String buildCareerRoadmapPrompt(String candidateProfileJson, String skillGapJson, String targetRole, int durationWeeks) {
        return """
                You are a Principal Software Engineer, Staff Engineering Mentor, and Tech Hiring Director.
                Generate a comprehensive, highly personalized AI Career Roadmap and Weekly Learning Planner for a candidate aspiring to become a "%s".
                The planned roadmap duration is %d weeks.
                
                CANDIDATE BACKGROUND & RESUME PROFILE:
                \"\"\"
                %s
                \"\"\"
                
                SKILL GAPS & JOB MARKET CONTEXT:
                \"\"\"
                %s
                \"\"\"
                
                CRITICAL INSTRUCTIONS:
                1. Analyze the candidate's verified skills and skill gaps against real-world expectations for "%s".
                2. Prioritize high-impact modern industry technologies, architectures, and design patterns.
                3. Create:
                   - summary: A strategic 2-3 sentence overview evaluating the candidate's baseline and the path to target role mastery.
                   - skillsToLearn: 4-8 prioritized skills with category, priority (HIGH, MEDIUM, LOW), estimated hours, and career justification.
                   - projects: 2-4 hands-on, resume-worthy capstone projects with difficulty, estimated hours, and skills demonstrated.
                   - interviewTopics: Key technical and behavioral preparation topics across DSA, Core Language, Frameworks, Databases, and System Design with 2-4 representative interview questions each.
                   - certifications: 1-3 genuine, industry-recognized certifications (or empty if practical experience is preferred).
                   - weeklyPlan: Exactly %d weeks (numbered 1 through %d). Each week must specify:
                     * weekNumber (Integer)
                     * theme (e.g., "Week 1: Advanced Java Concurrency & Collections")
                     * goals (Concrete weekly learning objectives)
                     * tasks: 2-4 actionable, distinct tasks. Each task must specify:
                       - title: Concrete action item (e.g. "Implement Thread pool with CompletableFuture")
                       - description: Clear learning or building activity
                       - category: One of "SKILL", "PROJECT", "INTERVIEW", "CERTIFICATION", "DSA", "SYSTEM_DESIGN", "SOFT_SKILL"
                       - priority: "HIGH", "MEDIUM", or "LOW"
                       - estimatedHours: Integer hours (e.g. 4, 6)
                       - resourceUrl: Optional helpful reference URL or documentation link
                   - milestones: 3-5 major chronological career milestones with timeframe, title, description, and target outcome.
                4. Return strictly valid JSON adhering to the JSON schema below.
                
                JSON SCHEMA TO RETURN:
                {
                  "targetRole": "%s",
                  "currentLevel": "Fresher / Entry Level",
                  "summary": "Strategic overview of candidate growth plan towards role mastery.",
                  "estimatedDurationWeeks": %d,
                  "skillsToLearn": [
                    {
                      "name": "Spring Security & OAuth2",
                      "category": "Backend Security",
                      "priority": "HIGH",
                      "estimatedHours": 25,
                      "reason": "Crucial for securing microservices in enterprise applications."
                    }
                  ],
                  "projects": [
                    {
                      "title": "E-Commerce Microservices Engine",
                      "description": "Multi-module distributed backend with API gateway and JWT authentication.",
                      "skillsDemonstrated": ["Spring Boot", "Docker", "JWT", "MySQL"],
                      "difficulty": "Intermediate",
                      "estimatedHours": 35
                    }
                  ],
                  "interviewTopics": [
                    {
                      "topicName": "Spring Framework Internals",
                      "category": "SPRING_BOOT",
                      "keyQuestions": [
                        "Explain Spring Bean lifecycle and scopes",
                        "How does @Transactional work with dynamic proxies?"
                      ]
                    }
                  ],
                  "certifications": [
                    {
                      "name": "Spring Certified Professional",
                      "provider": "VMware / Broadcom",
                      "importance": "Recommended",
                      "url": "https://spring.io/certifications"
                    }
                  ],
                  "weeklyPlan": [
                    {
                      "weekNumber": 1,
                      "theme": "Advanced Java & Memory Model",
                      "goals": "Master JVM internals, memory management, and modern Java 17/21 features.",
                      "tasks": [
                        {
                          "title": "Study Java Memory Model and Garbage Collection",
                          "description": "Understand Heap vs Stack, GC algorithms (G1, ZGC), and profiling with JConsole.",
                          "category": "SKILL",
                          "priority": "HIGH",
                          "estimatedHours": 6,
                          "resourceUrl": "https://docs.oracle.com/en/java/"
                        }
                      ]
                    }
                  ],
                  "milestones": [
                    {
                      "timeframe": "Month 1",
                      "title": "Core Foundations & Database Mastery",
                      "description": "Solidify multi-threading, advanced SQL indexing, and clean API design.",
                      "targetOutcome": "Build and benchmark high-throughput REST APIs."
                    }
                  ]
                }
                
                Return ONLY the JSON object.
                """.formatted(targetRole, durationWeeks, candidateProfileJson, skillGapJson, targetRole, durationWeeks, durationWeeks, targetRole, durationWeeks);
    }

    private CareerRoadmapDto parseAndValidateRoadmapJson(String jsonText, String targetRole, int durationWeeks) {
        if (jsonText == null || jsonText.trim().isEmpty()) {
            throw new AIServiceException("AI service returned empty career roadmap response.");
        }

        String cleanedJson = jsonText.trim();
        if (cleanedJson.startsWith("```json")) {
            cleanedJson = cleanedJson.substring(7);
        } else if (cleanedJson.startsWith("```")) {
            cleanedJson = cleanedJson.substring(3);
        }
        if (cleanedJson.endsWith("```")) {
            cleanedJson = cleanedJson.substring(0, cleanedJson.length() - 3);
        }
        cleanedJson = cleanedJson.trim();

        try {
            CareerRoadmapDto dto = objectMapper.readValue(cleanedJson, CareerRoadmapDto.class);

            if (dto.getTargetRole() == null || dto.getTargetRole().trim().isEmpty()) {
                dto.setTargetRole(targetRole);
            }
            if (dto.getCurrentLevel() == null || dto.getCurrentLevel().trim().isEmpty()) {
                dto.setCurrentLevel("Entry Level / Mid-Level");
            }
            if (dto.getEstimatedDurationWeeks() == null || dto.getEstimatedDurationWeeks() <= 0) {
                dto.setEstimatedDurationWeeks(durationWeeks);
            }
            if (dto.getSummary() == null || dto.getSummary().trim().isEmpty()) {
                dto.setSummary("Personalized " + dto.getEstimatedDurationWeeks() + "-week learning roadmap designed for " + dto.getTargetRole() + ".");
            }

            if (dto.getSkillsToLearn() == null) dto.setSkillsToLearn(Collections.emptyList());
            if (dto.getProjects() == null) dto.setProjects(Collections.emptyList());
            if (dto.getInterviewTopics() == null) dto.setInterviewTopics(Collections.emptyList());
            if (dto.getCertifications() == null) dto.setCertifications(Collections.emptyList());
            if (dto.getMilestones() == null) dto.setMilestones(Collections.emptyList());

            if (dto.getWeeklyPlan() == null) {
                dto.setWeeklyPlan(Collections.emptyList());
            } else {
                int week = 1;
                for (CareerRoadmapDto.WeeklyPlanDto plan : dto.getWeeklyPlan()) {
                    if (plan.getWeekNumber() == null || plan.getWeekNumber() <= 0) {
                        plan.setWeekNumber(week++);
                    }
                    if (plan.getTasks() == null) {
                        plan.setTasks(Collections.emptyList());
                    } else {
                        for (CareerRoadmapDto.WeeklyTaskDto task : plan.getTasks()) {
                            if (task.getTitle() == null || task.getTitle().trim().isEmpty()) {
                                task.setTitle("Complete weekly milestone task");
                            }
                            if (task.getCategory() == null || task.getCategory().trim().isEmpty()) {
                                task.setCategory("SKILL");
                            }
                            if (task.getPriority() == null || (!task.getPriority().equalsIgnoreCase("HIGH")
                                    && !task.getPriority().equalsIgnoreCase("MEDIUM")
                                    && !task.getPriority().equalsIgnoreCase("LOW"))) {
                                task.setPriority("MEDIUM");
                            } else {
                                task.setPriority(task.getPriority().toUpperCase());
                            }
                            if (task.getEstimatedHours() == null || task.getEstimatedHours() <= 0) {
                                task.setEstimatedHours(5);
                            }
                        }
                    }
                }
            }

            return dto;
        } catch (Exception e) {
            log.error("Failed to parse AI Career Roadmap JSON response: {}. Raw output: {}", e.getMessage(), jsonText);
            throw new AIServiceException("AI service produced a malformed career roadmap output. Please retry the roadmap generation.");
        }
    }

    public InterviewQuestionDto generateInterviewQuestion(
            String candidateProfileJson,
            String targetRole,
            String jobDetailsJson,
            String interviewType,
            String difficulty,
            int questionNumber,
            int totalQuestions,
            List<String> previousQuestions) {

        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.error("Gemini API key is not configured");
            throw new AIServiceException("Gemini API configuration is missing. Please set the GEMINI_API_KEY environment variable on the backend.");
        }

        String prompt = buildInterviewQuestionPrompt(candidateProfileJson, targetRole, jobDetailsJson, interviewType, difficulty, questionNumber, totalQuestions, previousQuestions);
        String jsonResponseStr = callGeminiApi(prompt);
        return parseAndValidateInterviewQuestionJson(jsonResponseStr, interviewType, difficulty);
    }

    private String buildInterviewQuestionPrompt(
            String candidateProfileJson,
            String targetRole,
            String jobDetailsJson,
            String interviewType,
            String difficulty,
            int questionNumber,
            int totalQuestions,
            List<String> previousQuestions) {

        String prevQuestionsStr = (previousQuestions != null && !previousQuestions.isEmpty())
                ? String.join("\n- ", previousQuestions)
                : "None (this is the first question).";

        return """
                You are a Senior Engineering Hiring Manager and Technical Interviewer conducting a mock interview for the role of "%s".
                
                INTERVIEW CONFIGURATION:
                - Interview Type: %s (TECHNICAL, BEHAVIORAL, HR, or MIXED)
                - Difficulty: %s (EASY, MEDIUM, or HARD)
                - Current Question Number: %d of %d
                
                CANDIDATE BACKGROUND & RESUME SKILLS:
                \"\"\"
                %s
                \"\"\"
                
                TARGET JOB DETAILS (IF APPLICABLE):
                \"\"\"
                %s
                \"\"\"
                
                PREVIOUSLY ASKED QUESTIONS IN THIS SESSION (DO NOT REPEAT OR CLOSELY DUPLICATE THESE):
                - %s
                
                INSTRUCTIONS:
                1. Generate a single, realistic, and highly relevant interview question suited for question %d of %d.
                2. Adapt the category to match the interview type:
                   - If TECHNICAL: Focus on programming languages, frameworks, SQL, debugging, algorithms, or architecture.
                   - If BEHAVIORAL: Use the STAR methodology (Situation, Task, Action, Result) regarding teamwork, conflict, deadlines, or failures.
                   - If HR: Focus on career goals, work culture, salary/relocation expectations, or strengths/weaknesses.
                   - If MIXED: Alternate progressively between technical, practical project design, and behavioral questions.
                3. Provide 3-5 expected core topics or concepts that a strong candidate's answer should demonstrate.
                4. Return strictly valid JSON adhering to the schema below.
                
                JSON SCHEMA TO RETURN:
                {
                  "question": "Explain how you would handle race conditions and concurrency when multiple users purchase the last item in a high-traffic Spring Boot e-commerce application.",
                  "category": "TECHNICAL",
                  "difficulty": "%s",
                  "expectedTopics": ["Optimistic vs Pessimistic Locking", "Database Isolation Levels", "Redis Distributed Locks", "Thread Safety"]
                }
                
                Return ONLY the JSON object.
                """.formatted(targetRole, interviewType, difficulty, questionNumber, totalQuestions,
                candidateProfileJson, jobDetailsJson, prevQuestionsStr, questionNumber, totalQuestions, difficulty);
    }

    private InterviewQuestionDto parseAndValidateInterviewQuestionJson(String jsonText, String fallbackType, String fallbackDifficulty) {
        if (jsonText == null || jsonText.trim().isEmpty()) {
            throw new AIServiceException("AI service returned empty interview question response.");
        }

        String cleanedJson = jsonText.trim();
        if (cleanedJson.startsWith("```json")) {
            cleanedJson = cleanedJson.substring(7);
        } else if (cleanedJson.startsWith("```")) {
            cleanedJson = cleanedJson.substring(3);
        }
        if (cleanedJson.endsWith("```")) {
            cleanedJson = cleanedJson.substring(0, cleanedJson.length() - 3);
        }
        cleanedJson = cleanedJson.trim();

        try {
            InterviewQuestionDto dto = objectMapper.readValue(cleanedJson, InterviewQuestionDto.class);

            if (dto.getQuestion() == null || dto.getQuestion().trim().isEmpty()) {
                dto.setQuestion("Can you describe a challenging technical problem you solved and the architectural approach you took?");
            }
            if (dto.getCategory() == null || dto.getCategory().trim().isEmpty()) {
                dto.setCategory(fallbackType.equalsIgnoreCase("BEHAVIORAL") ? "BEHAVIORAL" : "TECHNICAL");
            }
            if (dto.getDifficulty() == null || dto.getDifficulty().trim().isEmpty()) {
                dto.setDifficulty(fallbackDifficulty != null ? fallbackDifficulty.toUpperCase() : "MEDIUM");
            }
            if (dto.getExpectedTopics() == null) {
                dto.setExpectedTopics(Collections.emptyList());
            }

            return dto;
        } catch (Exception e) {
            log.error("Failed to parse AI Interview Question JSON: {}. Raw output: {}", e.getMessage(), jsonText);
            throw new AIServiceException("AI service produced a malformed interview question. Please retry.");
        }
    }

    public InterviewEvaluationDto evaluateInterviewAnswer(
            String questionText,
            String expectedTopicsJson,
            String userAnswer,
            String targetRole,
            String difficulty,
            String category) {

        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.error("Gemini API key is not configured");
            throw new AIServiceException("Gemini API configuration is missing. Please set the GEMINI_API_KEY environment variable on the backend.");
        }

        String prompt = buildInterviewEvaluationPrompt(questionText, expectedTopicsJson, userAnswer, targetRole, difficulty, category);
        String jsonResponseStr = callGeminiApi(prompt);
        return parseAndValidateInterviewEvaluationJson(jsonResponseStr);
    }

    private String buildInterviewEvaluationPrompt(
            String questionText,
            String expectedTopicsJson,
            String userAnswer,
            String targetRole,
            String difficulty,
            String category) {

        return """
                You are a Principal Software Engineer and Expert Interview Evaluator evaluating a candidate's answer for the target role: "%s".
                
                INTERVIEW QUESTION (Category: %s, Difficulty: %s):
                \"\"\"
                %s
                \"\"\"
                
                EXPECTED TOPICS / KEY CONCEPTS:
                \"\"\"
                %s
                \"\"\"
                
                CANDIDATE'S SUBMITTED ANSWER:
                \"\"\"
                %s
                \"\"\"
                
                CRITICAL INSTRUCTIONS:
                1. Objectively evaluate the candidate's answer based on technical correctness, clarity, completeness, depth, and practical experience.
                2. Calculate realistic scores (0 to 100):
                   - score: Overall answer quality score.
                   - technicalScore: Accuracy and depth of technical/conceptual knowledge.
                   - relevanceScore: How directly the candidate answered the specific question.
                   - communicationScore: Clarity, structure, articulation, and conciseness.
                   - confidenceScore: Professional tone, conviction, and authority.
                3. If the answer is vague, trivial, or incorrect, give honest lower scores (e.g. 20-50). If comprehensive and well-reasoned, award higher scores (75-95).
                4. Provide constructive feedback (2-3 sentences), a list of 2-3 specific strengths, a list of 2-3 specific actionable improvements, and a model/ideal answer.
                5. Return strictly valid JSON adhering to the schema below.
                
                JSON SCHEMA TO RETURN:
                {
                  "score": 85,
                  "technicalScore": 88,
                  "relevanceScore": 90,
                  "communicationScore": 82,
                  "confidenceScore": 80,
                  "feedback": "The candidate demonstrated strong understanding of locking mechanisms and database transactions, explaining both optimistic and pessimistic approaches clearly.",
                  "strengths": [
                    "Correctly distinguished between optimistic and pessimistic locking",
                    "Mentioned version column implementation for optimistic concurrency"
                  ],
                  "improvements": [
                    "Could explicitly mention Redis distributed locks (Redisson) for multi-instance scaling",
                    "Elaborate on dead-lock detection and retry strategies"
                  ],
                  "idealAnswer": "In a distributed Spring Boot environment, handling concurrent purchases requires a multi-layered approach. For single database instances, optimistic locking with JPA @Version ensures zero performance penalty for reads while preventing lost updates. For high-contention flash sales or multi-instance microservices, a distributed lock using Redis (Redisson) or transactional outbox pattern combined with database-level SELECT FOR UPDATE ensures atomic inventory decrements with graceful retry policies."
                }
                
                Return ONLY the JSON object.
                """.formatted(targetRole, category, difficulty, questionText, expectedTopicsJson, userAnswer);
    }

    private InterviewEvaluationDto parseAndValidateInterviewEvaluationJson(String jsonText) {
        if (jsonText == null || jsonText.trim().isEmpty()) {
            throw new AIServiceException("AI service returned empty interview evaluation response.");
        }

        String cleanedJson = jsonText.trim();
        if (cleanedJson.startsWith("```json")) {
            cleanedJson = cleanedJson.substring(7);
        } else if (cleanedJson.startsWith("```")) {
            cleanedJson = cleanedJson.substring(3);
        }
        if (cleanedJson.endsWith("```")) {
            cleanedJson = cleanedJson.substring(0, cleanedJson.length() - 3);
        }
        cleanedJson = cleanedJson.trim();

        try {
            InterviewEvaluationDto dto = objectMapper.readValue(cleanedJson, InterviewEvaluationDto.class);

            // Clamp scores between 0 and 100
            dto.setScore(clampScore(dto.getScore(), 70));
            dto.setTechnicalScore(clampScore(dto.getTechnicalScore(), dto.getScore()));
            dto.setRelevanceScore(clampScore(dto.getRelevanceScore(), dto.getScore()));
            dto.setCommunicationScore(clampScore(dto.getCommunicationScore(), dto.getScore()));
            dto.setConfidenceScore(clampScore(dto.getConfidenceScore(), dto.getScore()));

            if (dto.getFeedback() == null || dto.getFeedback().trim().isEmpty()) {
                dto.setFeedback("Answer evaluated with an overall score of " + dto.getScore() + "/100.");
            }
            if (dto.getStrengths() == null) dto.setStrengths(Collections.emptyList());
            if (dto.getImprovements() == null) dto.setImprovements(Collections.emptyList());
            if (dto.getIdealAnswer() == null || dto.getIdealAnswer().trim().isEmpty()) {
                dto.setIdealAnswer("A strong answer directly addresses the core concepts with concrete examples and industry standard practices.");
            }

            return dto;
        } catch (Exception e) {
            log.error("Failed to parse AI Interview Evaluation JSON: {}. Raw output: {}", e.getMessage(), jsonText);
            throw new AIServiceException("AI service produced a malformed interview evaluation. Please retry.");
        }
    }

    public InterviewFinalEvaluationDto generateFinalInterviewEvaluation(
            String targetRole,
            String difficulty,
            String candidateProfileJson,
            List<Map<String, Object>> evaluatedQuestions) {

        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.error("Gemini API key is not configured");
            throw new AIServiceException("Gemini API configuration is missing. Please set the GEMINI_API_KEY environment variable on the backend.");
        }

        String questionsSummaryJson = toJson(evaluatedQuestions);
        String prompt = buildFinalInterviewEvaluationPrompt(targetRole, difficulty, candidateProfileJson, questionsSummaryJson);
        String jsonResponseStr = callGeminiApi(prompt);
        return parseAndValidateFinalInterviewEvaluationJson(jsonResponseStr);
    }

    private String buildFinalInterviewEvaluationPrompt(
            String targetRole,
            String difficulty,
            String candidateProfileJson,
            String questionsSummaryJson) {

        return """
                You are a Director of Engineering and Hiring Committee Chair conducting the final review of a candidate's completed mock interview for "%s" (Difficulty: %s).
                
                CANDIDATE BACKGROUND:
                \"\"\"
                %s
                \"\"\"
                
                ALL QUESTIONS, ANSWERS, AND EVALUATIONS FROM THIS SESSION:
                \"\"\"
                %s
                \"\"\"
                
                CRITICAL INSTRUCTIONS:
                1. Perform a holistic assessment of the candidate's overall performance throughout the entire interview session.
                2. Provide:
                   - summary: An executive evaluation summary (3-4 sentences) capturing strengths, technical depth, communication style, and hiring readiness.
                   - strengths: 3-5 major recurring positive traits and demonstrated competencies.
                   - weaknesses: 3-5 specific gaps or areas where the candidate struggled.
                   - recommendations: 3-5 concrete, actionable study topics and interview prep tips before real company interviews.
                3. Return strictly valid JSON adhering to the schema below.
                
                JSON SCHEMA TO RETURN:
                {
                  "overallScore": 82,
                  "technicalScore": 85,
                  "communicationScore": 80,
                  "confidenceScore": 78,
                  "problemSolvingScore": 84,
                  "summary": "Candidate demonstrated solid grasp of core backend architectures and databases with strong conceptual articulation. To elevate to top-tier standards, further practice on distributed caching and trade-off analysis under system constraints is recommended.",
                  "strengths": [
                    "Clear understanding of relational databases and transaction isolation",
                    "Structured communication using clean examples"
                  ],
                  "weaknesses": [
                    "Hesitant when discussing asynchronous messaging patterns",
                    "Shallow explanation of distributed system failure modes"
                  ],
                  "recommendations": [
                    "Deepen understanding of Kafka partition strategies and consumer groups",
                    "Practice answering system design questions with back-of-the-envelope calculations"
                  ]
                }
                
                Return ONLY the JSON object.
                """.formatted(targetRole, difficulty, candidateProfileJson, questionsSummaryJson);
    }

    private InterviewFinalEvaluationDto parseAndValidateFinalInterviewEvaluationJson(String jsonText) {
        if (jsonText == null || jsonText.trim().isEmpty()) {
            throw new AIServiceException("AI service returned empty final interview evaluation response.");
        }

        String cleanedJson = jsonText.trim();
        if (cleanedJson.startsWith("```json")) {
            cleanedJson = cleanedJson.substring(7);
        } else if (cleanedJson.startsWith("```")) {
            cleanedJson = cleanedJson.substring(3);
        }
        if (cleanedJson.endsWith("```")) {
            cleanedJson = cleanedJson.substring(0, cleanedJson.length() - 3);
        }
        cleanedJson = cleanedJson.trim();

        try {
            InterviewFinalEvaluationDto dto = objectMapper.readValue(cleanedJson, InterviewFinalEvaluationDto.class);

            if (dto.getSummary() == null || dto.getSummary().trim().isEmpty()) {
                dto.setSummary("Mock interview session completed with comprehensive assessment across all questions.");
            }
            if (dto.getStrengths() == null) dto.setStrengths(Collections.emptyList());
            if (dto.getWeaknesses() == null) dto.setWeaknesses(Collections.emptyList());
            if (dto.getRecommendations() == null) dto.setRecommendations(Collections.emptyList());

            return dto;
        } catch (Exception e) {
            log.error("Failed to parse AI Final Interview Evaluation JSON: {}. Raw output: {}", e.getMessage(), jsonText);
            throw new AIServiceException("AI service produced a malformed final evaluation. Please retry.");
        }
    }

    // =========================================================================
    // Phase 10: AI Career Assistant & Context-Aware Career Copilot
    // =========================================================================

    private static final String CAREER_ASSISTANT_SYSTEM_PROMPT = """
        You are an elite Personalized AI Career & Placement Copilot.
        Your mission is to provide deeply personalized, actionable, empathetic, and highly strategic career advice to candidates based on their real application data.

        CANDIDATE CAREER CONTEXT:
        {{CANDIDATE_CONTEXT}}

        CONVERSATION HISTORY:
        {{CONVERSATION_HISTORY}}

        USER QUERY:
        {{USER_MESSAGE}}

        CRITICAL INSTRUCTIONS:
        1. Analyze the user's actual career data (Profile, Resume Analysis, Skill Gap, Job Matches, Saved Jobs, Career Roadmap, Mock Interviews, Job Preparation, Career Analytics).
        2. Give personalized, actionable answers formatted cleanly with markdown (bullet points, bold text, code snippets if technical).
        3. Avoid inventing skills, jobs, scores, interviews, or achievements not present in candidate context.
        4. Clearly state when specific information is unavailable (e.g., if no resume is uploaded or no mock interviews have been completed).
        5. Give actionable next steps and reference the appropriate application feature when useful.
        6. Provide 2-4 smart suggested actions in "suggestedActions" with exact routes:
           - "/mock-interview" -> e.g. "Practice Mock Interview"
           - "/resume" -> e.g. "Improve Resume ATS Score"
           - "/skill-gap" -> e.g. "Analyze Skill Gaps"
           - "/career-roadmap" -> e.g. "Continue Career Roadmap"
           - "/preparation" -> e.g. "Start Job Preparation"
           - "/career-analytics" -> e.g. "View Career Analytics"
           - "/jobs" -> e.g. "Explore Matching Jobs"
        7. In "contextUsed", list the actual data sources referenced in your answer from: ["resume", "skills", "skillGap", "jobs", "roadmap", "mockInterview", "jobPrep", "careerAnalytics"].
        8. Return strictly valid JSON adhering to the schema below.

        OUTPUT SCHEMA (STRICT JSON ONLY):
        {
          "message": "Comprehensive, structured, and personalized markdown answer.",
          "suggestedActions": [
            {
              "label": "Practice Mock Interview",
              "route": "/mock-interview"
            },
            {
              "label": "View Career Analytics",
              "route": "/career-analytics"
            }
          ],
          "contextUsed": ["resume", "skillGap", "careerAnalytics"],
          "relevantSkills": ["Java", "Spring Boot"],
          "relevantJobs": ["Backend Engineer"],
          "preparationSuggestions": ["Review REST API principles", "Practice concurrency"]
        }
        """;

    public CareerAssistantDto generateCareerAssistantResponse(String careerContextJson,
                                                              String conversationHistoryJson,
                                                              String userMessage) {
        log.info("Requesting Gemini AI Career Assistant Copilot guidance for message length: {}", userMessage != null ? userMessage.length() : 0);

        String prompt = CAREER_ASSISTANT_SYSTEM_PROMPT
                .replace("{{CANDIDATE_CONTEXT}}", careerContextJson != null ? careerContextJson : "{}")
                .replace("{{CONVERSATION_HISTORY}}", conversationHistoryJson != null ? conversationHistoryJson : "[]")
                .replace("{{USER_MESSAGE}}", userMessage != null ? userMessage : "");

        String jsonText = callGeminiApi(prompt);
        return parseCareerAssistantResponse(jsonText);
    }

    public CareerAssistantDto analyzeCareerAssistant(String candidateContextJson,
                                                     String conversationHistoryJson,
                                                     String userMessage) {
        return generateCareerAssistantResponse(candidateContextJson, conversationHistoryJson, userMessage);
    }

    private CareerAssistantDto parseCareerAssistantResponse(String jsonText) {
        try {
            com.fasterxml.jackson.databind.JsonNode rootNode = objectMapper.readTree(jsonText);

            String message = "";
            if (rootNode.hasNonNull("message")) {
                message = rootNode.get("message").asText();
            } else if (rootNode.hasNonNull("answer")) {
                message = rootNode.get("answer").asText();
            }

            if (message.isBlank()) {
                message = "I have analyzed your career profile and goals. How else can I assist your career progression today?";
            }

            List<CareerAssistantDto.ActionItem> actions = new ArrayList<>();
            if (rootNode.has("suggestedActions") && rootNode.get("suggestedActions").isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode act : rootNode.get("suggestedActions")) {
                    if (act.isObject()) {
                        String label = act.hasNonNull("label") ? act.get("label").asText() : "Next Step";
                        String route = act.hasNonNull("route") ? act.get("route").asText() : "/dashboard";
                        actions.add(new CareerAssistantDto.ActionItem(label, route));
                    } else if (act.isTextual()) {
                        String label = act.asText();
                        String route = determineRouteForLabel(label);
                        actions.add(new CareerAssistantDto.ActionItem(label, route));
                    }
                }
            }

            List<String> contextUsed = new ArrayList<>();
            if (rootNode.has("contextUsed") && rootNode.get("contextUsed").isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode ctx : rootNode.get("contextUsed")) {
                    if (ctx.isTextual()) {
                        contextUsed.add(ctx.asText());
                    }
                }
            }

            List<String> relevantSkills = new ArrayList<>();
            if (rootNode.has("relevantSkills") && rootNode.get("relevantSkills").isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode s : rootNode.get("relevantSkills")) {
                    if (s.isTextual()) relevantSkills.add(s.asText());
                }
            }

            List<String> relevantJobs = new ArrayList<>();
            if (rootNode.has("relevantJobs") && rootNode.get("relevantJobs").isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode j : rootNode.get("relevantJobs")) {
                    if (j.isTextual()) relevantJobs.add(j.asText());
                }
            }

            List<String> prepSuggestions = new ArrayList<>();
            if (rootNode.has("preparationSuggestions") && rootNode.get("preparationSuggestions").isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode p : rootNode.get("preparationSuggestions")) {
                    if (p.isTextual()) prepSuggestions.add(p.asText());
                }
            }

            if (actions.isEmpty()) {
                actions.add(new CareerAssistantDto.ActionItem("View Career Analytics", "/career-analytics"));
                actions.add(new CareerAssistantDto.ActionItem("Practice Mock Interview", "/mock-interview"));
            }

            return CareerAssistantDto.builder()
                    .message(message)
                    .answer(message)
                    .suggestedActions(actions)
                    .contextUsed(contextUsed)
                    .relevantSkills(relevantSkills)
                    .relevantJobs(relevantJobs)
                    .preparationSuggestions(prepSuggestions)
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse Gemini AI Career Assistant JSON: {}. Raw output: {}", e.getMessage(), jsonText);
            throw new AIServiceException("AI Career Assistant produced a malformed response. Please try again.");
        }
    }

    private String determineRouteForLabel(String label) {
        if (label == null) return "/dashboard";
        String lower = label.toLowerCase();
        if (lower.contains("interview") || lower.contains("mock")) return "/mock-interview";
        if (lower.contains("resume") || lower.contains("ats")) return "/resume";
        if (lower.contains("gap") || lower.contains("skill")) return "/skill-gap";
        if (lower.contains("roadmap") || lower.contains("curriculum") || lower.contains("milestone")) return "/career-roadmap";
        if (lower.contains("prep") || lower.contains("target job")) return "/preparation";
        if (lower.contains("analytic") || lower.contains("readiness") || lower.contains("intelligence")) return "/career-analytics";
        if (lower.contains("job") || lower.contains("match") || lower.contains("opening")) return "/jobs";
        return "/dashboard";
    }

    // ==========================================
    // PHASE 9: CAREER ANALYTICS & INTELLIGENCE
    // ==========================================

    private static final String CAREER_ANALYTICS_SYSTEM_PROMPT = """
        You are an expert AI Career Intelligence & Placement Analytics engine.
        Your task is to analyze the candidate's complete career data profile (profile, resume analysis, skill gaps, job matches, mock interview history, career roadmap progress, and job preparation) and generate a comprehensive Career Readiness and Analytics Intelligence Report in structured JSON format.

        CANDIDATE PROFILE:
        {{CANDIDATE_DATA}}

        RESUME ANALYSIS & ATS:
        {{RESUME_ANALYSIS_DATA}}

        SKILL GAP ANALYSIS:
        {{SKILL_GAP_DATA}}

        JOB MATCHES & SAVED JOBS:
        {{JOB_MATCH_DATA}}

        MOCK INTERVIEW PERFORMANCE:
        {{MOCK_INTERVIEW_DATA}}

        CAREER ROADMAP PROGRESS:
        {{ROADMAP_DATA}}

        JOB PREPARATION SESSIONS:
        {{JOB_PREPARATION_DATA}}

        ANALYSIS GUIDELINES:
        1. Calculate an accurate `overallReadinessScore` (integer 0 to 100) reflecting their collective readiness across skills, resume, interview scores, and roadmap progress.
        2. Assign a `readinessLevel`: "READY" (80-100), "ALMOST READY" (60-79), "NEEDS IMPROVEMENT" (40-59), or "NEEDS PREPARATION" (0-39).
        3. Write an insightful, motivating, professional `careerSummary` (2-4 paragraphs in markdown format) highlighting where they stand, major milestones achieved, and direct areas to focus on.
        4. List top `strengths` (key competencies verified in resume/interviews/skills).
        5. List prioritized `weaknesses` with:
           - skill: string
           - currentStatus: string
           - priority: "HIGH" | "MEDIUM" | "LOW"
           - recommendedAction: string
        6. Break down `skillProgress` categorized into technical, frameworks, databases, tools, soft skills with proficiencyPercentage (0-100) and status ("STRONG" | "DEVELOPING" | "MISSING").
        7. Synthesize `interviewProgress`:
           - averageScore: double (0-100)
           - technicalScore: double (0-100)
           - behavioralScore: double (0-100)
           - communicationScore: double (0-100)
           - totalInterviews: integer
           - trend: "IMPROVING" | "STABLE" | "NEEDS_PRACTICE" | "NOT_STARTED"
        8. Synthesize `jobSearchProgress`:
           - jobsMatched: integer
           - savedJobs: integer
           - appliedJobs: integer
           - preparationSessions: integer
           - avgMatchScore: double (0-100)
        9. Synthesize `roadmapProgress`:
           - completedMilestones: integer
           - totalMilestones: integer
           - currentMilestone: string
           - upcomingMilestone: string
           - completionPercentage: integer (0-100)
        10. Provide 3-5 high-impact `recommendations` with:
           - priority: "HIGH" | "MEDIUM" | "LOW"
           - action: concise action title
           - reason: why this matters
           - relatedFeature: "SKILL_GAP" | "CAREER_ROADMAP" | "MOCK_INTERVIEW" | "JOB_PREPARATION" | "RESUME_ANALYZER" | "JOB_MATCHER"
        11. Generate a concrete 7-day `weeklyGoals` plan (Day 1 through Day 7) directly targeting their top weaknesses.

        RESPONSE REQUIREMENTS:
        - Return ONLY valid JSON matching this schema:
        {
          "overallReadinessScore": 75,
          "readinessLevel": "ALMOST READY",
          "careerSummary": "Personalized markdown summary...",
          "strengths": ["Java", "Spring Boot", "REST APIs"],
          "weaknesses": [
            {
              "skill": "System Design",
              "currentStatus": "Missing practical distributed systems knowledge",
              "priority": "HIGH",
              "recommendedAction": "Study caching and message queues"
            }
          ],
          "skillProgress": [
            {
              "skill": "Java",
              "category": "TECHNICAL",
              "status": "STRONG",
              "proficiencyPercentage": 85
            }
          ],
          "interviewProgress": {
            "averageScore": 72.5,
            "technicalScore": 75.0,
            "behavioralScore": 70.0,
            "communicationScore": 72.0,
            "totalInterviews": 3,
            "trend": "IMPROVING"
          },
          "jobSearchProgress": {
            "jobsMatched": 12,
            "savedJobs": 3,
            "appliedJobs": 1,
            "preparationSessions": 2,
            "avgMatchScore": 78.0
          },
          "roadmapProgress": {
            "completedMilestones": 2,
            "totalMilestones": 6,
            "currentMilestone": "Spring Security & Microservices",
            "upcomingMilestone": "Docker & Kubernetes Deployment",
            "completionPercentage": 33
          },
          "recommendations": [
            {
              "priority": "HIGH",
              "action": "Complete Spring Security Module",
              "reason": "Top requirement in 80% of your matched jobs",
              "relatedFeature": "CAREER_ROADMAP"
            }
          ],
          "weeklyGoals": [
            {
              "day": "Day 1",
              "title": "Spring Security JWT Practice",
              "description": "Implement authentication filter and review token expiry handling",
              "focusArea": "Backend Security"
            }
          ]
        }
        """;

    public CareerAnalyticsDto analyzeCareerAnalytics(String candidateJson,
                                                     String resumeAnalysisJson,
                                                     String skillGapJson,
                                                     String jobMatchJson,
                                                     String mockInterviewJson,
                                                     String roadmapJson,
                                                     String jobPreparationJson) {
        log.info("Requesting Gemini AI Career Analytics Intelligence evaluation");

        StringBuilder sb = new StringBuilder(CAREER_ANALYTICS_SYSTEM_PROMPT);
        replacePlaceholder(sb, "{{CANDIDATE_DATA}}", candidateJson != null ? candidateJson : "{}");
        replacePlaceholder(sb, "{{RESUME_ANALYSIS_DATA}}", resumeAnalysisJson != null ? resumeAnalysisJson : "{}");
        replacePlaceholder(sb, "{{SKILL_GAP_DATA}}", skillGapJson != null ? skillGapJson : "{}");
        replacePlaceholder(sb, "{{JOB_MATCH_DATA}}", jobMatchJson != null ? jobMatchJson : "{}");
        replacePlaceholder(sb, "{{MOCK_INTERVIEW_DATA}}", mockInterviewJson != null ? mockInterviewJson : "{}");
        replacePlaceholder(sb, "{{ROADMAP_DATA}}", roadmapJson != null ? roadmapJson : "{}");
        replacePlaceholder(sb, "{{JOB_PREPARATION_DATA}}", jobPreparationJson != null ? jobPreparationJson : "{}");

        String jsonText = callGeminiApi(sb.toString());
        return parseCareerAnalyticsResponse(jsonText);
    }

    private void replacePlaceholder(StringBuilder sb, String placeholder, String value) {
        int index;
        while ((index = sb.indexOf(placeholder)) != -1) {
            sb.replace(index, index + placeholder.length(), value != null ? value : "");
        }
    }

    private CareerAnalyticsDto parseCareerAnalyticsResponse(String jsonText) {
        try {
            CareerAnalyticsDto dto = objectMapper.readValue(jsonText, CareerAnalyticsDto.class);

            // Clamp and sanitize
            int score = clampScore(dto.getOverallReadinessScore(), 50);
            dto.setOverallReadinessScore(score);

            if (dto.getReadinessLevel() == null || dto.getReadinessLevel().isBlank()) {
                dto.setReadinessLevel(determineReadinessLevel(score));
            }

            if (dto.getCareerSummary() == null || dto.getCareerSummary().isBlank()) {
                dto.setCareerSummary("Based on your latest profile and activities, your career progress is developing steadily. Continue practicing mock interviews and completing career roadmap milestones to boost your market readiness.");
            }

            if (dto.getStrengths() == null) {
                dto.setStrengths(Collections.emptyList());
            }
            if (dto.getWeaknesses() == null) {
                dto.setWeaknesses(Collections.emptyList());
            }
            if (dto.getSkillProgress() == null) {
                dto.setSkillProgress(Collections.emptyList());
            }
            if (dto.getInterviewProgress() == null) {
                dto.setInterviewProgress(new CareerAnalyticsDto.InterviewProgressSummary());
            }
            if (dto.getJobSearchProgress() == null) {
                dto.setJobSearchProgress(new CareerAnalyticsDto.JobSearchProgressSummary());
            }
            if (dto.getRoadmapProgress() == null) {
                dto.setRoadmapProgress(new CareerAnalyticsDto.RoadmapProgressSummary());
            }
            if (dto.getRecommendations() == null) {
                dto.setRecommendations(Collections.emptyList());
            }
            if (dto.getWeeklyGoals() == null) {
                dto.setWeeklyGoals(Collections.emptyList());
            }

            return dto;
        } catch (Exception e) {
            log.error("Failed to parse Gemini AI Career Analytics JSON: {}. Raw output: {}", e.getMessage(), jsonText);
            throw new AIServiceException("AI Career Analytics produced a malformed response. Please try again.");
        }
    }

    private String determineReadinessLevel(int score) {
        if (score >= 80) return "READY";
        if (score >= 60) return "ALMOST READY";
        if (score >= 40) return "NEEDS IMPROVEMENT";
        return "NEEDS PREPARATION";
    }

    // =========================================================================
    // PHASE 11: AI APPLICATION INTELLIGENCE & TRACKING ADVISOR
    // =========================================================================

    private static final String APPLICATION_INTELLIGENCE_SYSTEM_PROMPT = """
        You are an elite Tech Recruiter, Hiring Committee Member, and Placement Campaign Strategist.
        Your task is to analyze the candidate's complete background, the targeted job, current application status, job preparation notes, and career analytics to provide tactical, high-impact Application Intelligence and an interview conversion strategy.

        CANDIDATE PROFILE & RESUME:
        {{CANDIDATE_DATA}}

        TARGET JOB DETAILS:
        {{JOB_DATA}}

        APPLICATION TRACKING STATUS & TIMELINE:
        {{APPLICATION_DATA}}

        JOB PREPARATION PLAN:
        {{JOB_PREPARATION_DATA}}

        CAREER READINESS & ANALYTICS:
        {{CAREER_ANALYTICS_DATA}}

        CRITICAL INSTRUCTIONS:
        1. Tailor your strategy specifically to the current application status:
           - SAVED: Focus on resume alignment, bridging critical missing skills, and preparing before applying.
           - APPLIED: Focus on recruiter follow-up timing, initial outreach templates, and early screening prep.
           - SCREENING: Focus on company background, HR pitch, salary expectations, and cultural fit.
           - INTERVIEW: Focus on high-probability technical deep-dives, system design questions, and mock interview practice.
           - OFFER: Focus on offer evaluation, compensation benchmarks, and career alignment.
           - REJECTED / WITHDRAWN: Focus on post-mortem lessons and targeted skill adjustments.
        2. Calculate a realistic "readinessScore" (integer 0 to 100) reflecting candidate suitability for this specific job.
        3. Determine "priority" level: "HIGH", "MEDIUM", or "LOW" based on match score and opportunity quality.
        4. Return strictly valid JSON adhering to the schema below. No Markdown wrappers.

        OUTPUT SCHEMA (STRICT JSON ONLY):
        {
          "readinessScore": 82,
          "summary": "Strong candidate match for this role with solid Java and REST fundamentals. High conversion probability if distributed systems and concurrency topics are reviewed prior to technical rounds.",
          "strengths": [
            "Matches 85% of primary required backend skills (Spring Boot, MySQL, REST)",
            "Strong ATS score on recent resume upload"
          ],
          "risks": [
            "Missing hands-on experience in Docker/Kubernetes mentioned in job listing",
            "High competition role requiring structured recruiter follow-up"
          ],
          "recommendedActions": [
            "Customize resume bullet points to emphasize relational database indexing",
            "Complete 1 mock interview session on Backend Architecture before phone screen",
            "Send a polite LinkedIn message to the hiring manager 3 days after applying"
          ],
          "followUpAdvice": [
            "Follow up exactly 5-7 business days after applying if no status update is received",
            "Keep outreach messages under 100 words highlighting 2 relevant project achievements"
          ],
          "interviewAdvice": [
            "Prepare STAR-format answers for collaborative problem solving",
            "Expect deep questions on JPA entity lifecycle and database transaction isolation levels"
          ],
          "resumeAdvice": [
            "Ensure 'Spring Boot', 'REST API', and 'MySQL' appear in the top 1/3 of the resume",
            "Quantify project achievements with metrics where possible"
          ],
          "priority": "HIGH"
        }
        """;

    public ApplicationIntelligenceDto analyzeApplicationStrategy(String candidateJson,
                                                                 String applicationJson,
                                                                 String jobJson,
                                                                 String jobPreparationJson,
                                                                 String careerAnalyticsJson) {
        log.info("Requesting Gemini AI Application Strategy & Intelligence evaluation");

        StringBuilder sb = new StringBuilder(APPLICATION_INTELLIGENCE_SYSTEM_PROMPT);
        replacePlaceholder(sb, "{{CANDIDATE_DATA}}", candidateJson != null ? candidateJson : "{}");
        replacePlaceholder(sb, "{{APPLICATION_DATA}}", applicationJson != null ? applicationJson : "{}");
        replacePlaceholder(sb, "{{JOB_DATA}}", jobJson != null ? jobJson : "{}");
        replacePlaceholder(sb, "{{JOB_PREPARATION_DATA}}", jobPreparationJson != null ? jobPreparationJson : "{}");
        replacePlaceholder(sb, "{{CAREER_ANALYTICS_DATA}}", careerAnalyticsJson != null ? careerAnalyticsJson : "{}");

        String jsonText = callGeminiApi(sb.toString());
        return parseApplicationIntelligenceResponse(jsonText);
    }

    private ApplicationIntelligenceDto parseApplicationIntelligenceResponse(String jsonText) {
        try {
            ApplicationIntelligenceDto dto = objectMapper.readValue(jsonText, ApplicationIntelligenceDto.class);

            int score = clampScore(dto.getReadinessScore(), 60);
            dto.setReadinessScore(score);

            if (dto.getPriority() == null || dto.getPriority().isBlank()) {
                dto.setPriority(score >= 75 ? "HIGH" : score >= 50 ? "MEDIUM" : "LOW");
            }

            if (dto.getSummary() == null || dto.getSummary().isBlank()) {
                dto.setSummary("Application strategy evaluated with a match readiness score of " + score + "%. Follow the tailored action items to maximize interview conversion.");
            }

            if (dto.getStrengths() == null) dto.setStrengths(Collections.emptyList());
            if (dto.getRisks() == null) dto.setRisks(Collections.emptyList());
            if (dto.getRecommendedActions() == null) dto.setRecommendedActions(Collections.emptyList());
            if (dto.getFollowUpAdvice() == null) dto.setFollowUpAdvice(Collections.emptyList());
            if (dto.getInterviewAdvice() == null) dto.setInterviewAdvice(Collections.emptyList());
            if (dto.getResumeAdvice() == null) dto.setResumeAdvice(Collections.emptyList());

            return dto;
        } catch (Exception e) {
            log.error("Failed to parse Gemini AI Application Intelligence JSON: {}. Raw output: {}", e.getMessage(), jsonText);
            throw new AIServiceException("AI Application Intelligence produced a malformed response. Please try again.");
        }
    }

    private int clampScore(Integer val, int defaultScore) {
        if (val == null) return defaultScore;
        return Math.max(0, Math.min(100, val));
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("Failed to serialize object to JSON: {}", e.getMessage());
            return "[]";
        }
    }
}
