package com.careerpilot.app.service;

import com.careerpilot.app.dto.response.*;
import com.careerpilot.app.entity.Profile;
import com.careerpilot.app.entity.Resume;
import com.careerpilot.app.entity.ResumeAnalysis;
import com.careerpilot.app.entity.User;
import com.careerpilot.app.exception.PdfExtractionException;
import com.careerpilot.app.exception.ResourceNotFoundException;
import com.careerpilot.app.repository.ProfileRepository;
import com.careerpilot.app.repository.ResumeAnalysisRepository;
import com.careerpilot.app.repository.ResumeRepository;
import com.careerpilot.app.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeAnalysisService {

    private final ResumeRepository resumeRepository;
    private final ResumeAnalysisRepository resumeAnalysisRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;

    @Transactional
    public ResumeAnalysisResponse analyzeResume(String userEmail, Long resumeId) {
        User user = findUserByEmail(userEmail);
        Resume resume = resumeRepository.findByIdAndUser(resumeId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Resume", "id", resumeId));

        if (resume.getExtractedText() == null || resume.getExtractedText().trim().isEmpty()) {
            throw new PdfExtractionException("Your resume contains no extractable text. Please upload a readable PDF resume before requesting AI analysis.");
        }

        Profile profile = profileRepository.findByUser(user).orElse(null);
        String targetRole = profile != null ? profile.getTargetRole() : null;
        String profileContext = buildProfileContext(profile);

        log.info("Requesting Gemini AI analysis for resume ID {} (user: {})", resumeId, userEmail);
        ResumeAnalysisDto analysisDto = geminiService.analyzeResumeText(resume.getExtractedText(), targetRole, profileContext);

        ResumeAnalysis analysis = resumeAnalysisRepository.findByResume(resume)
                .orElse(ResumeAnalysis.builder().resume(resume).build());

        analysis.setOverallScore(analysisDto.getOverallScore());
        analysis.setAtsScore(analysisDto.getAtsScore());
        analysis.setTargetRole(targetRole != null ? targetRole : "Software Developer");
        analysis.setSummary(analysisDto.getSummary());

        // Serialize complex fields to JSON for persistence
        analysis.setStrengthsJson(toJson(analysisDto.getStrengths()));
        analysis.setWeaknessesJson(toJson(analysisDto.getWeaknesses()));
        analysis.setDetectedSkillsJson(toJson(analysisDto.getDetectedSkills()));
        analysis.setMissingSkillsJson(toJson(analysisDto.getMissingSkills()));
        analysis.setSuggestionsJson(toJson(analysisDto.getSuggestions()));
        analysis.setSectionAnalysisJson(toJson(analysisDto.getSectionAnalysis()));
        analysis.setRawAnalysisJson(toJson(analysisDto));
        analysis.setAnalyzedAt(LocalDateTime.now());

        analysis = resumeAnalysisRepository.save(analysis);
        log.info("Saved Gemini AI resume analysis ID {} for resume ID {}", analysis.getId(), resumeId);

        return toResponse(analysis, analysisDto);
    }

    public ResumeAnalysisResponse getAnalysisForResume(String userEmail, Long resumeId) {
        User user = findUserByEmail(userEmail);
        // Ensure user owns the resume
        if (!resumeRepository.existsByIdAndUser(resumeId, user)) {
            throw new ResourceNotFoundException("Resume", "id", resumeId);
        }

        ResumeAnalysis analysis = resumeAnalysisRepository.findByResumeId(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("ResumeAnalysis", "resumeId", resumeId));

        return toResponse(analysis, null);
    }

    @Transactional
    public void deleteAnalysisForResume(String userEmail, Long resumeId) {
        User user = findUserByEmail(userEmail);
        Resume resume = resumeRepository.findByIdAndUser(resumeId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Resume", "id", resumeId));

        resumeAnalysisRepository.deleteByResume(resume);
        log.info("Deleted resume analysis for resume ID {}", resumeId);
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private String buildProfileContext(Profile profile) {
        if (profile == null) return "No user profile set";
        StringBuilder sb = new StringBuilder();
        if (profile.getDegree() != null) sb.append("Degree: ").append(profile.getDegree()).append("; ");
        if (profile.getBranch() != null) sb.append("Branch: ").append(profile.getBranch()).append("; ");
        if (profile.getExperienceLevel() != null) sb.append("Experience: ").append(profile.getExperienceLevel()).append("; ");
        if (profile.getSkillsSummary() != null) sb.append("Profile Skills: ").append(profile.getSkillsSummary());
        return sb.toString();
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

    private ResumeAnalysisResponse toResponse(ResumeAnalysis entity, ResumeAnalysisDto dto) {
        List<String> strengths = dto != null ? dto.getStrengths()
                : fromJson(entity.getStrengthsJson(), new TypeReference<List<String>>() {}, Collections.emptyList());

        List<String> weaknesses = dto != null ? dto.getWeaknesses()
                : fromJson(entity.getWeaknessesJson(), new TypeReference<List<String>>() {}, Collections.emptyList());

        List<DetectedSkillDto> detectedSkills = dto != null ? dto.getDetectedSkills()
                : fromJson(entity.getDetectedSkillsJson(), new TypeReference<List<DetectedSkillDto>>() {}, Collections.emptyList());

        List<MissingSkillDto> missingSkills = dto != null ? dto.getMissingSkills()
                : fromJson(entity.getMissingSkillsJson(), new TypeReference<List<MissingSkillDto>>() {}, Collections.emptyList());

        List<String> suggestions = dto != null ? dto.getSuggestions()
                : fromJson(entity.getSuggestionsJson(), new TypeReference<List<String>>() {}, Collections.emptyList());

        SectionAnalysisDto sectionAnalysis = dto != null ? dto.getSectionAnalysis()
                : fromJson(entity.getSectionAnalysisJson(), new TypeReference<SectionAnalysisDto>() {}, new SectionAnalysisDto());

        return ResumeAnalysisResponse.builder()
                .id(entity.getId())
                .resumeId(entity.getResume().getId())
                .overallScore(entity.getOverallScore())
                .atsScore(entity.getAtsScore())
                .targetRole(entity.getTargetRole())
                .summary(entity.getSummary())
                .strengths(strengths)
                .weaknesses(weaknesses)
                .detectedSkills(detectedSkills)
                .missingSkills(missingSkills)
                .suggestions(suggestions)
                .sectionAnalysis(sectionAnalysis)
                .analyzedAt(entity.getAnalyzedAt())
                .build();
    }
}
