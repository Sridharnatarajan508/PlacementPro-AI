package com.careerpilot.app.interview;

import com.careerpilot.app.dto.gemini.InterviewEvaluationDto;
import com.careerpilot.app.dto.gemini.InterviewFinalEvaluationDto;
import com.careerpilot.app.dto.gemini.InterviewQuestionDto;
import com.careerpilot.app.dto.request.InterviewStartRequest;
import com.careerpilot.app.dto.response.InterviewEvaluationResponse;
import com.careerpilot.app.dto.response.InterviewHistoryResponse;
import com.careerpilot.app.dto.response.InterviewQuestionResponse;
import com.careerpilot.app.dto.response.InterviewSessionResponse;
import com.careerpilot.app.entity.InterviewQuestion;
import com.careerpilot.app.entity.InterviewSession;
import com.careerpilot.app.entity.Job;
import com.careerpilot.app.entity.User;
import com.careerpilot.app.exception.AIServiceException;
import com.careerpilot.app.exception.ResourceNotFoundException;
import com.careerpilot.app.repository.*;
import com.careerpilot.app.service.AuthService;
import com.careerpilot.app.service.GeminiService;
import com.careerpilot.app.service.InterviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class InterviewServiceTest {

    @Autowired
    private InterviewService interviewService;

    @Autowired
    private InterviewSessionRepository sessionRepository;

    @Autowired
    private InterviewQuestionRepository questionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private AuthService authService;

    @MockBean
    private GeminiService geminiService;

    private String user1Email = "interviewuser1@example.com";
    private String user2Email = "interviewuser2@example.com";
    private User user1;
    private User user2;
    private Job testJob;

    @BeforeEach
    void setUp() {
        com.careerpilot.app.dto.request.RegisterRequest req1 = new com.careerpilot.app.dto.request.RegisterRequest();
        req1.setName("User One");
        req1.setEmail(user1Email);
        req1.setPassword("Password123");
        authService.register(req1);
        user1 = userRepository.findByEmail(user1Email).orElseThrow();

        com.careerpilot.app.dto.request.RegisterRequest req2 = new com.careerpilot.app.dto.request.RegisterRequest();
        req2.setName("User Two");
        req2.setEmail(user2Email);
        req2.setPassword("Password123");
        authService.register(req2);
        user2 = userRepository.findByEmail(user2Email).orElseThrow();

        testJob = jobRepository.save(Job.builder()
                .title("Senior Java Developer")
                .company("Acme Corp")
                .location("Remote")
                .jobType("FULL_TIME")
                .experienceLevel("MID")
                .requiredSkills("Java, Spring Boot, MySQL, Kafka")
                .description("Build high scale microservices.")
                .build());

        // Default Mock Responses for Gemini
        when(geminiService.generateInterviewQuestion(any(), any(), any(), any(), any(), anyInt(), anyInt(), any()))
                .thenReturn(InterviewQuestionDto.builder()
                        .question("Explain how Spring Bean scopes work and when you would use Prototype scope.")
                        .category("TECHNICAL")
                        .difficulty("MEDIUM")
                        .expectedTopics(List.of("Singleton", "Prototype", "Request", "Session"))
                        .build());

        when(geminiService.evaluateInterviewAnswer(any(), any(), any(), any(), any(), any()))
                .thenReturn(InterviewEvaluationDto.builder()
                        .score(85)
                        .technicalScore(90)
                        .relevanceScore(85)
                        .communicationScore(80)
                        .confidenceScore(85)
                        .feedback("Strong and articulate answer on Spring lifecycle.")
                        .strengths(List.of("Clear definition of Singleton vs Prototype"))
                        .improvements(List.of("Mention thread safety implications"))
                        .idealAnswer("Spring default scope is Singleton, but Prototype creates a new instance each time.")
                        .build());

        when(geminiService.generateFinalInterviewEvaluation(any(), any(), any(), any()))
                .thenReturn(InterviewFinalEvaluationDto.builder()
                        .overallScore(85)
                        .technicalScore(90)
                        .communicationScore(80)
                        .confidenceScore(85)
                        .problemSolvingScore(85)
                        .summary("Candidate demonstrated comprehensive backend knowledge.")
                        .strengths(List.of("Solid Java and Spring fundamentals"))
                        .weaknesses(List.of("Could improve depth on distributed transactions"))
                        .recommendations(List.of("Practice Saga pattern and 2PC"))
                        .build());
    }

    @Test
    void testStartInterview_Success() {
        InterviewStartRequest request = InterviewStartRequest.builder()
                .targetRole("Java Backend Engineer")
                .interviewType("TECHNICAL")
                .difficulty("MEDIUM")
                .totalQuestions(5)
                .build();

        InterviewSessionResponse response = interviewService.startInterview(request, user1.getEmail());

        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals("Java Backend Engineer", response.getTargetRole());
        assertEquals("TECHNICAL", response.getInterviewType());
        assertEquals(5, response.getTotalQuestions());
        assertEquals(0, response.getCompletedQuestions());
        assertEquals("IN_PROGRESS", response.getStatus());
        assertNotNull(response.getQuestions());
        assertEquals(1, response.getQuestions().size());
        assertEquals(1, response.getQuestions().get(0).getQuestionNumber());
        assertEquals("Explain how Spring Bean scopes work and when you would use Prototype scope.",
                response.getQuestions().get(0).getQuestionText());
    }

    @Test
    void testStartInterview_WithJob() {
        InterviewStartRequest request = InterviewStartRequest.builder()
                .targetJobId(testJob.getId())
                .interviewType("MIXED")
                .difficulty("HARD")
                .totalQuestions(10)
                .build();

        InterviewSessionResponse response = interviewService.startInterview(request, user1.getEmail());

        assertNotNull(response);
        assertEquals(testJob.getId(), response.getTargetJobId());
        assertEquals("Senior Java Developer", response.getTargetRole());
        assertEquals("HARD", response.getDifficulty());
    }

    @Test
    void testSubmitAnswer_EvaluatesAndGeneratesNextQuestion() {
        InterviewSessionResponse sessionResp = interviewService.startInterview(
                InterviewStartRequest.builder().targetRole("Java Developer").totalQuestions(5).build(), user1.getEmail());

        Long sessionId = sessionResp.getId();
        Long question1Id = sessionResp.getQuestions().get(0).getId();

        InterviewEvaluationResponse evalResp = interviewService.submitAnswer(
                sessionId, question1Id, "Singleton creates one instance per container, Prototype creates a new instance every lookup.", user1.getEmail());

        assertNotNull(evalResp);
        assertEquals(85, evalResp.getScore());
        assertEquals(90, evalResp.getTechnicalScore());
        assertEquals(1, evalResp.getQuestionNumber());
        assertFalse(evalResp.getIsComplete());
        assertNotNull(evalResp.getNextQuestion());
        assertEquals(2, evalResp.getNextQuestion().getQuestionNumber());

        // Check DB state
        InterviewSession session = sessionRepository.findById(sessionId).orElseThrow();
        assertEquals(1, session.getCompletedQuestions());
    }

    @Test
    void testSubmitAnswer_FinalQuestion_CompletesInterviewAndCalculatesScores() {
        InterviewSessionResponse sessionResp = interviewService.startInterview(
                InterviewStartRequest.builder().targetRole("Java Developer").totalQuestions(5).build(), user1.getEmail());

        Long sessionId = sessionResp.getId();
        InterviewSession session = sessionRepository.findById(sessionId).orElseThrow();
        session.setTotalQuestions(1); // Set to 1 total question for quick completion test
        sessionRepository.save(session);

        Long questionId = sessionResp.getQuestions().get(0).getId();

        InterviewEvaluationResponse evalResp = interviewService.submitAnswer(
                sessionId, questionId, "Comprehensive Spring answer.", user1.getEmail());

        assertNotNull(evalResp);
        assertTrue(evalResp.getIsComplete());
        assertNull(evalResp.getNextQuestion());
        assertNotNull(evalResp.getFinalSessionResult());
        assertEquals("COMPLETED", evalResp.getFinalSessionResult().getStatus());
        assertEquals(85, evalResp.getFinalSessionResult().getOverallScore());
        assertEquals(90, evalResp.getFinalSessionResult().getTechnicalScore());
        assertNotNull(evalResp.getFinalSessionResult().getAiSummary());
    }

    @Test
    void testGetNextQuestion_ReturnsActiveUnansweredQuestion() {
        InterviewSessionResponse sessionResp = interviewService.startInterview(
                InterviewStartRequest.builder().targetRole("Java Developer").totalQuestions(5).build(), user1.getEmail());

        InterviewQuestionResponse nextQ = interviewService.getNextQuestion(sessionResp.getId(), user1.getEmail());

        assertNotNull(nextQ);
        assertEquals(1, nextQ.getQuestionNumber());
        assertFalse(nextQ.getAnswered());
    }

    @Test
    void testGetNextQuestion_WhenAlreadyCompleted_ThrowsException() {
        InterviewSessionResponse sessionResp = interviewService.startInterview(
                InterviewStartRequest.builder().targetRole("Java Developer").totalQuestions(5).build(), user1.getEmail());

        InterviewSession session = sessionRepository.findById(sessionResp.getId()).orElseThrow();
        session.setStatus("COMPLETED");
        sessionRepository.save(session);

        assertThrows(IllegalStateException.class, () ->
                interviewService.getNextQuestion(session.getId(), user1.getEmail()));
    }

    @Test
    void testGetInterview_Success() {
        InterviewSessionResponse sessionResp = interviewService.startInterview(
                InterviewStartRequest.builder().targetRole("Full Stack Engineer").totalQuestions(5).build(), user1.getEmail());

        InterviewSessionResponse fetched = interviewService.getInterview(sessionResp.getId(), user1.getEmail());

        assertNotNull(fetched);
        assertEquals("Full Stack Engineer", fetched.getTargetRole());
        assertEquals(sessionResp.getId(), fetched.getId());
    }

    @Test
    void testGetInterviewHistory_ReturnsHistoryList() {
        interviewService.startInterview(InterviewStartRequest.builder().targetRole("Role A").build(), user1.getEmail());
        interviewService.startInterview(InterviewStartRequest.builder().targetRole("Role B").build(), user1.getEmail());

        List<InterviewHistoryResponse> history = interviewService.getInterviewHistory(user1.getEmail());

        assertEquals(2, history.size());
        assertEquals("Role B", history.get(0).getTargetRole());
    }

    @Test
    void testAbandonInterview_SetsStatusAbandoned() {
        InterviewSessionResponse sessionResp = interviewService.startInterview(
                InterviewStartRequest.builder().targetRole("Java Developer").build(), user1.getEmail());

        interviewService.abandonInterview(sessionResp.getId(), user1.getEmail());

        InterviewSession session = sessionRepository.findById(sessionResp.getId()).orElseThrow();
        assertEquals("ABANDONED", session.getStatus());
    }

    @Test
    void testSubmitAnswer_WhenSessionAbandoned_ThrowsException() {
        InterviewSessionResponse sessionResp = interviewService.startInterview(
                InterviewStartRequest.builder().targetRole("Java Developer").build(), user1.getEmail());

        interviewService.abandonInterview(sessionResp.getId(), user1.getEmail());

        assertThrows(IllegalStateException.class, () ->
                interviewService.submitAnswer(sessionResp.getId(), sessionResp.getQuestions().get(0).getId(), "Answer", user1.getEmail()));
    }

    @Test
    void testSubmitAnswer_WhenSessionCompleted_ThrowsException() {
        InterviewSessionResponse sessionResp = interviewService.startInterview(
                InterviewStartRequest.builder().targetRole("Java Developer").build(), user1.getEmail());

        InterviewSession session = sessionRepository.findById(sessionResp.getId()).orElseThrow();
        session.setStatus("COMPLETED");
        sessionRepository.save(session);

        assertThrows(IllegalStateException.class, () ->
                interviewService.submitAnswer(sessionResp.getId(), sessionResp.getQuestions().get(0).getId(), "Answer", user1.getEmail()));
    }

    @Test
    void testSubmitAnswer_DuplicateAnswer_ThrowsException() {
        InterviewSessionResponse sessionResp = interviewService.startInterview(
                InterviewStartRequest.builder().targetRole("Java Developer").totalQuestions(5).build(), user1.getEmail());

        Long sessionId = sessionResp.getId();
        Long questionId = sessionResp.getQuestions().get(0).getId();

        interviewService.submitAnswer(sessionId, questionId, "First answer submission.", user1.getEmail());

        assertThrows(IllegalStateException.class, () ->
                interviewService.submitAnswer(sessionId, questionId, "Second duplicate submission attempt.", user1.getEmail()));
    }

    @Test
    void testSubmitAnswer_BlankAnswer_ThrowsException() {
        InterviewSessionResponse sessionResp = interviewService.startInterview(
                InterviewStartRequest.builder().targetRole("Java Developer").build(), user1.getEmail());

        assertThrows(IllegalArgumentException.class, () ->
                interviewService.submitAnswer(sessionResp.getId(), sessionResp.getQuestions().get(0).getId(), "   ", user1.getEmail()));
    }

    @Test
    void testUserIsolation_UserBCannotAccessUserASession() {
        InterviewSessionResponse sessionResp = interviewService.startInterview(
                InterviewStartRequest.builder().targetRole("Java Developer").build(), user1.getEmail());

        assertThrows(ResourceNotFoundException.class, () ->
                interviewService.getInterview(sessionResp.getId(), user2.getEmail()));
    }

    @Test
    void testUserIsolation_UserBCannotAnswerUserAQuestion() {
        InterviewSessionResponse sessionResp = interviewService.startInterview(
                InterviewStartRequest.builder().targetRole("Java Developer").build(), user1.getEmail());

        assertThrows(ResourceNotFoundException.class, () ->
                interviewService.submitAnswer(sessionResp.getId(), sessionResp.getQuestions().get(0).getId(), "Answer from User 2", user2.getEmail()));
    }

    @Test
    void testGeminiFailure_ThrowsAIServiceException() {
        when(geminiService.generateInterviewQuestion(any(), any(), any(), any(), any(), anyInt(), anyInt(), any()))
                .thenThrow(new AIServiceException("Gemini API connection timeout"));

        assertThrows(AIServiceException.class, () ->
                interviewService.startInterview(InterviewStartRequest.builder().targetRole("Java Dev").build(), user1.getEmail()));
    }

    @Test
    void testMalformedGeminiJson_ThrowsAIServiceException() {
        when(geminiService.evaluateInterviewAnswer(any(), any(), any(), any(), any(), any()))
                .thenThrow(new AIServiceException("AI service produced a malformed interview evaluation."));

        InterviewSessionResponse sessionResp = interviewService.startInterview(
                InterviewStartRequest.builder().targetRole("Java Developer").build(), user1.getEmail());

        assertThrows(AIServiceException.class, () ->
                interviewService.submitAnswer(sessionResp.getId(), sessionResp.getQuestions().get(0).getId(), "Valid candidate answer", user1.getEmail()));
    }
}
