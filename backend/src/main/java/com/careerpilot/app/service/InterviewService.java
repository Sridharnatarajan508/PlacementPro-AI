package com.careerpilot.app.service;

import com.careerpilot.app.dto.gemini.InterviewEvaluationDto;
import com.careerpilot.app.dto.gemini.InterviewFinalEvaluationDto;
import com.careerpilot.app.dto.gemini.InterviewQuestionDto;
import com.careerpilot.app.dto.request.InterviewStartRequest;
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
public class InterviewService {

    private final InterviewSessionRepository interviewSessionRepository;
    private final InterviewQuestionRepository interviewQuestionRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final ResumeRepository resumeRepository;
    private final ResumeAnalysisRepository resumeAnalysisRepository;
    private final JobRepository jobRepository;
    private final SkillGapAnalysisRepository skillGapAnalysisRepository;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;

    @Transactional
    public InterviewSessionResponse startInterview(InterviewStartRequest request, String userEmail) {
        User user = findUserByEmail(userEmail);
        Profile profile = profileRepository.findByUser(user).orElse(null);

        // 1. Resolve Target Job (if specified)
        Job targetJob = null;
        if (request != null && request.getTargetJobId() != null) {
            targetJob = jobRepository.findById(request.getTargetJobId())
                    .orElseThrow(() -> new ResourceNotFoundException("Job", "id", request.getTargetJobId()));
        }

        // 2. Resolve Target Role
        String targetRole = null;
        if (request != null && request.getTargetRole() != null && !request.getTargetRole().trim().isEmpty()) {
            targetRole = request.getTargetRole().trim();
        } else if (targetJob != null) {
            targetRole = targetJob.getTitle();
        } else if (profile != null && profile.getTargetRole() != null && !profile.getTargetRole().trim().isEmpty()) {
            targetRole = profile.getTargetRole().trim();
        } else {
            targetRole = "Software Engineer";
        }

        // 3. Resolve Interview Type & Difficulty & Question Count
        String interviewType = (request != null && request.getInterviewType() != null && !request.getInterviewType().trim().isEmpty())
                ? request.getInterviewType().trim().toUpperCase()
                : "MIXED";

        String difficulty = (request != null && request.getDifficulty() != null && !request.getDifficulty().trim().isEmpty())
                ? request.getDifficulty().trim().toUpperCase()
                : "MEDIUM";

        int totalQuestions = (request != null && request.getTotalQuestions() != null)
                ? (request.getTotalQuestions() == 5 || request.getTotalQuestions() == 15 ? request.getTotalQuestions() : 10)
                : 10;

        // 4. Build Candidate Profile Context
        String candidateProfileJson = buildCandidateProfileJson(user, profile, targetRole);
        String jobDetailsJson = buildJobDetailsJson(targetJob);

        // 5. Generate Question 1 via Gemini AI
        log.info("Generating question 1/{} via Gemini for user {} ({}, {})",
                totalQuestions, userEmail, targetRole, interviewType);
        InterviewQuestionDto qDto = geminiService.generateInterviewQuestion(
                candidateProfileJson, targetRole, jobDetailsJson, interviewType, difficulty, 1, totalQuestions, Collections.emptyList());

        // 6. Create InterviewSession
        InterviewSession session = InterviewSession.builder()
                .user(user)
                .targetRole(targetRole)
                .targetJob(targetJob)
                .interviewType(interviewType)
                .difficulty(difficulty)
                .totalQuestions(totalQuestions)
                .completedQuestions(0)
                .status("IN_PROGRESS")
                .startedAt(LocalDateTime.now())
                .build();

        session = interviewSessionRepository.save(session);

        // 7. Create Question 1
        InterviewQuestion question1 = InterviewQuestion.builder()
                .session(session)
                .questionNumber(1)
                .questionText(qDto.getQuestion())
                .category(qDto.getCategory() != null ? qDto.getCategory().toUpperCase() : "TECHNICAL")
                .expectedTopicsJson(toJson(qDto.getExpectedTopics()))
                .difficulty(qDto.getDifficulty() != null ? qDto.getDifficulty().toUpperCase() : difficulty)
                .build();

        question1 = interviewQuestionRepository.save(question1);
        session.addQuestion(question1);

        log.info("Started InterviewSession ID {} with Question 1 ID {} for user {}",
                session.getId(), question1.getId(), userEmail);

        return toSessionResponse(session);
    }

    @Transactional
    public InterviewEvaluationResponse submitAnswer(Long sessionId, Long questionId, String answer, String userEmail) {
        if (answer == null || answer.trim().isEmpty()) {
            throw new IllegalArgumentException("Answer cannot be blank.");
        }

        User user = findUserByEmail(userEmail);
        InterviewSession session = interviewSessionRepository.findByIdAndUser(sessionId, user)
                .orElseThrow(() -> new ResourceNotFoundException("InterviewSession", "id", sessionId));

        if ("COMPLETED".equalsIgnoreCase(session.getStatus())) {
            throw new IllegalStateException("Interview session is already completed.");
        }
        if ("ABANDONED".equalsIgnoreCase(session.getStatus())) {
            throw new IllegalStateException("Interview session was abandoned.");
        }

        InterviewQuestion question = interviewQuestionRepository.findByIdAndSession(questionId, session)
                .orElseThrow(() -> new ResourceNotFoundException("InterviewQuestion", "id", questionId));

        if (question.getUserAnswer() != null) {
            throw new IllegalStateException("This question has already been answered and evaluated.");
        }

        // 1. Evaluate answer via Gemini
        log.info("Evaluating answer for session ID {}, question {} ({})",
                sessionId, question.getQuestionNumber(), question.getCategory());
        InterviewEvaluationDto evalDto = geminiService.evaluateInterviewAnswer(
                question.getQuestionText(),
                question.getExpectedTopicsJson(),
                answer.trim(),
                session.getTargetRole(),
                question.getDifficulty(),
                question.getCategory()
        );

        // 2. Persist evaluation on Question
        question.setUserAnswer(answer.trim());
        question.setScore(evalDto.getScore());
        question.setTechnicalScore(evalDto.getTechnicalScore());
        question.setRelevanceScore(evalDto.getRelevanceScore());
        question.setCommunicationScore(evalDto.getCommunicationScore());
        question.setConfidenceScore(evalDto.getConfidenceScore());
        question.setAiFeedback(evalDto.getFeedback());
        question.setStrengthsJson(toJson(evalDto.getStrengths()));
        question.setImprovementsJson(toJson(evalDto.getImprovements()));
        question.setIdealAnswer(evalDto.getIdealAnswer());
        question.setAnsweredAt(LocalDateTime.now());
        question = interviewQuestionRepository.save(question);

        // 3. Update Session completed count
        int completedCount = session.getCompletedQuestions() + 1;
        session.setCompletedQuestions(completedCount);

        InterviewQuestionResponse nextQuestionResponse = null;
        boolean isComplete = false;
        InterviewSessionResponse finalSessionResult = null;

        if (completedCount < session.getTotalQuestions()) {
            // 4. Generate next question
            int nextQNum = completedCount + 1;
            List<InterviewQuestion> allQuestions = interviewQuestionRepository.findBySessionOrderByQuestionNumberAsc(session);
            List<String> previousQuestionTexts = allQuestions.stream().map(InterviewQuestion::getQuestionText).toList();

            Profile profile = profileRepository.findByUser(user).orElse(null);
            String candidateProfileJson = buildCandidateProfileJson(user, profile, session.getTargetRole());
            String jobDetailsJson = buildJobDetailsJson(session.getTargetJob());

            InterviewQuestionDto nextQDto = geminiService.generateInterviewQuestion(
                    candidateProfileJson,
                    session.getTargetRole(),
                    jobDetailsJson,
                    session.getInterviewType(),
                    session.getDifficulty(),
                    nextQNum,
                    session.getTotalQuestions(),
                    previousQuestionTexts
            );

            InterviewQuestion nextQuestion = InterviewQuestion.builder()
                    .session(session)
                    .questionNumber(nextQNum)
                    .questionText(nextQDto.getQuestion())
                    .category(nextQDto.getCategory() != null ? nextQDto.getCategory().toUpperCase() : "TECHNICAL")
                    .expectedTopicsJson(toJson(nextQDto.getExpectedTopics()))
                    .difficulty(nextQDto.getDifficulty() != null ? nextQDto.getDifficulty().toUpperCase() : session.getDifficulty())
                    .build();

            nextQuestion = interviewQuestionRepository.save(nextQuestion);
            session.addQuestion(nextQuestion);
            nextQuestionResponse = toQuestionResponse(nextQuestion, session.getTotalQuestions());
        } else {
            // 5. Final Evaluation & Completion
            isComplete = true;
            List<InterviewQuestion> allQuestions = interviewQuestionRepository.findBySessionOrderByQuestionNumberAsc(session);

            // Compute exact average scores from actual questions
            int avgOverall = (int) Math.round(allQuestions.stream().mapToInt(q -> q.getScore() != null ? q.getScore() : 0).average().orElse(0));
            int avgTech = (int) Math.round(allQuestions.stream().mapToInt(q -> q.getTechnicalScore() != null ? q.getTechnicalScore() : 0).average().orElse(0));
            int avgRelevance = (int) Math.round(allQuestions.stream().mapToInt(q -> q.getRelevanceScore() != null ? q.getRelevanceScore() : 0).average().orElse(0));
            int avgComm = (int) Math.round(allQuestions.stream().mapToInt(q -> q.getCommunicationScore() != null ? q.getCommunicationScore() : 0).average().orElse(0));
            int avgConf = (int) Math.round(allQuestions.stream().mapToInt(q -> q.getConfidenceScore() != null ? q.getConfidenceScore() : 0).average().orElse(0));

            // Summary data for Gemini
            List<Map<String, Object>> summaryList = new ArrayList<>();
            for (InterviewQuestion q : allQuestions) {
                Map<String, Object> qMap = new HashMap<>();
                qMap.put("questionNumber", q.getQuestionNumber());
                qMap.put("question", q.getQuestionText());
                qMap.put("category", q.getCategory());
                qMap.put("score", q.getScore());
                qMap.put("feedback", q.getAiFeedback());
                summaryList.add(qMap);
            }

            Profile profile = profileRepository.findByUser(user).orElse(null);
            String candidateProfileJson = buildCandidateProfileJson(user, profile, session.getTargetRole());

            InterviewFinalEvaluationDto finalEval = geminiService.generateFinalInterviewEvaluation(
                    session.getTargetRole(), session.getDifficulty(), candidateProfileJson, summaryList);

            session.setOverallScore(avgOverall);
            session.setTechnicalScore(avgTech);
            session.setProblemSolvingScore(avgRelevance);
            session.setCommunicationScore(avgComm);
            session.setConfidenceScore(avgConf);
            session.setAiSummary(finalEval.getSummary());
            session.setStrengthsJson(toJson(finalEval.getStrengths()));
            session.setWeaknessesJson(toJson(finalEval.getWeaknesses()));
            session.setRecommendationsJson(toJson(finalEval.getRecommendations()));
            session.setStatus("COMPLETED");
            session.setCompletedAt(LocalDateTime.now());

            finalSessionResult = toSessionResponse(session);
        }

        session = interviewSessionRepository.save(session);

        return InterviewEvaluationResponse.builder()
                .questionId(question.getId())
                .questionNumber(question.getQuestionNumber())
                .score(question.getScore())
                .technicalScore(question.getTechnicalScore())
                .relevanceScore(question.getRelevanceScore())
                .communicationScore(question.getCommunicationScore())
                .confidenceScore(question.getConfidenceScore())
                .feedback(question.getAiFeedback())
                .strengths(fromJson(question.getStrengthsJson(), new TypeReference<List<String>>() {}, Collections.emptyList()))
                .improvements(fromJson(question.getImprovementsJson(), new TypeReference<List<String>>() {}, Collections.emptyList()))
                .idealAnswer(question.getIdealAnswer())
                .nextQuestion(nextQuestionResponse)
                .isComplete(isComplete)
                .finalSessionResult(finalSessionResult)
                .build();
    }

    @Transactional(readOnly = true)
    public InterviewQuestionResponse getNextQuestion(Long sessionId, String userEmail) {
        User user = findUserByEmail(userEmail);
        InterviewSession session = interviewSessionRepository.findByIdAndUser(sessionId, user)
                .orElseThrow(() -> new ResourceNotFoundException("InterviewSession", "id", sessionId));

        if ("COMPLETED".equalsIgnoreCase(session.getStatus())) {
            throw new IllegalStateException("Interview session is already completed.");
        }

        List<InterviewQuestion> questions = interviewQuestionRepository.findBySessionOrderByQuestionNumberAsc(session);
        InterviewQuestion active = questions.stream()
                .filter(q -> q.getUserAnswer() == null)
                .findFirst()
                .orElse(null);

        if (active == null) {
            throw new ResourceNotFoundException("InterviewQuestion", "sessionId", sessionId);
        }

        return toQuestionResponse(active, session.getTotalQuestions());
    }

    @Transactional(readOnly = true)
    public InterviewSessionResponse getInterview(Long sessionId, String userEmail) {
        User user = findUserByEmail(userEmail);
        InterviewSession session = interviewSessionRepository.findByIdAndUser(sessionId, user)
                .orElseThrow(() -> new ResourceNotFoundException("InterviewSession", "id", sessionId));
        return toSessionResponse(session);
    }

    @Transactional(readOnly = true)
    public List<InterviewHistoryResponse> getInterviewHistory(String userEmail) {
        User user = findUserByEmail(userEmail);
        List<InterviewSession> sessions = interviewSessionRepository.findByUserOrderByCreatedAtDesc(user);
        return sessions.stream().map(s -> InterviewHistoryResponse.builder()
                .id(s.getId())
                .targetRole(s.getTargetRole())
                .targetJobTitle(s.getTargetJob() != null ? s.getTargetJob().getTitle() : null)
                .interviewType(s.getInterviewType())
                .difficulty(s.getDifficulty())
                .overallScore(s.getOverallScore())
                .technicalScore(s.getTechnicalScore())
                .communicationScore(s.getCommunicationScore())
                .totalQuestions(s.getTotalQuestions())
                .completedQuestions(s.getCompletedQuestions())
                .status(s.getStatus())
                .createdAt(s.getCreatedAt())
                .completedAt(s.getCompletedAt())
                .build()).toList();
    }

    @Transactional
    public void abandonInterview(Long sessionId, String userEmail) {
        User user = findUserByEmail(userEmail);
        InterviewSession session = interviewSessionRepository.findByIdAndUser(sessionId, user)
                .orElseThrow(() -> new ResourceNotFoundException("InterviewSession", "id", sessionId));
        session.setStatus("ABANDONED");
        interviewSessionRepository.save(session);
        log.info("User {} abandoned interview session ID {}", userEmail, sessionId);
    }

    private String buildCandidateProfileJson(User user, Profile profile, String targetRole) {
        Resume resume = resumeRepository.findFirstByUserOrderByUploadedAtDesc(user).orElse(null);
        ResumeAnalysis resumeAnalysis = (resume != null) ? resumeAnalysisRepository.findByResume(resume).orElse(null) : null;
        SkillGapAnalysis gapAnalysis = skillGapAnalysisRepository.findFirstByUserOrderByAnalyzedAtDesc(user).orElse(null);

        Map<String, Object> map = new HashMap<>();
        map.put("targetRole", targetRole);

        List<String> skills = new ArrayList<>();
        if (resumeAnalysis != null) {
            List<DetectedSkillDto> detected = fromJson(resumeAnalysis.getDetectedSkillsJson(),
                    new TypeReference<List<DetectedSkillDto>>() {}, Collections.emptyList());
            skills.addAll(detected.stream().map(DetectedSkillDto::getName).filter(Objects::nonNull).toList());
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
            skills.add("Core Software Engineering");
        }

        map.put("candidateSkills", skills);

        if (gapAnalysis != null) {
            List<String> missing = fromJson(gapAnalysis.getMissingSkillsJson(), new TypeReference<List<String>>() {}, Collections.emptyList());
            map.put("knownSkillGaps", missing);
        }

        return toJson(map);
    }

    private String buildJobDetailsJson(Job job) {
        if (job == null) return "None (general role interview).";
        Map<String, Object> map = new HashMap<>();
        map.put("title", job.getTitle());
        map.put("company", job.getCompany());
        map.put("requiredSkills", job.getRequiredSkills());
        map.put("preferredSkills", job.getPreferredSkills());
        map.put("experienceLevel", job.getExperienceLevel());
        map.put("description", job.getDescription());
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

    public InterviewQuestionResponse toQuestionResponse(InterviewQuestion q, Integer totalQuestions) {
        return InterviewQuestionResponse.builder()
                .id(q.getId())
                .sessionId(q.getSession() != null ? q.getSession().getId() : null)
                .questionNumber(q.getQuestionNumber())
                .questionText(q.getQuestionText())
                .category(q.getCategory())
                .difficulty(q.getDifficulty())
                .totalQuestions(totalQuestions)
                .answered(q.getUserAnswer() != null)
                .build();
    }

    public InterviewSessionResponse toSessionResponse(InterviewSession session) {
        List<InterviewQuestion> qEntities = (session.getQuestions() != null && !session.getQuestions().isEmpty())
                ? session.getQuestions()
                : interviewQuestionRepository.findBySessionOrderByQuestionNumberAsc(session);

        List<InterviewSessionResponse.QuestionDetailDto> qDtos = qEntities.stream().map(q ->
                InterviewSessionResponse.QuestionDetailDto.builder()
                        .id(q.getId())
                        .questionNumber(q.getQuestionNumber())
                        .questionText(q.getQuestionText())
                        .category(q.getCategory())
                        .difficulty(q.getDifficulty())
                        .userAnswer(q.getUserAnswer())
                        .score(q.getScore())
                        .technicalScore(q.getTechnicalScore())
                        .relevanceScore(q.getRelevanceScore())
                        .communicationScore(q.getCommunicationScore())
                        .confidenceScore(q.getConfidenceScore())
                        .aiFeedback(q.getAiFeedback())
                        .strengths(fromJson(q.getStrengthsJson(), new TypeReference<List<String>>() {}, Collections.emptyList()))
                        .improvements(fromJson(q.getImprovementsJson(), new TypeReference<List<String>>() {}, Collections.emptyList()))
                        .idealAnswer(q.getIdealAnswer())
                        .answeredAt(q.getAnsweredAt())
                        .build()
        ).toList();

        return InterviewSessionResponse.builder()
                .id(session.getId())
                .targetRole(session.getTargetRole())
                .targetJobId(session.getTargetJob() != null ? session.getTargetJob().getId() : null)
                .targetJobTitle(session.getTargetJob() != null ? session.getTargetJob().getTitle() : null)
                .targetJobCompany(session.getTargetJob() != null ? session.getTargetJob().getCompany() : null)
                .interviewType(session.getInterviewType())
                .difficulty(session.getDifficulty())
                .totalQuestions(session.getTotalQuestions())
                .completedQuestions(session.getCompletedQuestions())
                .overallScore(session.getOverallScore())
                .technicalScore(session.getTechnicalScore())
                .communicationScore(session.getCommunicationScore())
                .confidenceScore(session.getConfidenceScore())
                .problemSolvingScore(session.getProblemSolvingScore())
                .status(session.getStatus())
                .aiSummary(session.getAiSummary())
                .strengths(fromJson(session.getStrengthsJson(), new TypeReference<List<String>>() {}, Collections.emptyList()))
                .weaknesses(fromJson(session.getWeaknessesJson(), new TypeReference<List<String>>() {}, Collections.emptyList()))
                .recommendations(fromJson(session.getRecommendationsJson(), new TypeReference<List<String>>() {}, Collections.emptyList()))
                .startedAt(session.getStartedAt())
                .completedAt(session.getCompletedAt())
                .questions(qDtos)
                .build();
    }
}
