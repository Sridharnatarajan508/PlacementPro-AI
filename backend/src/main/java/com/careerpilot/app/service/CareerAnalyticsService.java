package com.careerpilot.app.service;

import com.careerpilot.app.dto.CareerAnalyticsDto;
import com.careerpilot.app.dto.response.CareerAnalyticsResponse;
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
public class CareerAnalyticsService {

    private final CareerAnalyticsRepository careerAnalyticsRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final ResumeRepository resumeRepository;
    private final ResumeAnalysisRepository resumeAnalysisRepository;
    private final SkillGapAnalysisRepository skillGapAnalysisRepository;
    private final JobMatchRepository jobMatchRepository;
    private final SavedJobRepository savedJobRepository;
    private final InterviewSessionRepository interviewSessionRepository;
    private final CareerRoadmapRepository careerRoadmapRepository;
    private final JobPreparationRepository jobPreparationRepository;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;

    /**
     * Retrieves stored career analytics for the user.
     * Does NOT call Gemini.
     */
    @Transactional(readOnly = true)
    public CareerAnalyticsResponse getAnalytics(String userEmail) {
        User user = findUserByEmail(userEmail);
        CareerAnalytics analytics = careerAnalyticsRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("CareerAnalytics", "user", userEmail));
        return toResponse(analytics);
    }

    /**
     * Returns cached analytics if available; otherwise generates fresh analytics.
     */
    @Transactional
    public CareerAnalyticsResponse generateAnalytics(String userEmail) {
        User user = findUserByEmail(userEmail);
        Optional<CareerAnalytics> existing = careerAnalyticsRepository.findByUser(user);
        if (existing.isPresent()) {
            log.info("Found cached CareerAnalytics ID {} for user {}. Returning cached result without calling Gemini.",
                    existing.get().getId(), userEmail);
            return toResponse(existing.get());
        }
        return regenerateAnalytics(userEmail);
    }

    /**
     * Forces regeneration of AI Career Analytics Intelligence via Gemini AI.
     */
    @Transactional
    public CareerAnalyticsResponse regenerateAnalytics(String userEmail) {
        long startTime = System.currentTimeMillis();
        User user = findUserByEmail(userEmail);

        log.info("[DIAGNOSTIC] Career Analytics generation started for user {}", userEmail);

        // 1. Collect candidate information
        String candidateJson = buildCandidateContext(user);

        // 2. Collect resume analysis information
        String resumeAnalysisJson = buildResumeAnalysisContext(user);

        // 3. Collect skill-gap information
        String skillGapJson = buildSkillGapContext(user);

        // 4. Collect job matching and saved jobs information
        String jobMatchJson = buildJobMatchContext(user);

        // 5. Collect mock interview performance
        String mockInterviewJson = buildMockInterviewContext(user);

        // 6. Collect career roadmap progress
        String roadmapJson = buildRoadmapContext(user);

        // 7. Collect job preparation information
        String jobPreparationJson = buildJobPreparationContext(user);

        // 8. Call Gemini AI
        log.info("Requesting AI Career Analytics evaluation from Gemini for user {}", userEmail);
        CareerAnalyticsDto dto = geminiService.analyzeCareerAnalytics(
                candidateJson,
                resumeAnalysisJson,
                skillGapJson,
                jobMatchJson,
                mockInterviewJson,
                roadmapJson,
                jobPreparationJson
        );

        // 9. Save or update CareerAnalytics entity
        CareerAnalytics analytics = careerAnalyticsRepository.findByUser(user)
                .orElseGet(() -> CareerAnalytics.builder().user(user).build());

        Integer previousScore = analytics.getOverallReadinessScore();
        if (previousScore != null && previousScore > 0) {
            analytics.setPreviousReadinessScore(previousScore);
        }

        int score = clampScore(dto.getOverallReadinessScore(), 50);
        analytics.setOverallReadinessScore(score);
        analytics.setReadinessLevel(dto.getReadinessLevel() != null ? dto.getReadinessLevel() : determineReadinessLevel(score));
        analytics.setCareerSummary(dto.getCareerSummary());
        analytics.setStrengthsJson(toJson(dto.getStrengths()));
        analytics.setWeaknessesJson(toJson(dto.getWeaknesses()));
        analytics.setSkillProgressJson(toJson(dto.getSkillProgress()));
        analytics.setInterviewProgressJson(toJson(dto.getInterviewProgress()));
        analytics.setJobSearchProgressJson(toJson(dto.getJobSearchProgress()));
        analytics.setRoadmapProgressJson(toJson(dto.getRoadmapProgress()));
        analytics.setRecommendationsJson(toJson(dto.getRecommendations()));
        analytics.setWeeklyGoalsJson(toJson(dto.getWeeklyGoals()));

        CareerAnalytics saved = careerAnalyticsRepository.save(analytics);
        long elapsed = System.currentTimeMillis() - startTime;
        log.info("[DIAGNOSTIC] Generated CareerAnalytics ID {} (Score: {}%) in {} ms for user {}",
                saved.getId(), saved.getOverallReadinessScore(), elapsed, userEmail);

        return toResponse(saved);
    }

    // ==========================================
    // CONTEXT BUILDERS
    // ==========================================

    private String buildCandidateContext(User user) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("userId", user.getId());
        ctx.put("name", user.getName());
        ctx.put("email", user.getEmail());

        profileRepository.findByUser(user).ifPresent(profile -> {
            ctx.put("targetRole", profile.getTargetRole());
            ctx.put("experienceLevel", profile.getExperienceLevel());
            ctx.put("preferredJobType", profile.getPreferredJobType());
            ctx.put("college", profile.getCollege());
            ctx.put("degree", profile.getDegree());
            ctx.put("branch", profile.getBranch());
            ctx.put("graduationYear", profile.getGraduationYear());
            ctx.put("skillsSummary", profile.getSkillsSummary());
        });
        return toJson(ctx);
    }

    private String buildResumeAnalysisContext(User user) {
        List<ResumeAnalysis> analyses = resumeAnalysisRepository.findByResumeUserOrderByIdDesc(user);
        if (analyses.isEmpty()) {
            return "{}";
        }
        ResumeAnalysis ra = analyses.get(0);
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("overallScore", ra.getOverallScore());
        ctx.put("atsScore", ra.getAtsScore());
        ctx.put("strengths", fromJson(ra.getStrengthsJson(), new TypeReference<List<String>>() {}, Collections.emptyList()));
        ctx.put("weaknesses", fromJson(ra.getWeaknessesJson(), new TypeReference<List<String>>() {}, Collections.emptyList()));
        ctx.put("detectedSkills", fromJson(ra.getDetectedSkillsJson(), new TypeReference<List<String>>() {}, Collections.emptyList()));
        ctx.put("missingSkills", fromJson(ra.getMissingSkillsJson(), new TypeReference<List<String>>() {}, Collections.emptyList()));
        ctx.put("suggestions", fromJson(ra.getSuggestionsJson(), new TypeReference<List<String>>() {}, Collections.emptyList()));
        return toJson(ctx);
    }

    private String buildSkillGapContext(User user) {
        Optional<SkillGapAnalysis> latest = skillGapAnalysisRepository.findFirstByUserOrderByAnalyzedAtDesc(user);
        if (latest.isEmpty()) {
            return "{}";
        }
        SkillGapAnalysis sga = latest.get();
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("targetRole", sga.getTargetRole());
        ctx.put("readinessScore", sga.getReadinessScore());
        ctx.put("skillCoverageScore", sga.getSkillCoverageScore());
        ctx.put("gapSummary", sga.getSummary());
        ctx.put("currentSkills", fromJson(sga.getCurrentSkillsJson(), new TypeReference<List<String>>() {}, Collections.emptyList()));
        ctx.put("missingSkills", fromJson(sga.getMissingSkillsJson(), new TypeReference<List<String>>() {}, Collections.emptyList()));
        ctx.put("skillGaps", fromJson(sga.getSkillGapsJson(), new TypeReference<List<Object>>() {}, Collections.emptyList()));
        return toJson(ctx);
    }

    private String buildJobMatchContext(User user) {
        List<JobMatch> matches = jobMatchRepository.findByUserOrderByOverallScoreDesc(user);
        List<SavedJob> savedJobs = savedJobRepository.findByUserOrderByCreatedAtDesc(user);

        Map<String, Object> ctx = new HashMap<>();
        ctx.put("totalJobsMatched", matches.size());
        ctx.put("totalSavedJobs", savedJobs.size());

        List<Map<String, Object>> topMatches = new ArrayList<>();
        double totalScore = 0;
        int count = 0;
        for (JobMatch jm : matches) {
            if (count < 5) {
                Map<String, Object> m = new HashMap<>();
                if (jm.getJob() != null) {
                    m.put("jobTitle", jm.getJob().getTitle());
                    m.put("company", jm.getJob().getCompany());
                }
                m.put("overallScore", jm.getOverallScore());
                m.put("skillScore", jm.getSkillScore());
                topMatches.add(m);
            }
            if (jm.getOverallScore() != null) {
                totalScore += jm.getOverallScore();
                count++;
            }
        }
        ctx.put("topMatches", topMatches);
        ctx.put("averageJobMatchScore", count > 0 ? (totalScore / count) : 0.0);
        return toJson(ctx);
    }

    private String buildMockInterviewContext(User user) {
        List<InterviewSession> sessions = interviewSessionRepository.findByUserOrderByCreatedAtDesc(user);
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("totalInterviewsCompleted", sessions.size());

        if (sessions.isEmpty()) {
            ctx.put("averageOverallScore", 0.0);
            ctx.put("averageTechnicalScore", 0.0);
            ctx.put("averageBehavioralScore", 0.0);
            ctx.put("averageCommunicationScore", 0.0);
            return toJson(ctx);
        }

        double totalOverall = 0;
        double totalTech = 0;
        double totalComm = 0;
        int completedCount = 0;

        List<Map<String, Object>> recentSessions = new ArrayList<>();
        for (InterviewSession s : sessions) {
            if (s.getOverallScore() != null) {
                totalOverall += s.getOverallScore();
                totalTech += (s.getTechnicalScore() != null ? s.getTechnicalScore() : s.getOverallScore());
                totalComm += (s.getCommunicationScore() != null ? s.getCommunicationScore() : s.getOverallScore());
                completedCount++;
            }
            if (recentSessions.size() < 3) {
                Map<String, Object> sm = new HashMap<>();
                sm.put("targetRole", s.getTargetRole());
                sm.put("overallScore", s.getOverallScore());
                sm.put("status", s.getStatus());
                recentSessions.add(sm);
            }
        }

        ctx.put("recentSessions", recentSessions);
        ctx.put("averageOverallScore", completedCount > 0 ? (totalOverall / completedCount) : 0.0);
        ctx.put("averageTechnicalScore", completedCount > 0 ? (totalTech / completedCount) : 0.0);
        ctx.put("averageCommunicationScore", completedCount > 0 ? (totalComm / completedCount) : 0.0);
        return toJson(ctx);
    }

    private String buildRoadmapContext(User user) {
        Optional<CareerRoadmap> roadmapOpt = careerRoadmapRepository.findTopByUserOrderByUpdatedAtDesc(user);
        if (roadmapOpt.isEmpty()) {
            return "{}";
        }
        CareerRoadmap r = roadmapOpt.get();
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("targetRole", r.getTargetRole());
        ctx.put("estimatedDurationWeeks", r.getEstimatedDurationWeeks());
        ctx.put("overallProgress", r.getOverallProgress());
        ctx.put("overallReadinessScore", r.getOverallReadinessScore());
        ctx.put("summary", r.getSummary());
        return toJson(ctx);
    }

    private String buildJobPreparationContext(User user) {
        List<JobPreparation> preps = jobPreparationRepository.findByUserOrderByUpdatedAtDesc(user);
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("totalPreparationSessions", preps.size());

        List<Map<String, Object>> list = new ArrayList<>();
        for (JobPreparation p : preps) {
            if (list.size() < 3) {
                Map<String, Object> pm = new HashMap<>();
                if (p.getJob() != null) {
                    pm.put("jobTitle", p.getJob().getTitle());
                    pm.put("company", p.getJob().getCompany());
                }
                pm.put("readinessScore", p.getReadinessScore());
                pm.put("preparationLevel", p.getPreparationLevel());
                list.add(pm);
            }
        }
        ctx.put("recentPreparations", list);
        return toJson(ctx);
    }

    // ==========================================
    // HELPERS & RESPONSE MAPPING
    // ==========================================

    public CareerAnalyticsResponse toResponse(CareerAnalytics analytics) {
        List<String> strengths = fromJson(analytics.getStrengthsJson(),
                new TypeReference<List<String>>() {}, Collections.emptyList());
        List<CareerAnalyticsDto.WeaknessItem> weaknesses = fromJson(analytics.getWeaknessesJson(),
                new TypeReference<List<CareerAnalyticsDto.WeaknessItem>>() {}, Collections.emptyList());
        List<CareerAnalyticsDto.SkillProgressItem> skillProgress = fromJson(analytics.getSkillProgressJson(),
                new TypeReference<List<CareerAnalyticsDto.SkillProgressItem>>() {}, Collections.emptyList());

        CareerAnalyticsDto.InterviewProgressSummary interviewProgress = fromJson(analytics.getInterviewProgressJson(),
                new TypeReference<CareerAnalyticsDto.InterviewProgressSummary>() {}, new CareerAnalyticsDto.InterviewProgressSummary());
        CareerAnalyticsDto.JobSearchProgressSummary jobSearchProgress = fromJson(analytics.getJobSearchProgressJson(),
                new TypeReference<CareerAnalyticsDto.JobSearchProgressSummary>() {}, new CareerAnalyticsDto.JobSearchProgressSummary());
        CareerAnalyticsDto.RoadmapProgressSummary roadmapProgress = fromJson(analytics.getRoadmapProgressJson(),
                new TypeReference<CareerAnalyticsDto.RoadmapProgressSummary>() {}, new CareerAnalyticsDto.RoadmapProgressSummary());

        List<CareerAnalyticsDto.RecommendationItem> recommendations = fromJson(analytics.getRecommendationsJson(),
                new TypeReference<List<CareerAnalyticsDto.RecommendationItem>>() {}, Collections.emptyList());
        List<CareerAnalyticsDto.WeeklyGoalItem> weeklyGoals = fromJson(analytics.getWeeklyGoalsJson(),
                new TypeReference<List<CareerAnalyticsDto.WeeklyGoalItem>>() {}, Collections.emptyList());

        if (strengths == null) strengths = Collections.emptyList();
        if (weaknesses == null) weaknesses = Collections.emptyList();
        if (skillProgress == null) skillProgress = Collections.emptyList();
        if (interviewProgress == null) interviewProgress = new CareerAnalyticsDto.InterviewProgressSummary();
        if (jobSearchProgress == null) jobSearchProgress = new CareerAnalyticsDto.JobSearchProgressSummary();
        if (roadmapProgress == null) roadmapProgress = new CareerAnalyticsDto.RoadmapProgressSummary();
        if (recommendations == null) recommendations = Collections.emptyList();
        if (weeklyGoals == null) weeklyGoals = Collections.emptyList();

        return CareerAnalyticsResponse.builder()
                .id(analytics.getId())
                .overallReadinessScore(analytics.getOverallReadinessScore() != null ? analytics.getOverallReadinessScore() : 0)
                .previousReadinessScore(analytics.getPreviousReadinessScore())
                .readinessLevel(analytics.getReadinessLevel() != null ? analytics.getReadinessLevel() : "NEEDS PREPARATION")
                .careerSummary(analytics.getCareerSummary() != null ? analytics.getCareerSummary() : "")
                .strengths(strengths)
                .weaknesses(weaknesses)
                .skillProgress(skillProgress)
                .interviewProgress(interviewProgress)
                .jobSearchProgress(jobSearchProgress)
                .roadmapProgress(roadmapProgress)
                .recommendations(recommendations)
                .weeklyGoals(weeklyGoals)
                .generatedAt(analytics.getGeneratedAt())
                .updatedAt(analytics.getUpdatedAt())
                .build();
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private int clampScore(Integer val, int defaultScore) {
        if (val == null) return defaultScore;
        return Math.max(0, Math.min(100, val));
    }

    private String determineReadinessLevel(int score) {
        if (score >= 80) return "READY";
        if (score >= 60) return "ALMOST READY";
        if (score >= 40) return "NEEDS IMPROVEMENT";
        return "NEEDS PREPARATION";
    }

    private String toJson(Object obj) {
        if (obj == null) return "[]";
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("Failed to serialize object to JSON: {}", e.getMessage());
            return "[]";
        }
    }

    private <T> T fromJson(String json, TypeReference<T> typeRef, T defaultValue) {
        if (json == null || json.trim().isEmpty() || "null".equalsIgnoreCase(json.trim())) return defaultValue;
        try {
            T val = objectMapper.readValue(json, typeRef);
            return val != null ? val : defaultValue;
        } catch (Exception e) {
            log.warn("Failed to deserialize JSON: {}", e.getMessage());
            return defaultValue;
        }
    }
}
