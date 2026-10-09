package com.careerpilot.app.service;

import com.careerpilot.app.dto.AssistantConversationDto;
import com.careerpilot.app.dto.AssistantMessageDto;
import com.careerpilot.app.dto.CareerAssistantContext;
import com.careerpilot.app.dto.CareerAssistantDto;
import com.careerpilot.app.dto.request.CareerAssistantRequest;
import com.careerpilot.app.dto.response.CareerAssistantResponse;
import com.careerpilot.app.dto.response.CareerChatMessageDto;
import com.careerpilot.app.dto.response.CareerChatResponse;
import com.careerpilot.app.entity.*;
import com.careerpilot.app.exception.ChatNotFoundException;
import com.careerpilot.app.exception.ResourceNotFoundException;
import com.careerpilot.app.exception.UnauthorizedChatAccessException;
import com.careerpilot.app.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CareerAssistantService {

    private final UserRepository userRepository;
    private final AssistantConversationRepository assistantConversationRepository;
    private final AssistantMessageRepository assistantMessageRepository;
    private final ProfileRepository profileRepository;
    private final ResumeAnalysisRepository resumeAnalysisRepository;
    private final SkillGapAnalysisRepository skillGapAnalysisRepository;
    private final JobMatchRepository jobMatchRepository;
    private final SavedJobRepository savedJobRepository;
    private final CareerRoadmapRepository careerRoadmapRepository;
    private final JobPreparationRepository jobPreparationRepository;
    private final InterviewSessionRepository interviewSessionRepository;
    private final CareerAnalyticsRepository careerAnalyticsRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;

    /**
     * Sends a user message to the context-aware AI Career Assistant and records the AI response.
     */
    @Transactional
    public CareerAssistantResponse sendMessage(String userEmail, Long conversationId, String message) {
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Message content cannot be empty.");
        }

        User user = getUserByEmail(userEmail);
        AssistantConversation conversation;
        boolean isNewConversation = false;

        if (conversationId != null) {
            conversation = assistantConversationRepository.findById(conversationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with ID: " + conversationId));

            if (!conversation.getUser().getId().equals(user.getId())) {
                throw new UnauthorizedChatAccessException("You do not have permission to access this conversation.");
            }
        } else {
            isNewConversation = true;
            String generatedTitle = generateChatTitle(message);
            conversation = AssistantConversation.builder()
                    .user(user)
                    .title(generatedTitle)
                    .build();
            conversation = assistantConversationRepository.save(conversation);
        }

        // 1. Build comprehensive, strictly authenticated candidate context
        CareerAssistantContext contextObj = buildCareerAssistantContext(user);
        String careerContextJson = toJson(contextObj);

        // 2. Load recent conversation history (last 10 messages)
        String conversationHistoryJson = buildConversationHistory(conversation);

        // 3. Request AI Career guidance from Gemini
        log.info("Requesting context-aware AI Career Assistant guidance for user: {} (conversation: {})", userEmail, conversation.getId());
        CareerAssistantDto aiDto = geminiService.generateCareerAssistantResponse(
                careerContextJson,
                conversationHistoryJson,
                message
        );

        // 4. Save USER message
        AssistantMessage userMsg = AssistantMessage.builder()
                .conversation(conversation)
                .role("USER")
                .content(message)
                .build();
        assistantMessageRepository.save(userMsg);

        // 5. Save ASSISTANT message
        AssistantMessage assistantMsg = AssistantMessage.builder()
                .conversation(conversation)
                .role("ASSISTANT")
                .content(aiDto.getEffectiveMessage())
                .suggestedActionsJson(toJson(aiDto.getSuggestedActions()))
                .contextUsedJson(toJson(aiDto.getContextUsed()))
                .build();
        assistantMessageRepository.save(assistantMsg);

        // 6. Update conversation title & timestamp
        if (isNewConversation || conversation.getTitle() == null || conversation.getTitle().equals("New Conversation")) {
            conversation.setTitle(generateChatTitle(message));
        }
        conversation.setUpdatedAt(LocalDateTime.now());
        assistantConversationRepository.save(conversation);

        return CareerAssistantResponse.builder()
                .conversationId(conversation.getId())
                .chatId(conversation.getId())
                .chatTitle(conversation.getTitle())
                .message(aiDto.getEffectiveMessage())
                .answer(aiDto.getEffectiveMessage())
                .suggestedActions(aiDto.getSuggestedActions() != null ? aiDto.getSuggestedActions() : Collections.emptyList())
                .contextUsed(aiDto.getContextUsed() != null ? aiDto.getContextUsed() : Collections.emptyList())
                .relevantSkills(aiDto.getRelevantSkills() != null ? aiDto.getRelevantSkills() : Collections.emptyList())
                .relevantJobs(aiDto.getRelevantJobs() != null ? aiDto.getRelevantJobs() : Collections.emptyList())
                .preparationSuggestions(aiDto.getPreparationSuggestions() != null ? aiDto.getPreparationSuggestions() : Collections.emptyList())
                .rateLimited(false)
                .createdAt(assistantMsg.getCreatedAt() != null ? assistantMsg.getCreatedAt() : LocalDateTime.now())
                .build();
    }

    /**
     * Overload for backwards compatibility with CareerAssistantRequest.
     */
    @Transactional
    public CareerAssistantResponse sendMessage(String userEmail, CareerAssistantRequest request) {
        Long convId = request.getChatId();
        return sendMessage(userEmail, convId, request.getMessage());
    }

    /**
     * Fast single-shot quick question endpoint.
     */
    @Transactional
    public CareerAssistantResponse askQuickQuestion(String userEmail, String message) {
        User user = getUserByEmail(userEmail);
        List<AssistantConversation> convs = assistantConversationRepository.findByUserOrderByUpdatedAtDesc(user);
        Long targetId = !convs.isEmpty() ? convs.get(0).getId() : null;
        return sendMessage(userEmail, targetId, message);
    }

    /**
     * Lists all conversations belonging strictly to the authenticated user.
     */
    @Transactional(readOnly = true)
    public List<AssistantConversationDto> getConversations(String userEmail) {
        User user = getUserByEmail(userEmail);
        List<AssistantConversation> convs = assistantConversationRepository.findByUserOrderByUpdatedAtDesc(user);
        return convs.stream().map(this::mapConversationToDto).collect(Collectors.toList());
    }

    /**
     * Retrieves a single conversation and its ordered messages with strict user isolation.
     */
    @Transactional(readOnly = true)
    public AssistantConversationDto getConversation(Long conversationId, String userEmail) {
        User user = getUserByEmail(userEmail);
        AssistantConversation conv = assistantConversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with ID: " + conversationId));

        if (!conv.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedChatAccessException("You do not have permission to access this conversation.");
        }

        List<AssistantMessage> messages = assistantMessageRepository.findByConversationOrderByCreatedAtAsc(conv);
        List<AssistantMessageDto> messageDtos = messages.stream().map(this::mapMessageToDto).collect(Collectors.toList());

        return AssistantConversationDto.builder()
                .id(conv.getId())
                .title(conv.getTitle())
                .messages(messageDtos)
                .createdAt(conv.getCreatedAt())
                .updatedAt(conv.getUpdatedAt())
                .build();
    }

    /**
     * Creates a new conversation for the authenticated user.
     */
    @Transactional
    public AssistantConversationDto createConversation(String userEmail, String initialTitle) {
        User user = getUserByEmail(userEmail);
        String title = (initialTitle != null && !initialTitle.isBlank()) ? initialTitle.trim() : "New Conversation";
        AssistantConversation conv = AssistantConversation.builder()
                .user(user)
                .title(title)
                .build();
        conv = assistantConversationRepository.save(conv);

        return AssistantConversationDto.builder()
                .id(conv.getId())
                .title(conv.getTitle())
                .messages(Collections.emptyList())
                .createdAt(conv.getCreatedAt())
                .updatedAt(conv.getUpdatedAt())
                .build();
    }

    /**
     * Deletes a conversation and its messages with strict user isolation.
     */
    @Transactional
    public void deleteConversation(Long conversationId, String userEmail) {
        User user = getUserByEmail(userEmail);
        AssistantConversation conv = assistantConversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with ID: " + conversationId));

        if (!conv.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedChatAccessException("You do not have permission to delete this conversation.");
        }

        assistantConversationRepository.delete(conv);
        log.info("User {} deleted conversation ID {}", userEmail, conversationId);
    }

    // =========================================================================
    // Legacy Chat Adapters for Backwards Compatibility
    // =========================================================================

    @Transactional(readOnly = true)
    public List<CareerChatResponse> getChats(String userEmail) {
        return getConversations(userEmail).stream().map(dto -> CareerChatResponse.builder()
                .id(dto.getId())
                .title(dto.getTitle())
                .messages(Collections.emptyList())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .build()).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CareerChatResponse getChat(Long chatId, String userEmail) {
        AssistantConversationDto dto = getConversation(chatId, userEmail);
        List<CareerChatMessageDto> chatMessages = dto.getMessages().stream().map(m -> CareerChatMessageDto.builder()
                .id(m.getId())
                .role(m.getRole())
                .message(m.getContent())
                .suggestedActions(m.getSuggestedActions() != null ? m.getSuggestedActions().stream().map(CareerAssistantDto.ActionItem::getLabel).collect(Collectors.toList()) : Collections.emptyList())
                .createdAt(m.getCreatedAt())
                .build()).collect(Collectors.toList());

        return CareerChatResponse.builder()
                .id(dto.getId())
                .title(dto.getTitle())
                .messages(chatMessages)
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .build();
    }

    @Transactional
    public CareerChatResponse createChat(String userEmail, String initialTitle) {
        AssistantConversationDto dto = createConversation(userEmail, initialTitle);
        return CareerChatResponse.builder()
                .id(dto.getId())
                .title(dto.getTitle())
                .messages(Collections.emptyList())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .build();
    }

    @Transactional
    public void deleteChat(Long chatId, String userEmail) {
        deleteConversation(chatId, userEmail);
    }

    // =========================================================================
    // Context Aggregation & Helpers
    // =========================================================================

    public CareerAssistantContext buildCareerAssistantContext(User user) {
        List<String> activeSources = new ArrayList<>();

        // 1. Profile
        Map<String, Object> pMap = new LinkedHashMap<>();
        profileRepository.findByUser(user).ifPresent(profile -> {
            pMap.put("targetRole", profile.getTargetRole());
            pMap.put("experienceLevel", profile.getExperienceLevel());
            pMap.put("degree", profile.getDegree());
            pMap.put("branch", profile.getBranch());
            pMap.put("college", profile.getCollege());
            pMap.put("skillsSummary", profile.getSkillsSummary());
            pMap.put("preferredJobType", profile.getPreferredJobType());
            activeSources.add("profile");
        });

        // 2. Latest Resume Analysis
        Map<String, Object> rMap = new LinkedHashMap<>();
        resumeAnalysisRepository.findByResumeUserOrderByIdDesc(user).stream().findFirst().ifPresent(resume -> {
            rMap.put("overallScore", resume.getOverallScore());
            rMap.put("atsScore", resume.getAtsScore());
            rMap.put("detectedSkills", fromJson(resume.getDetectedSkillsJson(), new TypeReference<List<String>>() {}, Collections.emptyList()));
            rMap.put("missingSkills", fromJson(resume.getMissingSkillsJson(), new TypeReference<List<String>>() {}, Collections.emptyList()));
            rMap.put("strengths", fromJson(resume.getStrengthsJson(), new TypeReference<List<String>>() {}, Collections.emptyList()));
            rMap.put("weaknesses", fromJson(resume.getWeaknessesJson(), new TypeReference<List<String>>() {}, Collections.emptyList()));
            rMap.put("suggestions", fromJson(resume.getSuggestionsJson(), new TypeReference<List<String>>() {}, Collections.emptyList()));
            activeSources.add("resume");
        });

        // 3. Skill Gap Analysis
        Map<String, Object> sgMap = new LinkedHashMap<>();
        skillGapAnalysisRepository.findFirstByUserOrderByAnalyzedAtDesc(user).ifPresent(sg -> {
            sgMap.put("targetRole", sg.getTargetRole());
            sgMap.put("readinessScore", sg.getReadinessScore());
            sgMap.put("skillCoverageScore", sg.getSkillCoverageScore());
            sgMap.put("currentSkills", fromJson(sg.getCurrentSkillsJson(), new TypeReference<List<String>>() {}, Collections.emptyList()));
            sgMap.put("missingSkills", fromJson(sg.getMissingSkillsJson(), new TypeReference<List<String>>() {}, Collections.emptyList()));
            sgMap.put("skillGaps", fromJson(sg.getSkillGapsJson(), new TypeReference<List<Object>>() {}, Collections.emptyList()));
            activeSources.add("skillGap");
        });

        // 4. Job Matches & Saved Jobs
        List<Map<String, Object>> topMatches = new ArrayList<>();
        List<JobMatch> matches = jobMatchRepository.findByUserOrderByOverallScoreDesc(user);
        if (!matches.isEmpty()) {
            topMatches = matches.stream().limit(4).map(m -> {
                Map<String, Object> jm = new LinkedHashMap<>();
                jm.put("jobTitle", m.getJob() != null ? m.getJob().getTitle() : "Unknown");
                jm.put("company", m.getJob() != null ? m.getJob().getCompany() : "Unknown");
                jm.put("overallScore", m.getOverallScore());
                jm.put("recommendation", m.getRecommendation());
                jm.put("matchingSkills", fromJson(m.getMatchingSkillsJson(), new TypeReference<List<String>>() {}, Collections.emptyList()));
                jm.put("missingSkills", fromJson(m.getMissingSkillsJson(), new TypeReference<List<String>>() {}, Collections.emptyList()));
                return jm;
            }).collect(Collectors.toList());
            activeSources.add("jobs");
        }

        List<Map<String, Object>> savedJobsList = new ArrayList<>();
        List<SavedJob> saved = savedJobRepository.findByUserOrderByCreatedAtDesc(user);
        if (!saved.isEmpty()) {
            savedJobsList = saved.stream().limit(4).map(s -> {
                Map<String, Object> sj = new LinkedHashMap<>();
                sj.put("jobTitle", s.getJob() != null ? s.getJob().getTitle() : "Unknown");
                sj.put("company", s.getJob() != null ? s.getJob().getCompany() : "Unknown");
                return sj;
            }).collect(Collectors.toList());
        }

        // 5. Career Roadmap
        Map<String, Object> rmMap = new LinkedHashMap<>();
        careerRoadmapRepository.findTopByUserOrderByUpdatedAtDesc(user).ifPresent(roadmap -> {
            rmMap.put("targetRole", roadmap.getTargetRole());
            rmMap.put("currentLevel", roadmap.getCurrentLevel());
            rmMap.put("overallReadinessScore", roadmap.getOverallReadinessScore());
            rmMap.put("overallProgress", roadmap.getOverallProgress());
            rmMap.put("estimatedDurationWeeks", roadmap.getEstimatedDurationWeeks());
            rmMap.put("summary", roadmap.getSummary());
            activeSources.add("roadmap");
        });

        // 6. Mock Interview History
        Map<String, Object> miMap = new LinkedHashMap<>();
        List<InterviewSession> sessions = interviewSessionRepository.findByUserOrderByCreatedAtDesc(user);
        if (!sessions.isEmpty()) {
            double totalOverall = 0;
            double totalTech = 0;
            double totalComm = 0;
            int count = 0;
            for (InterviewSession s : sessions) {
                if (s.getOverallScore() != null) {
                    totalOverall += s.getOverallScore();
                    totalTech += (s.getTechnicalScore() != null ? s.getTechnicalScore() : s.getOverallScore());
                    totalComm += (s.getCommunicationScore() != null ? s.getCommunicationScore() : s.getOverallScore());
                    count++;
                }
            }
            miMap.put("totalInterviews", sessions.size());
            miMap.put("averageOverallScore", count > 0 ? (totalOverall / count) : 0);
            miMap.put("averageTechnicalScore", count > 0 ? (totalTech / count) : 0);
            miMap.put("averageCommunicationScore", count > 0 ? (totalComm / count) : 0);

            InterviewSession latest = sessions.get(0);
            miMap.put("latestRole", latest.getTargetRole());
            miMap.put("latestScore", latest.getOverallScore());
            miMap.put("latestStatus", latest.getStatus());
            activeSources.add("mockInterviews");
        }

        // 7. Job Preparations
        List<Map<String, Object>> prepSummaries = new ArrayList<>();
        List<JobPreparation> preps = jobPreparationRepository.findByUserOrderByUpdatedAtDesc(user);
        if (!preps.isEmpty()) {
            prepSummaries = preps.stream().limit(3).map(p -> {
                Map<String, Object> jp = new LinkedHashMap<>();
                jp.put("jobTitle", p.getJob() != null ? p.getJob().getTitle() : "Unknown");
                jp.put("company", p.getJob() != null ? p.getJob().getCompany() : "Unknown");
                jp.put("readinessScore", p.getReadinessScore());
                jp.put("preparationLevel", p.getPreparationLevel());
                return jp;
            }).collect(Collectors.toList());
            activeSources.add("jobPrep");
        }

        // 8. Career Analytics
        Map<String, Object> caMap = new LinkedHashMap<>();
        careerAnalyticsRepository.findByUser(user).ifPresent(ca -> {
            caMap.put("overallReadinessScore", ca.getOverallReadinessScore());
            caMap.put("previousReadinessScore", ca.getPreviousReadinessScore());
            caMap.put("readinessLevel", ca.getReadinessLevel());
            caMap.put("careerSummary", ca.getCareerSummary());
            caMap.put("strengths", fromJson(ca.getStrengthsJson(), new TypeReference<List<String>>() {}, Collections.emptyList()));
            caMap.put("weaknesses", fromJson(ca.getWeaknessesJson(), new TypeReference<List<Object>>() {}, Collections.emptyList()));
            caMap.put("recommendations", fromJson(ca.getRecommendationsJson(), new TypeReference<List<Object>>() {}, Collections.emptyList()));
            caMap.put("weeklyGoals", fromJson(ca.getWeeklyGoalsJson(), new TypeReference<List<Object>>() {}, Collections.emptyList()));
            activeSources.add("careerAnalytics");
        });

        // 9. Job Applications Tracker
        Map<String, Object> appMap = new LinkedHashMap<>();
        List<JobApplication> apps = jobApplicationRepository.findByUserOrderByUpdatedAtDesc(user);
        if (!apps.isEmpty()) {
            appMap.put("totalTracked", apps.size());
            List<Map<String, Object>> recentApps = apps.stream().limit(5).map(a -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("company", a.getCompany());
                item.put("jobTitle", a.getJobTitle());
                item.put("status", a.getStatus().name());
                item.put("applicationDate", a.getApplicationDate());
                item.put("deadline", a.getDeadline());
                item.put("nextFollowUpDate", a.getNextFollowUpDate());
                item.put("interviewDate", a.getInterviewDate());
                return item;
            }).collect(Collectors.toList());
            appMap.put("recentApplications", recentApps);
            activeSources.add("jobApplications");
        }

        return CareerAssistantContext.builder()
                .profile(pMap)
                .resumeAnalysis(rMap)
                .skillGapAnalysis(sgMap)
                .topJobMatches(topMatches)
                .savedJobs(savedJobsList)
                .careerRoadmap(rmMap)
                .mockInterviewSummary(miMap)
                .jobPreparations(prepSummaries)
                .careerAnalytics(caMap)
                .jobApplications(appMap)
                .activeContextSources(activeSources)
                .build();
    }

    private String buildConversationHistory(AssistantConversation conversation) {
        List<AssistantMessage> recent = assistantMessageRepository.findTop10ByConversationOrderByCreatedAtDesc(conversation);
        Collections.reverse(recent);

        List<Map<String, String>> history = recent.stream().map(m -> {
            Map<String, String> entry = new LinkedHashMap<>();
            entry.put("role", m.getRole());
            entry.put("content", m.getContent());
            return entry;
        }).collect(Collectors.toList());

        return toJson(history);
    }

    private String generateChatTitle(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return "Career Guidance";
        }
        String clean = userMessage.trim().replaceAll("\\s+", " ");
        if (clean.length() <= 45) {
            return clean;
        }
        return clean.substring(0, 42) + "...";
    }

    private AssistantConversationDto mapConversationToDto(AssistantConversation conv) {
        return AssistantConversationDto.builder()
                .id(conv.getId())
                .title(conv.getTitle())
                .messages(Collections.emptyList())
                .createdAt(conv.getCreatedAt())
                .updatedAt(conv.getUpdatedAt())
                .build();
    }

    private AssistantMessageDto mapMessageToDto(AssistantMessage message) {
        List<CareerAssistantDto.ActionItem> actions = fromJson(
                message.getSuggestedActionsJson(),
                new TypeReference<List<CareerAssistantDto.ActionItem>>() {},
                Collections.emptyList()
        );
        List<String> contextUsed = fromJson(
                message.getContextUsedJson(),
                new TypeReference<List<String>>() {},
                Collections.emptyList()
        );

        return AssistantMessageDto.builder()
                .id(message.getId())
                .role(message.getRole())
                .content(message.getContent())
                .message(message.getContent())
                .suggestedActions(actions != null ? actions : Collections.emptyList())
                .contextUsed(contextUsed != null ? contextUsed : Collections.emptyList())
                .createdAt(message.getCreatedAt())
                .build();
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    private String toJson(Object obj) {
        if (obj == null) return "[]";
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("JSON serialization error in CareerAssistantService: {}", e.getMessage());
            return "[]";
        }
    }

    private <T> T fromJson(String json, TypeReference<T> typeRef, T defaultValue) {
        if (json == null || json.trim().isEmpty() || "null".equalsIgnoreCase(json.trim())) return defaultValue;
        try {
            T val = objectMapper.readValue(json, typeRef);
            return val != null ? val : defaultValue;
        } catch (Exception e) {
            log.error("JSON deserialization error in CareerAssistantService: {}", e.getMessage());
            return defaultValue;
        }
    }
}
