package com.careerpilot.app.service;

import com.careerpilot.app.dto.JobPreparationDto;
import com.careerpilot.app.dto.response.DetectedSkillDto;
import com.careerpilot.app.dto.response.JobPreparationResponse;
import com.careerpilot.app.dto.response.MissingSkillDto;
import com.careerpilot.app.entity.*;
import com.careerpilot.app.exception.ResourceNotFoundException;
import com.careerpilot.app.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobPreparationService {

    private final JobPreparationRepository jobPreparationRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final ResumeRepository resumeRepository;
    private final ResumeAnalysisRepository resumeAnalysisRepository;
    private final JobMatchRepository jobMatchRepository;
    private final CareerRoadmapRepository careerRoadmapRepository;
    private final InterviewSessionRepository interviewSessionRepository;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;

    /**
     * Generates a job preparation plan. If a cached plan exists for this user and job, returns it immediately.
     */
    @Transactional
    public JobPreparationResponse generatePreparation(Long jobId, String userEmail) {
        User user = findUserByEmail(userEmail);
        Optional<JobPreparation> existing = jobPreparationRepository.findByUserAndJobId(user, jobId);
        if (existing.isPresent()) {
            log.info("Found cached JobPreparation ID {} for job ID {} and user {}. Returning cached result.",
                    existing.get().getId(), jobId, userEmail);
            return toResponse(existing.get());
        }
        return regeneratePreparation(jobId, userEmail);
    }

    /**
     * Forces regeneration of the AI Job Preparation plan via Gemini AI.
     */
    @Transactional
    public JobPreparationResponse regeneratePreparation(Long jobId, String userEmail) {
        long startTime = System.currentTimeMillis();
        User user = findUserByEmail(userEmail);
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", jobId));

        log.info("[DIAGNOSTIC] Job Preparation generation started for job '{}' (ID: {}) and user {}",
                job.getTitle(), jobId, userEmail);

        // 1. Build compact candidate context
        String candidateJson = buildCandidateContext(user);

        // 2. Build Job Match context if available
        String jobMatchJson = buildJobMatchContext(user, job);

        // 3. Build Career Roadmap context if available
        String careerRoadmapJson = buildCareerRoadmapContext(user);

        // 4. Call Gemini AI
        log.info("Requesting AI Job Preparation analysis from Gemini for job ID {} and user {}", jobId, userEmail);
        JobPreparationDto dto = geminiService.analyzeJobPreparation(candidateJson, job, jobMatchJson, careerRoadmapJson);

        // 5. Save or update JobPreparation entity
        JobPreparation prep = jobPreparationRepository.findByUserAndJob(user, job)
                .orElseGet(() -> JobPreparation.builder().user(user).job(job).build());

        prep.setUser(user);
        prep.setJob(job);
        prep.setReadinessScore(dto.getReadinessScore() != null ? dto.getReadinessScore() : 70);
        prep.setPreparationLevel(dto.getPreparationLevel() != null ? dto.getPreparationLevel() : "INTERMEDIATE");
        prep.setSummary(dto.getSummary());
        prep.setStrengthsJson(toJson(dto.getStrengths()));
        prep.setSkillGapsJson(toJson(dto.getSkillGaps()));
        prep.setTechnicalTopicsJson(toJson(dto.getTechnicalTopics()));
        prep.setInterviewTopicsJson(toJson(dto.getInterviewTopics()));
        prep.setCodingTopicsJson(toJson(dto.getCodingTopics()));
        prep.setProjectTopicsJson(toJson(dto.getProjectTopics()));
        prep.setBehavioralTopicsJson(toJson(dto.getBehavioralTopics()));
        prep.setPreparationPlanJson(toJson(dto.getPreparationPlan()));
        prep.setRecommendedQuestionsJson(toJson(dto.getRecommendedQuestions()));

        prep = jobPreparationRepository.save(prep);

        long duration = System.currentTimeMillis() - startTime;
        log.info("[DIAGNOSTIC] Generated JobPreparation ID {} for job ID {} in {} ms for user {}",
                prep.getId(), jobId, duration, userEmail);

        return toResponse(prep);
    }

    /**
     * Gets preparation plan for a specific job and user.
     */
    @Transactional(readOnly = true)
    public JobPreparationResponse getPreparation(Long jobId, String userEmail) {
        User user = findUserByEmail(userEmail);
        JobPreparation prep = jobPreparationRepository.findByUserAndJobId(user, jobId)
                .orElseThrow(() -> new ResourceNotFoundException("JobPreparation", "jobId", jobId));
        return toResponse(prep);
    }

    /**
     * Gets all preparation plans for the authenticated user.
     */
    @Transactional(readOnly = true)
    public List<JobPreparationResponse> getAllPreparations(String userEmail) {
        User user = findUserByEmail(userEmail);
        List<JobPreparation> list = jobPreparationRepository.findByUserOrderByUpdatedAtDesc(user);
        return list.stream().map(this::toResponse).toList();
    }

    /**
     * Deletes a preparation plan.
     */
    @Transactional
    public void deletePreparation(Long jobId, String userEmail) {
        User user = findUserByEmail(userEmail);
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", jobId));
        jobPreparationRepository.deleteByUserAndJob(user, job);
        log.info("User {} deleted JobPreparation for job ID {}", userEmail, jobId);
    }

    private String buildCandidateContext(User user) {
        Profile profile = profileRepository.findByUser(user).orElse(null);
        Resume resume = resumeRepository.findFirstByUserOrderByUploadedAtDesc(user).orElse(null);
        ResumeAnalysis resumeAnalysis = (resume != null) ? resumeAnalysisRepository.findByResume(resume).orElse(null) : null;

        Map<String, Object> map = new HashMap<>();

        if (profile != null) {
            map.put("targetRole", profile.getTargetRole());
            map.put("experienceLevel", profile.getExperienceLevel());
            Map<String, String> edu = new HashMap<>();
            if (profile.getDegree() != null) edu.put("degree", profile.getDegree());
            if (profile.getBranch() != null) edu.put("branch", profile.getBranch());
            if (profile.getCollege() != null) edu.put("college", profile.getCollege());
            map.put("education", edu);
        }

        List<String> detectedSkills = new ArrayList<>();
        List<String> missingResumeSkills = new ArrayList<>();

        if (resumeAnalysis != null) {
            List<DetectedSkillDto> detected = fromJson(resumeAnalysis.getDetectedSkillsJson(),
                    new TypeReference<List<DetectedSkillDto>>() {}, Collections.emptyList());
            detectedSkills.addAll(detected.stream().map(DetectedSkillDto::getName).filter(Objects::nonNull).toList());

            List<MissingSkillDto> missing = fromJson(resumeAnalysis.getMissingSkillsJson(),
                    new TypeReference<List<MissingSkillDto>>() {}, Collections.emptyList());
            missingResumeSkills.addAll(missing.stream().map(MissingSkillDto::getName).filter(Objects::nonNull).toList());

            if (resumeAnalysis.getSummary() != null) {
                map.put("resumeSummary", resumeAnalysis.getSummary());
            }
        }

        if (profile != null && profile.getSkillsSummary() != null && !profile.getSkillsSummary().trim().isEmpty()) {
            String[] skills = profile.getSkillsSummary().split("[,;\\n]+");
            for (String s : skills) {
                String clean = s.trim();
                if (!clean.isEmpty() && !detectedSkills.contains(clean)) {
                    detectedSkills.add(clean);
                }
            }
        }

        if (detectedSkills.isEmpty()) {
            detectedSkills.add("Software Engineering Core");
        }

        map.put("detectedSkills", detectedSkills);
        map.put("missingResumeSkills", missingResumeSkills);

        // Latest mock interview weaknesses
        List<InterviewSession> sessions = interviewSessionRepository.findByUserOrderByCreatedAtDesc(user);
        if (!sessions.isEmpty()) {
            InterviewSession latestSession = sessions.get(0);
            if (latestSession.getOverallScore() != null) {
                map.put("recentMockInterviewScore", latestSession.getOverallScore());
                List<String> weaknesses = fromJson(latestSession.getWeaknessesJson(), new TypeReference<List<String>>() {}, Collections.emptyList());
                map.put("mockInterviewWeaknesses", weaknesses);
            }
        }

        return toJson(map);
    }

    private String buildJobMatchContext(User user, Job job) {
        JobMatch match = jobMatchRepository.findByUserAndJob(user, job).orElse(null);
        if (match == null) return "{}";

        Map<String, Object> map = new HashMap<>();
        map.put("overallScore", match.getOverallScore());
        map.put("recommendation", match.getRecommendation());
        map.put("matchingSkills", fromJson(match.getMatchingSkillsJson(), new TypeReference<List<String>>() {}, Collections.emptyList()));
        map.put("missingSkills", fromJson(match.getMissingSkillsJson(), new TypeReference<List<String>>() {}, Collections.emptyList()));
        map.put("summary", match.getAiExplanation());
        return toJson(map);
    }

    private String buildCareerRoadmapContext(User user) {
        CareerRoadmap roadmap = careerRoadmapRepository.findTopByUserOrderByUpdatedAtDesc(user).orElse(null);
        if (roadmap == null) return "{}";

        Map<String, Object> map = new HashMap<>();
        map.put("targetRole", roadmap.getTargetRole());
        map.put("overallReadinessScore", roadmap.getOverallReadinessScore());
        map.put("recommendedSkills", fromJson(roadmap.getRecommendedSkillsJson(), new TypeReference<List<String>>() {}, Collections.emptyList()));
        map.put("projectRecommendations", fromJson(roadmap.getProjectRecommendationsJson(), new TypeReference<List<String>>() {}, Collections.emptyList()));
        return toJson(map);
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("Failed to serialize object to JSON: {}", e.getMessage());
            return "[]";
        }
    }

    private <T> T fromJson(String json, TypeReference<T> typeRef, T defaultValue) {
        if (json == null || json.trim().isEmpty()) return defaultValue;
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            log.warn("Failed to deserialize JSON: {}", e.getMessage());
            return defaultValue;
        }
    }

    public JobPreparationResponse toResponse(JobPreparation prep) {
        Job job = prep.getJob();
        JobMatch match = (job != null) ? jobMatchRepository.findByUserAndJob(prep.getUser(), job).orElse(null) : null;

        List<String> strengths = fromJson(prep.getStrengthsJson(), new TypeReference<List<String>>() {}, Collections.emptyList());
        List<String> skillGaps = fromJson(prep.getSkillGapsJson(), new TypeReference<List<String>>() {}, Collections.emptyList());
        List<String> technicalTopics = fromJson(prep.getTechnicalTopicsJson(), new TypeReference<List<String>>() {}, Collections.emptyList());
        List<String> interviewTopics = fromJson(prep.getInterviewTopicsJson(), new TypeReference<List<String>>() {}, Collections.emptyList());
        List<String> codingTopics = fromJson(prep.getCodingTopicsJson(), new TypeReference<List<String>>() {}, Collections.emptyList());
        List<String> projectTopics = fromJson(prep.getProjectTopicsJson(), new TypeReference<List<String>>() {}, Collections.emptyList());
        List<String> behavioralTopics = fromJson(prep.getBehavioralTopicsJson(), new TypeReference<List<String>>() {}, Collections.emptyList());
        List<JobPreparationDto.PreparationDayDto> preparationPlan = fromJson(prep.getPreparationPlanJson(),
                new TypeReference<List<JobPreparationDto.PreparationDayDto>>() {}, Collections.emptyList());
        List<JobPreparationDto.RecommendedQuestionDto> recommendedQuestions = fromJson(prep.getRecommendedQuestionsJson(),
                new TypeReference<List<JobPreparationDto.RecommendedQuestionDto>>() {}, Collections.emptyList());

        return JobPreparationResponse.builder()
                .id(prep.getId())
                .jobId(job != null ? job.getId() : null)
                .jobTitle(job != null ? job.getTitle() : "Software Developer")
                .company(job != null ? job.getCompany() : "")
                .location(job != null ? job.getLocation() : "")
                .experienceLevel(job != null ? job.getExperienceLevel() : "")
                .jobMatchScore(match != null ? match.getOverallScore() : null)
                .readinessScore(prep.getReadinessScore() != null ? prep.getReadinessScore() : 0)
                .preparationLevel(prep.getPreparationLevel() != null ? prep.getPreparationLevel() : "INTERMEDIATE")
                .summary(prep.getSummary())
                .strengths(strengths)
                .skillGaps(skillGaps)
                .technicalTopics(technicalTopics)
                .interviewTopics(interviewTopics)
                .codingTopics(codingTopics)
                .projectTopics(projectTopics)
                .behavioralTopics(behavioralTopics)
                .preparationPlan(preparationPlan)
                .recommendedQuestions(recommendedQuestions)
                .createdAt(prep.getCreatedAt())
                .updatedAt(prep.getUpdatedAt())
                .build();
    }
}
