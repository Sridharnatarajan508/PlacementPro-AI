package com.careerpilot.app.assistant;

import com.careerpilot.app.dto.AssistantConversationDto;
import com.careerpilot.app.dto.CareerAssistantContext;
import com.careerpilot.app.dto.CareerAssistantDto;
import com.careerpilot.app.dto.request.RegisterRequest;
import com.careerpilot.app.dto.response.CareerAssistantResponse;
import com.careerpilot.app.entity.*;
import com.careerpilot.app.exception.AIServiceException;
import com.careerpilot.app.exception.GeminiRateLimitException;
import com.careerpilot.app.exception.ResourceNotFoundException;
import com.careerpilot.app.exception.UnauthorizedChatAccessException;
import com.careerpilot.app.repository.*;
import com.careerpilot.app.service.AuthService;
import com.careerpilot.app.service.CareerAssistantService;
import com.careerpilot.app.service.GeminiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:assistanttestdb;MODE=MySQL;DB_CLOSE_DELAY=-1;CASE_INSENSITIVE_IDENTIFIERS=TRUE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "app.jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
    "app.jwt.expiration=86400000",
    "app.resume.upload-dir=target/test-assistant-uploads"
})
@Transactional
public class CareerAssistantServiceTest {

    @Autowired
    private CareerAssistantService careerAssistantService;

    @Autowired
    private AssistantConversationRepository assistantConversationRepository;

    @Autowired
    private AssistantMessageRepository assistantMessageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private AuthService authService;

    @MockBean
    private GeminiService geminiService;

    private String user1Email = "asstuser1@example.com";
    private String user2Email = "asstuser2@example.com";

    private CareerAssistantDto mockDto;

    @BeforeEach
    public void setup() {
        if (userRepository.findByEmail(user1Email).isEmpty()) {
            authService.register(new RegisterRequest("Assistant User One", user1Email, "Password123!"));
        }
        if (userRepository.findByEmail(user2Email).isEmpty()) {
            authService.register(new RegisterRequest("Assistant User Two", user2Email, "Password123!"));
        }

        User user1 = userRepository.findByEmail(user1Email).get();
        Profile p = profileRepository.findByUser(user1).orElse(Profile.builder().user(user1).build());
        p.setTargetRole("Full Stack Java Engineer");
        p.setExperienceLevel("MID_LEVEL");
        p.setSkillsSummary("Java, Spring Boot, React, MySQL");
        profileRepository.save(p);

        mockDto = CareerAssistantDto.builder()
                .message("Based on your background in Java and Spring Boot, focusing on system design and mock interviews will significantly boost your readiness.")
                .answer("Based on your background in Java and Spring Boot, focusing on system design and mock interviews will significantly boost your readiness.")
                .suggestedActions(List.of(
                        new CareerAssistantDto.ActionItem("Practice Mock Interview", "/mock-interview"),
                        new CareerAssistantDto.ActionItem("View Career Analytics", "/career-analytics")
                ))
                .contextUsed(List.of("profile", "careerAnalytics", "mockInterview"))
                .relevantSkills(List.of("Java", "Spring Boot", "System Design"))
                .relevantJobs(List.of("Senior Backend Engineer"))
                .preparationSuggestions(List.of("Review Distributed Caching with Redis"))
                .build();

        when(geminiService.generateCareerAssistantResponse(any(), any(), any()))
                .thenReturn(mockDto);
        when(geminiService.analyzeCareerAssistant(any(), any(), any()))
                .thenReturn(mockDto);
    }

    @Test
    public void testSuccessfulAIMessageGeneration() {
        CareerAssistantResponse response = careerAssistantService.sendMessage(user1Email, null, "What should I learn next?");

        assertNotNull(response);
        assertNotNull(response.getConversationId());
        assertTrue(response.getMessage().contains("Java and Spring Boot"));
        assertFalse(response.getSuggestedActions().isEmpty());
        assertEquals("/mock-interview", response.getSuggestedActions().get(0).getRoute());
        assertEquals(3, response.getContextUsed().size());
        assertFalse(response.getRateLimited());

        verify(geminiService, times(1))
                .generateCareerAssistantResponse(any(), any(), eq("What should I learn next?"));
    }

    @Test
    public void testNewConversationCreation() {
        AssistantConversationDto conv = careerAssistantService.createConversation(user1Email, "Interview Strategy");

        assertNotNull(conv);
        assertNotNull(conv.getId());
        assertEquals("Interview Strategy", conv.getTitle());
        assertTrue(conv.getMessages().isEmpty());
    }

    @Test
    public void testExistingConversationReuse() {
        AssistantConversationDto conv = careerAssistantService.createConversation(user1Email, "Roadmap Questions");

        CareerAssistantResponse resp1 = careerAssistantService.sendMessage(user1Email, conv.getId(), "First question");
        assertEquals(conv.getId(), resp1.getConversationId());

        CareerAssistantResponse resp2 = careerAssistantService.sendMessage(user1Email, conv.getId(), "Second follow-up question");
        assertEquals(conv.getId(), resp2.getConversationId());

        AssistantConversationDto retrieved = careerAssistantService.getConversation(conv.getId(), user1Email);
        assertEquals(4, retrieved.getMessages().size()); // 2 USER + 2 ASSISTANT messages
    }

    @Test
    public void testConversationHistoryLoading() {
        AssistantConversationDto conv = careerAssistantService.createConversation(user1Email, "Multi-Turn Chat");

        careerAssistantService.sendMessage(user1Email, conv.getId(), "Turn 1 question");
        careerAssistantService.sendMessage(user1Email, conv.getId(), "Turn 2 question");

        verify(geminiService, times(2)).generateCareerAssistantResponse(any(), any(), any());
    }

    @Test
    public void testConversationPersistence() {
        CareerAssistantResponse resp = careerAssistantService.sendMessage(user1Email, null, "Persist this please");
        Long convId = resp.getConversationId();

        AssistantConversation stored = assistantConversationRepository.findById(convId).orElse(null);
        assertNotNull(stored);
        assertEquals(user1Email, stored.getUser().getEmail());

        List<AssistantMessage> msgs = assistantMessageRepository.findByConversationOrderByCreatedAtAsc(stored);
        assertEquals(2, msgs.size());
        assertEquals("USER", msgs.get(0).getRole());
        assertEquals("ASSISTANT", msgs.get(1).getRole());
    }

    @Test
    public void testCachedContextDataLoading() {
        User user1 = userRepository.findByEmail(user1Email).get();
        CareerAssistantContext ctx = careerAssistantService.buildCareerAssistantContext(user1);

        assertNotNull(ctx);
        assertNotNull(ctx.getProfile());
        assertEquals("Full Stack Java Engineer", ctx.getProfile().get("targetRole"));
        assertTrue(ctx.getActiveContextSources().contains("profile"));
    }

    @Test
    public void testEmptyCareerProfileHandling() {
        String freshEmail = "freshcandidate@example.com";
        authService.register(new RegisterRequest("Fresh Candidate", freshEmail, "Password123!"));

        CareerAssistantResponse resp = careerAssistantService.sendMessage(freshEmail, null, "Hello Copilot!");

        assertNotNull(resp);
        assertNotNull(resp.getMessage());
    }

    @Test
    public void testMissingResumeHandling() {
        // user2 has no resume uploaded
        CareerAssistantResponse resp = careerAssistantService.sendMessage(user2Email, null, "Can you review my resume?");

        assertNotNull(resp);
        assertNotNull(resp.getMessage());
    }

    @Test
    public void testMissingSkillGapAnalysisHandling() {
        // user2 has no skill gap analysis
        CareerAssistantResponse resp = careerAssistantService.sendMessage(user2Email, null, "What are my skill gaps?");

        assertNotNull(resp);
        assertNotNull(resp.getMessage());
    }

    @Test
    public void testMissingMockInterviewsHandling() {
        // user2 has no mock interviews
        CareerAssistantResponse resp = careerAssistantService.sendMessage(user2Email, null, "How was my mock interview?");

        assertNotNull(resp);
        assertNotNull(resp.getMessage());
    }

    @Test
    public void testMissingCareerAnalyticsHandling() {
        // user2 has no career analytics generated yet
        CareerAssistantResponse resp = careerAssistantService.sendMessage(user2Email, null, "What is my readiness score?");

        assertNotNull(resp);
        assertNotNull(resp.getMessage());
    }

    @Test
    public void testGeminiMalformedJsonHandling() {
        when(geminiService.generateCareerAssistantResponse(any(), any(), any()))
                .thenThrow(new AIServiceException("AI Career Assistant produced a malformed response. Please try again."));

        assertThrows(AIServiceException.class, () -> {
            careerAssistantService.sendMessage(user1Email, null, "Test malformed");
        });
    }

    @Test
    public void testGemini429Handling() {
        when(geminiService.generateCareerAssistantResponse(any(), any(), any()))
                .thenThrow(new GeminiRateLimitException("Gemini usage limit reached. Please wait and try again later.", 30));

        GeminiRateLimitException ex = assertThrows(GeminiRateLimitException.class, () -> {
            careerAssistantService.sendMessage(user1Email, null, "Test quota");
        });

        assertEquals(30, ex.getRetryAfterSeconds());
        assertTrue(ex.getMessage().contains("Gemini usage limit reached"));
    }

    @Test
    public void testUnauthorizedConversationAccess() {
        AssistantConversationDto conv = careerAssistantService.createConversation(user1Email, "User 1 Private Chat");

        // User 2 cannot read User 1's conversation
        assertThrows(UnauthorizedChatAccessException.class, () -> {
            careerAssistantService.getConversation(conv.getId(), user2Email);
        });

        // User 2 cannot post messages to User 1's conversation
        assertThrows(UnauthorizedChatAccessException.class, () -> {
            careerAssistantService.sendMessage(user2Email, conv.getId(), "Hacking attempt");
        });

        // User 2 cannot delete User 1's conversation
        assertThrows(UnauthorizedChatAccessException.class, () -> {
            careerAssistantService.deleteConversation(conv.getId(), user2Email);
        });
    }

    @Test
    public void testStrictUserDataIsolation() {
        careerAssistantService.createConversation(user1Email, "User 1 Chat A");
        careerAssistantService.createConversation(user1Email, "User 1 Chat B");
        careerAssistantService.createConversation(user2Email, "User 2 Chat C");

        List<AssistantConversationDto> user1List = careerAssistantService.getConversations(user1Email);
        List<AssistantConversationDto> user2List = careerAssistantService.getConversations(user2Email);

        assertEquals(2, user1List.size());
        assertEquals(1, user2List.size());
        assertEquals("User 2 Chat C", user2List.get(0).getTitle());
    }

    @Test
    public void testConversationDeletion() {
        AssistantConversationDto conv = careerAssistantService.createConversation(user1Email, "To be deleted");
        careerAssistantService.sendMessage(user1Email, conv.getId(), "Message before delete");

        careerAssistantService.deleteConversation(conv.getId(), user1Email);

        assertThrows(ResourceNotFoundException.class, () -> {
            careerAssistantService.getConversation(conv.getId(), user1Email);
        });
    }

    @Test
    public void testEmptyAIResponseHandling() {
        CareerAssistantDto emptyDto = CareerAssistantDto.builder()
                .message("")
                .answer(null)
                .suggestedActions(Collections.emptyList())
                .contextUsed(Collections.emptyList())
                .build();

        when(geminiService.generateCareerAssistantResponse(any(), any(), any()))
                .thenReturn(emptyDto);

        CareerAssistantResponse resp = careerAssistantService.sendMessage(user1Email, null, "Hello");

        assertNotNull(resp);
        assertNotNull(resp.getMessage());
        assertNotNull(resp.getSuggestedActions());
        assertNotNull(resp.getContextUsed());
    }

    @Test
    public void testSuggestedActionParsing() {
        CareerAssistantResponse resp = careerAssistantService.sendMessage(user1Email, null, "What to do next?");

        assertNotNull(resp.getSuggestedActions());
        assertEquals(2, resp.getSuggestedActions().size());
        assertEquals("Practice Mock Interview", resp.getSuggestedActions().get(0).getLabel());
        assertEquals("/mock-interview", resp.getSuggestedActions().get(0).getRoute());
        assertEquals("/career-analytics", resp.getSuggestedActions().get(1).getRoute());
    }

    @Test
    public void testAskQuickQuestionEndpoint() {
        CareerAssistantResponse resp = careerAssistantService.askQuickQuestion(user1Email, "Quick tip for technical interviews?");

        assertNotNull(resp);
        assertNotNull(resp.getConversationId());
        assertNotNull(resp.getMessage());
    }
}
