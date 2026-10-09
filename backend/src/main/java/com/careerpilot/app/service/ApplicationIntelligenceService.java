package com.careerpilot.app.service;

import com.careerpilot.app.dto.ApplicationIntelligenceDto;
import com.careerpilot.app.entity.Job;
import com.careerpilot.app.entity.JobApplication;
import com.careerpilot.app.entity.User;
import com.careerpilot.app.exception.ResourceNotFoundException;
import com.careerpilot.app.exception.UnauthorizedChatAccessException;
import com.careerpilot.app.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationIntelligenceService {

    private final UserRepository userRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final ProfileRepository profileRepository;
    private final ResumeAnalysisRepository resumeAnalysisRepository;
    private final JobMatchRepository jobMatchRepository;
    private final SkillGapAnalysisRepository skillGapAnalysisRepository;
    private final JobPreparationRepository jobPreparationRepository;
    private final InterviewSessionRepository interviewSessionRepository;
    private final CareerAnalyticsRepository careerAnalyticsRepository;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;

    @Transactional
    public ApplicationIntelligenceDto analyzeApplication(Long applicationId, String userEmail) {
        JobApplication application = getAndVerifyApplication(applicationId, userEmail);

        if (application.getAiAnalysisJson() != null && !application.getAiAnalysisJson().isBlank()) {
            ApplicationIntelligenceDto cached = parseIntelligence(application.getAiAnalysisJson());
            if (cached != null) {
                cached.setIsCached(true);
                cached.setApplicationStatus(application.getStatus().name());
                return cached;
            }
        }

        return generateAndPersistAnalysis(application);
    }

    @Transactional(readOnly = true)
    public ApplicationIntelligenceDto getCachedAnalysis(Long applicationId, String userEmail) {
        JobApplication application = getAndVerifyApplication(applicationId, userEmail);

        if (application.getAiAnalysisJson() != null && !application.getAiAnalysisJson().isBlank()) {
            ApplicationIntelligenceDto cached = parseIntelligence(application.getAiAnalysisJson());
            if (cached != null) {
                cached.setIsCached(true);
                cached.setApplicationStatus(application.getStatus().name());
                return cached;
            }
        }

        return null;
    }

    @Transactional
    public ApplicationIntelligenceDto regenerateAnalysis(Long applicationId, String userEmail) {
        JobApplication application = getAndVerifyApplication(applicationId, userEmail);
        return generateAndPersistAnalysis(application);
    }

    private ApplicationIntelligenceDto generateAndPersistAnalysis(JobApplication application) {
        User user = application.getUser();
        Job job = application.getJob();

        // 1. Candidate profile & resume JSON
        Map<String, Object> candidateMap = new LinkedHashMap<>();
        profileRepository.findByUser(user).ifPresent(p -> {
            candidateMap.put("targetRole", p.getTargetRole());
            candidateMap.put("experienceLevel", p.getExperienceLevel());
            candidateMap.put("skillsSummary", p.getSkillsSummary());
            candidateMap.put("degree", p.getDegree());
            candidateMap.put("branch", p.getBranch());
        });
        resumeAnalysisRepository.findByResumeUserOrderByIdDesc(user).stream().findFirst().ifPresent(r -> {
            candidateMap.put("atsScore", r.getAtsScore());
            candidateMap.put("overallScore", r.getOverallScore());
            candidateMap.put("detectedSkills", fromJson(r.getDetectedSkillsJson(), new TypeReference<List<String>>() {}, Collections.emptyList()));
            candidateMap.put("missingSkills", fromJson(r.getMissingSkillsJson(), new TypeReference<List<String>>() {}, Collections.emptyList()));
            candidateMap.put("strengths", fromJson(r.getStrengthsJson(), new TypeReference<List<String>>() {}, Collections.emptyList()));
        });
        String candidateJson = toJson(candidateMap);

        // 2. Application status & timeline JSON
        Map<String, Object> appMap = new LinkedHashMap<>();
        appMap.put("id", application.getId());
        appMap.put("status", application.getStatus().name());
        appMap.put("company", application.getCompany());
        appMap.put("jobTitle", application.getJobTitle());
        appMap.put("applicationDate", application.getApplicationDate() != null ? application.getApplicationDate().toString() : null);
        appMap.put("deadline", application.getDeadline() != null ? application.getDeadline().toString() : null);
        appMap.put("nextFollowUpDate", application.getNextFollowUpDate() != null ? application.getNextFollowUpDate().toString() : null);
        appMap.put("interviewDate", application.getInterviewDate() != null ? application.getInterviewDate().toString() : null);
        appMap.put("source", application.getSource());
        appMap.put("notes", application.getNotes());
        String applicationJson = toJson(appMap);

        // 3. Target job JSON
        Map<String, Object> jobMap = new LinkedHashMap<>();
        if (job != null) {
            jobMap.put("id", job.getId());
            jobMap.put("title", job.getTitle());
            jobMap.put("company", job.getCompany());
            jobMap.put("location", job.getLocation());
            jobMap.put("jobType", job.getJobType());
            jobMap.put("experienceLevel", job.getExperienceLevel());
            jobMap.put("requiredSkills", job.getRequiredSkills());
            jobMap.put("description", job.getDescription());
        } else {
            jobMap.put("title", application.getJobTitle());
            jobMap.put("company", application.getCompany());
            jobMap.put("location", application.getLocation());
        }
        if (job != null) {
            jobMatchRepository.findByUserAndJob(user, job).ifPresent(jm -> {
                jobMap.put("matchScore", jm.getOverallScore());
                jobMap.put("matchRecommendation", jm.getRecommendation());
                jobMap.put("matchingSkills", fromJson(jm.getMatchingSkillsJson(), new TypeReference<List<String>>() {}, Collections.emptyList()));
                jobMap.put("missingSkills", fromJson(jm.getMissingSkillsJson(), new TypeReference<List<String>>() {}, Collections.emptyList()));
            });
        }
        String jobJson = toJson(jobMap);

        // 4. Job preparation plan JSON
        Map<String, Object> prepMap = new LinkedHashMap<>();
        if (job != null) {
            jobPreparationRepository.findByUserAndJob(user, job).ifPresent(jp -> {
                prepMap.put("readinessScore", jp.getReadinessScore());
                prepMap.put("preparationLevel", jp.getPreparationLevel());
                prepMap.put("skillGaps", fromJson(jp.getSkillGapsJson(), new TypeReference<List<String>>() {}, Collections.emptyList()));
                prepMap.put("technicalTopics", fromJson(jp.getTechnicalTopicsJson(), new TypeReference<List<String>>() {}, Collections.emptyList()));
            });
        }
        String jobPrepJson = toJson(prepMap);

        // 5. Career analytics JSON
        Map<String, Object> analyticsMap = new LinkedHashMap<>();
        careerAnalyticsRepository.findByUser(user).ifPresent(ca -> {
            analyticsMap.put("overallReadinessScore", ca.getOverallReadinessScore());
            analyticsMap.put("readinessLevel", ca.getReadinessLevel());
            analyticsMap.put("strengths", fromJson(ca.getStrengthsJson(), new TypeReference<List<String>>() {}, Collections.emptyList()));
            analyticsMap.put("weaknesses", fromJson(ca.getWeaknessesJson(), new TypeReference<List<Object>>() {}, Collections.emptyList()));
        });
        String careerAnalyticsJson = toJson(analyticsMap);

        log.info("Generating AI Application Strategy for Application ID: {} (Job: {} at {})",
                application.getId(), application.getJobTitle(), application.getCompany());

        ApplicationIntelligenceDto result = geminiService.analyzeApplicationStrategy(
                candidateJson,
                applicationJson,
                jobJson,
                jobPrepJson,
                careerAnalyticsJson
        );

        result.setIsCached(false);
        result.setApplicationStatus(application.getStatus().name());

        // Cache result in entity
        application.setAiAnalysisJson(toJson(result));
        jobApplicationRepository.save(application);

        return result;
    }

    private JobApplication getAndVerifyApplication(Long applicationId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Job application not found with ID: " + applicationId));

        if (!application.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedChatAccessException("You do not have permission to access this application intelligence.");
        }

        return application;
    }

    private ApplicationIntelligenceDto parseIntelligence(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, ApplicationIntelligenceDto.class);
        } catch (Exception e) {
            log.error("Failed to parse cached application intelligence: {}", e.getMessage());
            return null;
        }
    }

    private String toJson(Object obj) {
        if (obj == null) return "{}";
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("JSON error in ApplicationIntelligenceService: {}", e.getMessage());
            return "{}";
        }
    }

    private <T> T fromJson(String json, TypeReference<T> typeRef, T defaultValue) {
        if (json == null || json.isBlank() || "null".equalsIgnoreCase(json.trim())) return defaultValue;
        try {
            T val = objectMapper.readValue(json, typeRef);
            return val != null ? val : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
