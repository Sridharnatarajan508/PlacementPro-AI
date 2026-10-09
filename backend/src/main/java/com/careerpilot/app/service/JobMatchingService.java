package com.careerpilot.app.service;

import com.careerpilot.app.dto.response.*;
import com.careerpilot.app.entity.*;
import com.careerpilot.app.exception.AIServiceException;
import com.careerpilot.app.exception.ResourceNotFoundException;
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
public class JobMatchingService {

    private final JobRepository jobRepository;
    private final JobMatchRepository jobMatchRepository;
    private final SavedJobRepository savedJobRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final ResumeRepository resumeRepository;
    private final ResumeAnalysisRepository resumeAnalysisRepository;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;

    @Transactional
    public JobMatchResponse matchJob(Long jobId, String userEmail) {
        long startTime = System.currentTimeMillis();
        log.info("[DIAGNOSTIC] POST /api/jobs/{}/match started for user {}", jobId, userEmail);

        long dbStart = System.currentTimeMillis();
        User user = findUserByEmail(userEmail);
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", jobId));

        // 1. Check if a JobMatch already exists for this user and job (CACHE CHECK)
        Optional<JobMatch> existing = jobMatchRepository.findByUserAndJob(user, job);
        long dbDuration = System.currentTimeMillis() - dbStart;

        if (existing.isPresent()) {
            long totalTime = System.currentTimeMillis() - startTime;
            log.info("[DIAGNOSTIC] Cache HIT for job ID {} and user {} (JobMatch ID: {}) - returned in {} ms (Gemini was NOT called)",
                    jobId, userEmail, existing.get().getId(), totalTime);
            return toMatchResponse(existing.get(), null);
        }

        log.info("[DIAGNOSTIC] Cache MISS for job ID {} and user {} (DB lookup took {} ms). Calling Gemini AI...",
                jobId, userEmail, dbDuration);

        // 2. Build candidate information from Profile and ResumeAnalysis
        String candidateProfileJson = buildCandidateProfileJson(user);

        // 3. Call Gemini AI for job matching
        long geminiStart = System.currentTimeMillis();
        log.info("[DIAGNOSTIC] Gemini AI job match request started for job '{}' (ID: {})", job.getTitle(), jobId);
        JobMatchDto matchDto = geminiService.analyzeJobMatch(candidateProfileJson, job);
        long geminiDuration = System.currentTimeMillis() - geminiStart;
        log.info("[DIAGNOSTIC] Gemini AI job match request completed in {} ms (Overall Score: {}%, Recommendation: {})",
                geminiDuration, matchDto.getOverallScore(), matchDto.getRecommendation());

        // 4. Save the result to MySQL
        long saveStart = System.currentTimeMillis();
        JobMatch jobMatch = JobMatch.builder()
                .user(user)
                .job(job)
                .overallScore(matchDto.getOverallScore())
                .skillScore(matchDto.getSkillScore())
                .experienceScore(matchDto.getExperienceScore())
                .educationScore(matchDto.getEducationScore())
                .matchingSkillsJson(toJson(matchDto.getMatchingSkills()))
                .missingSkillsJson(toJson(matchDto.getMissingSkills()))
                .recommendation(matchDto.getRecommendation())
                .aiExplanation(matchDto.getExplanation())
                .build();

        jobMatch = jobMatchRepository.save(jobMatch);
        long saveDuration = System.currentTimeMillis() - saveStart;
        long totalDuration = System.currentTimeMillis() - startTime;

        log.info("[DIAGNOSTIC] Saved JobMatch ID {} to MySQL in {} ms. Total matchJob processing time: {} ms",
                jobMatch.getId(), saveDuration, totalDuration);

        return toMatchResponse(jobMatch, matchDto);
    }

    @Transactional(readOnly = true)
    public JobMatchResponse getMatchForJob(Long jobId, String userEmail) {
        long startTime = System.currentTimeMillis();
        log.info("[DIAGNOSTIC] GET /api/jobs/{}/match started for user {}", jobId, userEmail);

        User user = findUserByEmail(userEmail);
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", jobId));

        JobMatch jobMatch = jobMatchRepository.findByUserAndJob(user, job)
                .orElseThrow(() -> new ResourceNotFoundException("JobMatch", "jobId", jobId));

        long totalTime = System.currentTimeMillis() - startTime;
        log.info("[DIAGNOSTIC] GET /api/jobs/{}/match completed in {} ms (Score: {}%)",
                jobId, totalTime, jobMatch.getOverallScore());

        return toMatchResponse(jobMatch, null);
    }

    @Transactional
    public List<JobRecommendationResponse> getRecommendedJobs(String userEmail) {
        long startTime = System.currentTimeMillis();
        log.info("[DIAGNOSTIC] GET /api/jobs/recommended started for user {}", userEmail);

        long dbStart = System.currentTimeMillis();
        User user = findUserByEmail(userEmail);
        Profile profile = profileRepository.findByUser(user).orElse(null);

        // Fetch user's existing saved and matched jobs
        List<SavedJob> saved = savedJobRepository.findByUserOrderByCreatedAtDesc(user);
        Set<Long> savedIds = saved.stream().map(s -> s.getJob().getId()).collect(Collectors.toSet());

        List<JobMatch> existingMatches = jobMatchRepository.findByUser(user);
        Map<Long, JobMatch> matchMap = existingMatches.stream()
                .collect(Collectors.toMap(m -> m.getJob().getId(), m -> m, (m1, m2) -> m1));

        // Load maximum 15 recent jobs per Phase 4 specification
        List<Job> candidateJobs = new ArrayList<>(jobRepository.findTop15ByOrderByPostedDateDescCreatedAtDesc());
        long dbDuration = System.currentTimeMillis() - dbStart;

        if (candidateJobs.isEmpty()) {
            log.info("[DIAGNOSTIC] GET /api/jobs/recommended completed in {} ms (0 jobs in DB)", System.currentTimeMillis() - startTime);
            return Collections.emptyList();
        }

        // If target role is present, prioritize relevant jobs
        String targetRole = profile != null ? profile.getTargetRole() : null;
        if (targetRole != null && !targetRole.trim().isEmpty()) {
            String roleLower = targetRole.toLowerCase();
            candidateJobs.sort((j1, j2) -> {
                boolean j1Rel = j1.getTitle().toLowerCase().contains(roleLower) || roleLower.contains(j1.getTitle().toLowerCase());
                boolean j2Rel = j2.getTitle().toLowerCase().contains(roleLower) || roleLower.contains(j2.getTitle().toLowerCase());
                if (j1Rel && !j2Rel) return -1;
                if (!j1Rel && j2Rel) return 1;
                return 0;
            });
        }

        List<JobRecommendationResponse> recommendations = new ArrayList<>();

        for (Job job : candidateJobs) {
            JobMatch match = matchMap.get(job.getId());
            boolean isSaved = savedIds.contains(job.getId());

            JobResponse jobResponse = JobResponse.builder()
                    .id(job.getId())
                    .title(job.getTitle())
                    .company(job.getCompany())
                    .location(job.getLocation())
                    .jobType(job.getJobType())
                    .experienceLevel(job.getExperienceLevel())
                    .salaryMin(job.getSalaryMin())
                    .salaryMax(job.getSalaryMax())
                    .description(job.getDescription())
                    .requiredSkills(job.getRequiredSkills())
                    .preferredSkills(job.getPreferredSkills())
                    .applicationUrl(job.getApplicationUrl())
                    .postedDate(job.getPostedDate())
                    .createdAt(job.getCreatedAt())
                    .isSaved(isSaved)
                    .matchScore(match != null ? match.getOverallScore() : null)
                    .matchRecommendation(match != null ? match.getRecommendation() : null)
                    .build();

            JobMatchResponse matchResponse = match != null ? toMatchResponse(match, null) : null;

            recommendations.add(JobRecommendationResponse.builder()
                    .job(jobResponse)
                    .match(matchResponse)
                    .build());
        }

        // Sort by match score descending (matched jobs first, ordered by score; unmatched after)
        recommendations.sort((r1, r2) -> {
            Integer s1 = r1.getMatch() != null ? r1.getMatch().getOverallScore() : -1;
            Integer s2 = r2.getMatch() != null ? r2.getMatch().getOverallScore() : -1;
            return s2.compareTo(s1);
        });

        long totalTime = System.currentTimeMillis() - startTime;
        log.info("[DIAGNOSTIC] GET /api/jobs/recommended completed in {} ms (DB operations: {} ms, returned {} jobs, {} with cached matches)",
                totalTime, dbDuration, recommendations.size(), matchMap.size());

        return recommendations;
    }

    private String buildCandidateProfileJson(User user) {
        Profile profile = profileRepository.findByUser(user).orElse(null);
        Resume resume = resumeRepository.findFirstByUserOrderByUploadedAtDesc(user).orElse(null);
        ResumeAnalysis analysis = (resume != null) ? resumeAnalysisRepository.findByResume(resume).orElse(null) : null;

        Map<String, Object> candidateMap = new HashMap<>();

        // Target Role & Experience
        String targetRole = (profile != null && profile.getTargetRole() != null)
                ? profile.getTargetRole()
                : (analysis != null && analysis.getTargetRole() != null ? analysis.getTargetRole() : "Software Developer");
        candidateMap.put("targetRole", targetRole);

        String expLevel = (profile != null && profile.getExperienceLevel() != null)
                ? profile.getExperienceLevel()
                : "Fresher / Entry Level";
        candidateMap.put("experienceLevel", expLevel);

        // Skills from Resume Analysis & Profile
        List<String> skills = new ArrayList<>();
        List<String> missingSkills = new ArrayList<>();

        if (analysis != null) {
            List<DetectedSkillDto> detected = fromJson(analysis.getDetectedSkillsJson(),
                    new TypeReference<List<DetectedSkillDto>>() {}, Collections.emptyList());
            skills.addAll(detected.stream().map(DetectedSkillDto::getName).filter(Objects::nonNull).toList());

            List<MissingSkillDto> missing = fromJson(analysis.getMissingSkillsJson(),
                    new TypeReference<List<MissingSkillDto>>() {}, Collections.emptyList());
            missingSkills.addAll(missing.stream().map(MissingSkillDto::getName).filter(Objects::nonNull).toList());

            if (analysis.getSummary() != null) {
                candidateMap.put("summary", analysis.getSummary());
            }
        }

        if (profile != null && profile.getSkillsSummary() != null && !profile.getSkillsSummary().trim().isEmpty()) {
            String[] profileSkills = profile.getSkillsSummary().split("[,;\\n]+");
            for (String ps : profileSkills) {
                String clean = ps.trim();
                if (!clean.isEmpty() && !skills.contains(clean)) {
                    skills.add(clean);
                }
            }
        }

        if (skills.isEmpty()) {
            skills.add("General Software Engineering");
        }

        candidateMap.put("skills", skills);
        candidateMap.put("missingSkills", missingSkills);

        // Education
        Map<String, String> education = new HashMap<>();
        if (profile != null) {
            if (profile.getDegree() != null) education.put("degree", profile.getDegree());
            if (profile.getBranch() != null) education.put("branch", profile.getBranch());
            if (profile.getCollege() != null) education.put("college", profile.getCollege());
            if (profile.getGraduationYear() != null) education.put("graduationYear", String.valueOf(profile.getGraduationYear()));
        }
        candidateMap.put("education", education);

        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(candidateMap);
        } catch (Exception e) {
            log.warn("Failed to serialize candidate profile map to JSON: {}", e.getMessage());
            return "{\"targetRole\":\"" + targetRole + "\",\"experienceLevel\":\"" + expLevel + "\"}";
        }
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("Failed to serialize to JSON: {}", e.getMessage());
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

    public JobMatchResponse toMatchResponse(JobMatch entity, JobMatchDto dto) {
        List<String> matching = dto != null ? dto.getMatchingSkills()
                : fromJson(entity.getMatchingSkillsJson(), new TypeReference<List<String>>() {}, Collections.emptyList());

        List<String> missing = dto != null ? dto.getMissingSkills()
                : fromJson(entity.getMissingSkillsJson(), new TypeReference<List<String>>() {}, Collections.emptyList());

        return JobMatchResponse.builder()
                .id(entity.getId())
                .jobId(entity.getJob().getId())
                .overallScore(entity.getOverallScore())
                .skillScore(entity.getSkillScore())
                .experienceScore(entity.getExperienceScore())
                .educationScore(entity.getEducationScore())
                .matchingSkills(matching)
                .missingSkills(missing)
                .recommendation(entity.getRecommendation())
                .aiExplanation(entity.getAiExplanation())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
