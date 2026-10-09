package com.careerpilot.app.service;

import com.careerpilot.app.dto.request.SkillGapAnalyzeRequest;
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
public class SkillGapService {

    private final SkillGapAnalysisRepository skillGapAnalysisRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final ResumeRepository resumeRepository;
    private final ResumeAnalysisRepository resumeAnalysisRepository;
    private final JobRepository jobRepository;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;

    @Transactional
    public SkillGapAnalysisResponse analyzeSkillGap(SkillGapAnalyzeRequest request, String userEmail) {
        User user = findUserByEmail(userEmail);
        Profile profile = profileRepository.findByUser(user).orElse(null);

        // 1. Resolve Target Job (if provided)
        Job job = null;
        if (request.getJobId() != null) {
            job = jobRepository.findById(request.getJobId())
                    .orElseThrow(() -> new ResourceNotFoundException("Job", "id", request.getJobId()));
        }

        // 2. Resolve Target Role
        String targetRole = null;
        if (request.getTargetRole() != null && !request.getTargetRole().trim().isEmpty()) {
            targetRole = request.getTargetRole().trim();
        } else if (job != null) {
            targetRole = job.getTitle();
        } else if (profile != null && profile.getTargetRole() != null && !profile.getTargetRole().trim().isEmpty()) {
            targetRole = profile.getTargetRole().trim();
        } else {
            throw new IllegalArgumentException("Please specify a target role or select a job to analyze your skill gap.");
        }

        // 3. Cache check (unless forceRefresh is true)
        boolean forceRefresh = Boolean.TRUE.equals(request.getForceRefresh());
        if (!forceRefresh) {
            Optional<SkillGapAnalysis> cached;
            if (job != null) {
                cached = skillGapAnalysisRepository.findFirstByUserAndJobOrderByAnalyzedAtDesc(user, job);
            } else {
                cached = skillGapAnalysisRepository.findFirstByUserAndTargetRoleIgnoreCaseOrderByAnalyzedAtDesc(user, targetRole);
            }

            if (cached.isPresent()) {
                log.info("Returning cached SkillGapAnalysis ID {} for user {} and target '{}'",
                        cached.get().getId(), userEmail, targetRole);
                return toResponse(cached.get(), null);
            }
        }

        // 4. Build Candidate Profile Context
        String candidateProfileJson = buildCandidateProfileJson(user, profile);

        // 5. Call Gemini AI
        log.info("Requesting Gemini AI Skill Gap analysis for user {} on role '{}' (jobId: {})",
                userEmail, targetRole, (job != null ? job.getId() : "none"));
        SkillGapGeminiDto geminiDto = geminiService.analyzeSkillGap(candidateProfileJson, targetRole, job);

        // 6. Persist SkillGapAnalysis entity
        SkillGapAnalysis entity = SkillGapAnalysis.builder()
                .user(user)
                .job(job)
                .targetRole(targetRole)
                .readinessScore(geminiDto.getReadinessScore())
                .skillCoverageScore(geminiDto.getSkillCoverageScore())
                .summary(geminiDto.getSummary())
                .currentSkillsJson(toJson(geminiDto.getCurrentSkills()))
                .requiredSkillsJson(toJson(geminiDto.getRequiredSkills()))
                .missingSkillsJson(toJson(geminiDto.getMissingSkills()))
                .skillGapsJson(toJson(geminiDto.getSkillGaps()))
                .learningPlanJson(toJson(geminiDto.getLearningPlan()))
                .rawAnalysisJson(toJson(geminiDto))
                .analyzedAt(LocalDateTime.now())
                .build();

        entity = skillGapAnalysisRepository.save(entity);
        log.info("Saved SkillGapAnalysis ID {} with readiness score {}% for user {}",
                entity.getId(), entity.getReadinessScore(), userEmail);

        return toResponse(entity, geminiDto);
    }

    @Transactional(readOnly = true)
    public SkillGapAnalysisResponse getLatestSkillGap(String userEmail) {
        User user = findUserByEmail(userEmail);
        SkillGapAnalysis analysis = skillGapAnalysisRepository.findFirstByUserOrderByAnalyzedAtDesc(user)
                .orElseThrow(() -> new ResourceNotFoundException("SkillGapAnalysis", "user", userEmail));
        return toResponse(analysis, null);
    }

    @Transactional(readOnly = true)
    public List<SkillGapAnalysisResponse> getSkillGapHistory(String userEmail) {
        User user = findUserByEmail(userEmail);
        List<SkillGapAnalysis> list = skillGapAnalysisRepository.findByUserOrderByAnalyzedAtDesc(user);
        return list.stream().map(item -> toResponse(item, null)).toList();
    }

    @Transactional
    public void deleteSkillGap(Long id, String userEmail) {
        User user = findUserByEmail(userEmail);
        SkillGapAnalysis analysis = skillGapAnalysisRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("SkillGapAnalysis", "id", id));
        skillGapAnalysisRepository.delete(analysis);
        log.info("User {} deleted SkillGapAnalysis ID {}", userEmail, id);
    }

    private String buildCandidateProfileJson(User user, Profile profile) {
        Resume resume = resumeRepository.findFirstByUserOrderByUploadedAtDesc(user).orElse(null);
        ResumeAnalysis resumeAnalysis = (resume != null) ? resumeAnalysisRepository.findByResume(resume).orElse(null) : null;

        Map<String, Object> candidateMap = new HashMap<>();

        // Target Role & Experience
        String currentTarget = (profile != null && profile.getTargetRole() != null)
                ? profile.getTargetRole()
                : (resumeAnalysis != null && resumeAnalysis.getTargetRole() != null ? resumeAnalysis.getTargetRole() : "Software Developer");
        candidateMap.put("targetRole", currentTarget);

        String expLevel = (profile != null && profile.getExperienceLevel() != null)
                ? profile.getExperienceLevel()
                : "Fresher / Entry Level";
        candidateMap.put("experienceLevel", expLevel);

        // Skills from Resume Analysis & Profile
        List<String> skills = new ArrayList<>();
        List<String> missingSkills = new ArrayList<>();

        if (resumeAnalysis != null) {
            List<DetectedSkillDto> detected = fromJson(resumeAnalysis.getDetectedSkillsJson(),
                    new TypeReference<List<DetectedSkillDto>>() {}, Collections.emptyList());
            skills.addAll(detected.stream().map(DetectedSkillDto::getName).filter(Objects::nonNull).toList());

            List<MissingSkillDto> missing = fromJson(resumeAnalysis.getMissingSkillsJson(),
                    new TypeReference<List<MissingSkillDto>>() {}, Collections.emptyList());
            missingSkills.addAll(missing.stream().map(MissingSkillDto::getName).filter(Objects::nonNull).toList());

            if (resumeAnalysis.getSummary() != null) {
                candidateMap.put("resumeSummary", resumeAnalysis.getSummary());
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

        candidateMap.put("currentSkills", skills);
        candidateMap.put("previouslyFlaggedGaps", missingSkills);

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
            return "{\"targetRole\":\"" + currentTarget + "\",\"experienceLevel\":\"" + expLevel + "\"}";
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

    public SkillGapAnalysisResponse toResponse(SkillGapAnalysis entity, SkillGapGeminiDto dto) {
        List<String> current = dto != null ? dto.getCurrentSkills()
                : fromJson(entity.getCurrentSkillsJson(), new TypeReference<List<String>>() {}, Collections.emptyList());

        List<String> required = dto != null ? dto.getRequiredSkills()
                : fromJson(entity.getRequiredSkillsJson(), new TypeReference<List<String>>() {}, Collections.emptyList());

        List<String> missing = dto != null ? dto.getMissingSkills()
                : fromJson(entity.getMissingSkillsJson(), new TypeReference<List<String>>() {}, Collections.emptyList());

        List<SkillGapDto> gaps = dto != null ? dto.getSkillGaps()
                : fromJson(entity.getSkillGapsJson(), new TypeReference<List<SkillGapDto>>() {}, Collections.emptyList());

        List<LearningPlanStepDto> plan = dto != null ? dto.getLearningPlan()
                : fromJson(entity.getLearningPlanJson(), new TypeReference<List<LearningPlanStepDto>>() {}, Collections.emptyList());

        return SkillGapAnalysisResponse.builder()
                .id(entity.getId())
                .jobId(entity.getJob() != null ? entity.getJob().getId() : null)
                .jobTitle(entity.getJob() != null ? entity.getJob().getTitle() : null)
                .jobCompany(entity.getJob() != null ? entity.getJob().getCompany() : null)
                .targetRole(entity.getTargetRole())
                .readinessScore(entity.getReadinessScore())
                .skillCoverageScore(entity.getSkillCoverageScore())
                .summary(entity.getSummary())
                .currentSkills(current)
                .requiredSkills(required)
                .missingSkills(missing)
                .skillGaps(gaps)
                .learningPlan(plan)
                .analyzedAt(entity.getAnalyzedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
