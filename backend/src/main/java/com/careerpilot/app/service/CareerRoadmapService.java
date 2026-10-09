package com.careerpilot.app.service;

import com.careerpilot.app.dto.CareerRoadmapDto;
import com.careerpilot.app.dto.LearningRecommendationDto;
import com.careerpilot.app.dto.request.CareerRoadmapRequest;
import com.careerpilot.app.dto.response.*;
import com.careerpilot.app.entity.*;
import com.careerpilot.app.exception.ResourceNotFoundException;
import com.careerpilot.app.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CareerRoadmapService {

    private final CareerRoadmapRepository careerRoadmapRepository;
    private final LearningRecommendationRepository learningRecommendationRepository;
    private final RoadmapTaskRepository roadmapTaskRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final ResumeRepository resumeRepository;
    private final ResumeAnalysisRepository resumeAnalysisRepository;
    private final JobMatchRepository jobMatchRepository;
    private final SkillGapAnalysisRepository skillGapAnalysisRepository;
    private final InterviewSessionRepository interviewSessionRepository;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;

    /**
     * Generates a roadmap for the user. If a cached roadmap exists, returns it immediately.
     */
    @Transactional
    public CareerRoadmapResponse generateRoadmap(String userEmail) {
        User user = findUserByEmail(userEmail);
        Optional<CareerRoadmap> existing = careerRoadmapRepository.findTopByUserOrderByUpdatedAtDesc(user);
        if (existing.isPresent()) {
            log.info("Found cached CareerRoadmap ID {} for user {}. Returning cached result.",
                    existing.get().getId(), userEmail);
            return toResponse(existing.get());
        }
        return regenerateRoadmap(userEmail);
    }

    /**
     * Overload for backward compatibility with request parameter.
     */
    @Transactional
    public CareerRoadmapResponse generateRoadmap(CareerRoadmapRequest request, String userEmail) {
        return regenerateRoadmap(userEmail);
    }

    /**
     * Forces regeneration of the AI Career Roadmap and Learning Recommendations via Gemini AI.
     */
    @Transactional
    public CareerRoadmapResponse regenerateRoadmap(String userEmail) {
        long startTime = System.currentTimeMillis();
        User user = findUserByEmail(userEmail);

        log.info("[DIAGNOSTIC] Career Roadmap generation started for user {}", userEmail);

        // 1. Build compact candidate profile context
        String candidateProfileJson = buildCandidateProfileContext(user);

        // 2. Call Gemini AI
        log.info("Requesting AI Career Roadmap from Gemini for user {}", userEmail);
        CareerRoadmapDto dto = geminiService.analyzeCareerRoadmap(candidateProfileJson);

        // 3. Save or update CareerRoadmap entity
        CareerRoadmap roadmap = careerRoadmapRepository.findTopByUserOrderByUpdatedAtDesc(user)
                .orElseGet(() -> CareerRoadmap.builder().user(user).build());

        roadmap.setUser(user);
        roadmap.setTargetRole(dto.getTargetRole() != null ? dto.getTargetRole() : "Software Developer");
        roadmap.setCurrentLevel(dto.getCurrentLevel() != null ? dto.getCurrentLevel() : "Fresher / Entry Level");
        roadmap.setOverallReadinessScore(dto.getOverallReadinessScore() != null ? dto.getOverallReadinessScore() : 70);
        roadmap.setSummary(dto.getSummary());
        roadmap.setCurrentSkillsJson(toJson(dto.getCurrentSkills()));
        roadmap.setSkillGapsJson(toJson(dto.getSkillGaps()));
        roadmap.setRecommendedSkillsJson(toJson(dto.getRecommendedSkills()));
        roadmap.setRoadmapJson(toJson(dto.getRoadmap()));
        roadmap.setProjectRecommendationsJson(toJson(dto.getProjectRecommendations()));
        roadmap.setJobReadinessSummary(dto.getJobReadinessSummary());

        // Legacy compatibility fields
        roadmap.setAiSummary(dto.getSummary());
        roadmap.setOverallProgress(dto.getOverallReadinessScore() != null ? dto.getOverallReadinessScore() : 0);
        roadmap.setEstimatedDurationWeeks(dto.getRoadmap() != null ? dto.getRoadmap().size() * 4 : 12);

        roadmap = careerRoadmapRepository.save(roadmap);

        // 4. Delete previous LearningRecommendation records for this user
        learningRecommendationRepository.deleteByUser(user);

        // 5. Save new LearningRecommendation records
        List<LearningRecommendation> savedRecs = new ArrayList<>();
        if (dto.getLearningRecommendations() != null) {
            for (var recDto : dto.getLearningRecommendations()) {
                LearningRecommendation rec = LearningRecommendation.builder()
                        .user(user)
                        .skill(recDto.getSkill())
                        .priority(recDto.getPriority())
                        .reason(recDto.getReason())
                        .learningObjective(recDto.getLearningObjective())
                        .recommendedTopicsJson(toJson(recDto.getRecommendedTopics()))
                        .projectIdea(recDto.getProjectIdea())
                        .estimatedEffort(recDto.getEstimatedEffort())
                        .build();
                savedRecs.add(learningRecommendationRepository.save(rec));
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("[DIAGNOSTIC] Generated CareerRoadmap ID {} with {} learning recommendations in {} ms for user {}",
                roadmap.getId(), savedRecs.size(), duration, userEmail);

        return toResponse(roadmap);
    }

    /**
     * Gets the latest career roadmap for the authenticated user.
     */
    @Transactional(readOnly = true)
    public CareerRoadmapResponse getLatestRoadmap(String userEmail) {
        User user = findUserByEmail(userEmail);
        CareerRoadmap roadmap = careerRoadmapRepository.findTopByUserOrderByUpdatedAtDesc(user)
                .orElseThrow(() -> new ResourceNotFoundException("CareerRoadmap", "user", userEmail));
        return toResponse(roadmap);
    }

    /**
     * Gets learning recommendations for the authenticated user.
     */
    @Transactional(readOnly = true)
    public List<LearningRecommendationResponse> getLearningRecommendations(String userEmail) {
        User user = findUserByEmail(userEmail);
        List<LearningRecommendation> recs = learningRecommendationRepository.findByUserOrderByIdAsc(user);
        return recs.stream().map(this::toLearningResponse).toList();
    }

    /**
     * Gets all career roadmaps for the user.
     */
    @Transactional(readOnly = true)
    public List<CareerRoadmapResponse> getRoadmaps(String userEmail) {
        User user = findUserByEmail(userEmail);
        List<CareerRoadmap> list = careerRoadmapRepository.findByUserOrderByUpdatedAtDesc(user);
        return list.stream().map(this::toResponse).toList();
    }

    /**
     * Gets a roadmap by ID.
     */
    @Transactional(readOnly = true)
    public CareerRoadmapResponse getRoadmapById(Long id, String userEmail) {
        User user = findUserByEmail(userEmail);
        CareerRoadmap roadmap = careerRoadmapRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("CareerRoadmap", "id", id));
        return toResponse(roadmap);
    }

    /**
     * Deletes a roadmap by ID.
     */
    @Transactional
    public void deleteRoadmap(Long id, String userEmail) {
        User user = findUserByEmail(userEmail);
        CareerRoadmap roadmap = careerRoadmapRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("CareerRoadmap", "id", id));
        careerRoadmapRepository.delete(roadmap);
        learningRecommendationRepository.deleteByUser(user);
        log.info("User {} deleted CareerRoadmap ID {}", userEmail, id);
    }

    /**
     * Completes a task (legacy compatibility).
     */
    @Transactional
    public RoadmapTaskResponse completeTask(Long roadmapId, Long taskId, String userEmail) {
        User user = findUserByEmail(userEmail);
        CareerRoadmap roadmap = careerRoadmapRepository.findByIdAndUser(roadmapId, user)
                .orElseThrow(() -> new ResourceNotFoundException("CareerRoadmap", "id", roadmapId));

        RoadmapTask task = roadmapTaskRepository.findByIdAndRoadmap(taskId, roadmap)
                .orElseThrow(() -> new ResourceNotFoundException("RoadmapTask", "id", taskId));

        task.setCompleted(true);
        task.setCompletedAt(LocalDateTime.now());
        task = roadmapTaskRepository.save(task);

        updateRoadmapProgress(roadmap);
        return toTaskResponse(task);
    }

    /**
     * Uncompletes a task (legacy compatibility).
     */
    @Transactional
    public RoadmapTaskResponse uncompleteTask(Long roadmapId, Long taskId, String userEmail) {
        User user = findUserByEmail(userEmail);
        CareerRoadmap roadmap = careerRoadmapRepository.findByIdAndUser(roadmapId, user)
                .orElseThrow(() -> new ResourceNotFoundException("CareerRoadmap", "id", roadmapId));

        RoadmapTask task = roadmapTaskRepository.findByIdAndRoadmap(taskId, roadmap)
                .orElseThrow(() -> new ResourceNotFoundException("RoadmapTask", "id", taskId));

        task.setCompleted(false);
        task.setCompletedAt(null);
        task = roadmapTaskRepository.save(task);

        updateRoadmapProgress(roadmap);
        return toTaskResponse(task);
    }

    /**
     * Gets progress stats (legacy compatibility).
     */
    @Transactional(readOnly = true)
    public RoadmapProgressResponse getProgress(Long roadmapId, String userEmail) {
        User user = findUserByEmail(userEmail);
        CareerRoadmap roadmap = careerRoadmapRepository.findByIdAndUser(roadmapId, user)
                .orElseThrow(() -> new ResourceNotFoundException("CareerRoadmap", "id", roadmapId));

        List<RoadmapTask> tasks = roadmapTaskRepository.findByRoadmapOrderByWeekNumberAscIdAsc(roadmap);
        int total = tasks.size();
        int completed = (int) tasks.stream().filter(t -> Boolean.TRUE.equals(t.getCompleted())).count();
        int progress = total == 0 ? (roadmap.getOverallReadinessScore() != null ? roadmap.getOverallReadinessScore() : 0)
                : (int) Math.round(((double) completed / total) * 100);

        int completedHours = tasks.stream()
                .filter(t -> Boolean.TRUE.equals(t.getCompleted()))
                .mapToInt(t -> t.getEstimatedHours() != null ? t.getEstimatedHours() : 0)
                .sum();

        int remainingHours = tasks.stream()
                .filter(t -> !Boolean.TRUE.equals(t.getCompleted()))
                .mapToInt(t -> t.getEstimatedHours() != null ? t.getEstimatedHours() : 0)
                .sum();

        return RoadmapProgressResponse.builder()
                .totalTasks(total)
                .completedTasks(completed)
                .progressPercentage(progress)
                .completedHours(completedHours)
                .remainingHours(remainingHours)
                .build();
    }

    private void updateRoadmapProgress(CareerRoadmap roadmap) {
        List<RoadmapTask> tasks = roadmapTaskRepository.findByRoadmapOrderByWeekNumberAscIdAsc(roadmap);
        if (tasks.isEmpty()) {
            roadmap.setOverallProgress(roadmap.getOverallReadinessScore() != null ? roadmap.getOverallReadinessScore() : 0);
        } else {
            long completed = tasks.stream().filter(t -> Boolean.TRUE.equals(t.getCompleted())).count();
            int progress = (int) Math.round(((double) completed / tasks.size()) * 100);
            roadmap.setOverallProgress(progress);
        }
        careerRoadmapRepository.save(roadmap);
    }

    private String buildCandidateProfileContext(User user) {
        Profile profile = profileRepository.findByUser(user).orElse(null);
        Resume resume = resumeRepository.findFirstByUserOrderByUploadedAtDesc(user).orElse(null);
        ResumeAnalysis resumeAnalysis = (resume != null) ? resumeAnalysisRepository.findByResume(resume).orElse(null) : null;

        Map<String, Object> map = new HashMap<>();

        // Target Role & Level
        String targetRole = (profile != null && profile.getTargetRole() != null && !profile.getTargetRole().trim().isEmpty())
                ? profile.getTargetRole().trim()
                : (resumeAnalysis != null && resumeAnalysis.getTargetRole() != null ? resumeAnalysis.getTargetRole() : "Software Developer");
        map.put("targetRole", targetRole);

        String experienceLevel = (profile != null && profile.getExperienceLevel() != null)
                ? profile.getExperienceLevel()
                : "Fresher / Entry Level";
        map.put("experienceLevel", experienceLevel);

        // Education
        if (profile != null) {
            Map<String, String> edu = new HashMap<>();
            if (profile.getDegree() != null) edu.put("degree", profile.getDegree());
            if (profile.getBranch() != null) edu.put("branch", profile.getBranch());
            if (profile.getCollege() != null) edu.put("college", profile.getCollege());
            map.put("education", edu);
        }

        // Detected & Missing Skills from Resume
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
                map.put("resumeAnalysisSummary", resumeAnalysis.getSummary());
            }
        }

        if (profile != null && profile.getSkillsSummary() != null && !profile.getSkillsSummary().trim().isEmpty()) {
            String[] profileSkills = profile.getSkillsSummary().split("[,;\\n]+");
            for (String ps : profileSkills) {
                String clean = ps.trim();
                if (!clean.isEmpty() && !detectedSkills.contains(clean)) {
                    detectedSkills.add(clean);
                }
            }
        }

        if (detectedSkills.isEmpty()) {
            detectedSkills.add("Software Engineering Core");
        }

        map.put("detectedResumeSkills", detectedSkills);
        map.put("missingResumeSkills", missingResumeSkills);

        // Job Match data
        List<JobMatch> jobMatches = jobMatchRepository.findByUserOrderByOverallScoreDesc(user);
        if (!jobMatches.isEmpty()) {
            List<String> jobMatchingSkills = new ArrayList<>();
            List<String> jobMissingSkills = new ArrayList<>();
            double avgScore = 0;
            int count = 0;

            for (JobMatch match : jobMatches) {
                if (match.getOverallScore() != null) {
                    avgScore += match.getOverallScore();
                    count++;
                }
                List<String> mSkills = fromJson(match.getMatchingSkillsJson(), new TypeReference<List<String>>() {}, Collections.emptyList());
                for (String ms : mSkills) {
                    if (!jobMatchingSkills.contains(ms)) jobMatchingSkills.add(ms);
                }
                List<String> missSkills = fromJson(match.getMissingSkillsJson(), new TypeReference<List<String>>() {}, Collections.emptyList());
                for (String ms : missSkills) {
                    if (!jobMissingSkills.contains(ms)) jobMissingSkills.add(ms);
                }
            }

            map.put("jobMatchingSkills", jobMatchingSkills);
            map.put("jobMissingSkills", jobMissingSkills);
            if (count > 0) {
                map.put("averageJobMatchScore", Math.round(avgScore / count));
                map.put("topJobMatchScore", jobMatches.get(0).getOverallScore());
            }
        }

        // Latest Skill Gap Analysis
        SkillGapAnalysis latestGap = skillGapAnalysisRepository.findFirstByUserOrderByAnalyzedAtDesc(user).orElse(null);
        if (latestGap != null) {
            map.put("skillGapReadinessScore", latestGap.getReadinessScore());
            map.put("skillCoverageScore", latestGap.getSkillCoverageScore());
            List<String> missing = fromJson(latestGap.getMissingSkillsJson(), new TypeReference<List<String>>() {}, Collections.emptyList());
            map.put("skillGapMissingSkills", missing);
        }

        // Latest Mock Interview Evaluation
        List<InterviewSession> interviewSessions = interviewSessionRepository.findByUserOrderByCreatedAtDesc(user);
        if (!interviewSessions.isEmpty()) {
            InterviewSession latestInterview = interviewSessions.get(0);
            if (latestInterview.getOverallScore() != null) {
                map.put("latestMockInterviewScore", latestInterview.getOverallScore());
                map.put("latestMockInterviewRole", latestInterview.getTargetRole());
                List<String> weaknesses = fromJson(latestInterview.getWeaknessesJson(), new TypeReference<List<String>>() {}, Collections.emptyList());
                map.put("mockInterviewWeaknesses", weaknesses);
            }
        }

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

    public CareerRoadmapResponse toResponse(CareerRoadmap roadmap) {
        List<String> currentSkills = fromJson(roadmap.getCurrentSkillsJson(), new TypeReference<List<String>>() {}, Collections.emptyList());
        List<String> skillGaps = fromJson(roadmap.getSkillGapsJson(), new TypeReference<List<String>>() {}, Collections.emptyList());
        List<String> recommendedSkills = fromJson(roadmap.getRecommendedSkillsJson(), new TypeReference<List<String>>() {}, Collections.emptyList());
        List<CareerRoadmapDto.RoadmapPhaseDto> phases = fromJson(roadmap.getRoadmapJson(), new TypeReference<List<CareerRoadmapDto.RoadmapPhaseDto>>() {}, Collections.emptyList());
        List<String> projects = fromJson(roadmap.getProjectRecommendationsJson(), new TypeReference<List<String>>() {}, Collections.emptyList());

        List<LearningRecommendation> recEntities = learningRecommendationRepository.findByUserOrderByIdAsc(roadmap.getUser());
        List<LearningRecommendationResponse> recResponses = recEntities.stream().map(this::toLearningResponse).toList();

        List<RoadmapTask> taskEntities = (roadmap.getTasks() != null && !roadmap.getTasks().isEmpty())
                ? roadmap.getTasks()
                : roadmapTaskRepository.findByRoadmapOrderByWeekNumberAscIdAsc(roadmap);

        List<RoadmapTaskResponse> taskResponses = taskEntities.stream()
                .map(this::toTaskResponse)
                .toList();

        return CareerRoadmapResponse.builder()
                .id(roadmap.getId())
                .targetRole(roadmap.getTargetRole())
                .currentLevel(roadmap.getCurrentLevel())
                .overallReadinessScore(roadmap.getOverallReadinessScore() != null ? roadmap.getOverallReadinessScore() : 0)
                .summary(roadmap.getSummary() != null ? roadmap.getSummary() : roadmap.getAiSummary())
                .currentSkills(currentSkills)
                .skillGaps(skillGaps)
                .recommendedSkills(recommendedSkills)
                .roadmap(phases)
                .projectRecommendations(projects)
                .jobReadinessSummary(roadmap.getJobReadinessSummary())
                .learningRecommendations(recResponses)
                .createdAt(roadmap.getCreatedAt())
                .updatedAt(roadmap.getUpdatedAt())
                .overallProgress(roadmap.getOverallProgress())
                .aiSummary(roadmap.getSummary() != null ? roadmap.getSummary() : roadmap.getAiSummary())
                .estimatedDurationWeeks(roadmap.getEstimatedDurationWeeks())
                .tasks(taskResponses)
                .build();
    }

    public LearningRecommendationResponse toLearningResponse(LearningRecommendation rec) {
        List<String> topics = fromJson(rec.getRecommendedTopicsJson(), new TypeReference<List<String>>() {}, Collections.emptyList());
        return LearningRecommendationResponse.builder()
                .skill(rec.getSkill())
                .priority(rec.getPriority())
                .reason(rec.getReason())
                .learningObjective(rec.getLearningObjective())
                .recommendedTopics(topics)
                .projectIdea(rec.getProjectIdea())
                .estimatedEffort(rec.getEstimatedEffort())
                .build();
    }

    public RoadmapTaskResponse toTaskResponse(RoadmapTask task) {
        return RoadmapTaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .category(task.getCategory())
                .priority(task.getPriority())
                .estimatedHours(task.getEstimatedHours())
                .weekNumber(task.getWeekNumber())
                .completed(task.getCompleted())
                .completedAt(task.getCompletedAt())
                .resourceUrl(task.getResourceUrl())
                .build();
    }
}
