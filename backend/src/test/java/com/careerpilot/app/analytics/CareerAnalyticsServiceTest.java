package com.careerpilot.app.analytics;

import com.careerpilot.app.dto.CareerAnalyticsDto;
import com.careerpilot.app.dto.request.RegisterRequest;
import com.careerpilot.app.dto.response.CareerAnalyticsResponse;
import com.careerpilot.app.entity.CareerAnalytics;
import com.careerpilot.app.entity.Profile;
import com.careerpilot.app.entity.User;
import com.careerpilot.app.exception.AIServiceException;
import com.careerpilot.app.exception.GeminiRateLimitException;
import com.careerpilot.app.exception.ResourceNotFoundException;
import com.careerpilot.app.repository.CareerAnalyticsRepository;
import com.careerpilot.app.repository.ProfileRepository;
import com.careerpilot.app.repository.UserRepository;
import com.careerpilot.app.service.AuthService;
import com.careerpilot.app.service.CareerAnalyticsService;
import com.careerpilot.app.service.GeminiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
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
    "spring.datasource.url=jdbc:h2:mem:analyticstestdb;MODE=MySQL;DB_CLOSE_DELAY=-1;CASE_INSENSITIVE_IDENTIFIERS=TRUE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "app.jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
    "app.jwt.expiration=86400000",
    "app.resume.upload-dir=target/test-analytics-uploads"
})
@Transactional
public class CareerAnalyticsServiceTest {

    @Autowired
    private CareerAnalyticsService careerAnalyticsService;

    @Autowired
    private CareerAnalyticsRepository careerAnalyticsRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private AuthService authService;

    @MockBean
    private GeminiService geminiService;

    private String user1Email = "analyticsuser1@example.com";
    private String user2Email = "analyticsuser2@example.com";

    private CareerAnalyticsDto mockDto;

    @BeforeEach
    public void setup() {
        if (userRepository.findByEmail(user1Email).isEmpty()) {
            authService.register(new RegisterRequest("Analytics User One", user1Email, "Password123!"));
        }
        if (userRepository.findByEmail(user2Email).isEmpty()) {
            authService.register(new RegisterRequest("Analytics User Two", user2Email, "Password123!"));
        }

        User user1 = userRepository.findByEmail(user1Email).get();
        profileRepository.findByUser(user1).orElseGet(() -> {
            Profile p = Profile.builder()
                    .user(user1)
                    .targetRole("Senior Java Developer")
                    .experienceLevel("MID_LEVEL")
                    .skillsSummary("Java, Spring Boot, MySQL, Docker")
                    .build();
            return profileRepository.save(p);
        });

        mockDto = CareerAnalyticsDto.builder()
                .overallReadinessScore(82)
                .readinessLevel("READY")
                .careerSummary("Strong technical foundation in Java and Spring Boot. Ready for mid-to-senior engineering roles.")
                .strengths(List.of("Java", "Spring Boot", "REST APIs", "Clean Architecture"))
                .weaknesses(List.of(
                        CareerAnalyticsDto.WeaknessItem.builder()
                                .skill("System Design")
                                .currentStatus("Needs distributed systems caching and queue experience")
                                .priority("HIGH")
                                .recommendedAction("Study Redis caching and Kafka event-driven architectures")
                                .build()
                ))
                .skillProgress(List.of(
                        CareerAnalyticsDto.SkillProgressItem.builder()
                                .skill("Java")
                                .category("TECHNICAL")
                                .status("STRONG")
                                .proficiencyPercentage(90)
                                .build(),
                        CareerAnalyticsDto.SkillProgressItem.builder()
                                .skill("Kafka")
                                .category("TOOL")
                                .status("DEVELOPING")
                                .proficiencyPercentage(45)
                                .build()
                ))
                .interviewProgress(CareerAnalyticsDto.InterviewProgressSummary.builder()
                        .averageScore(85.0)
                        .technicalScore(88.0)
                        .behavioralScore(82.0)
                        .communicationScore(85.0)
                        .totalInterviews(4)
                        .trend("IMPROVING")
                        .build())
                .jobSearchProgress(CareerAnalyticsDto.JobSearchProgressSummary.builder()
                        .jobsMatched(12)
                        .savedJobs(3)
                        .appliedJobs(1)
                        .preparationSessions(2)
                        .avgMatchScore(80.0)
                        .build())
                .roadmapProgress(CareerAnalyticsDto.RoadmapProgressSummary.builder()
                        .completedMilestones(3)
                        .totalMilestones(6)
                        .currentMilestone("Microservices Architecture")
                        .upcomingMilestone("Cloud Deployment")
                        .completionPercentage(50)
                        .build())
                .recommendations(List.of(
                        CareerAnalyticsDto.RecommendationItem.builder()
                                .priority("HIGH")
                                .action("Complete System Design Practice")
                                .reason("Crucial for Senior Java Developer interviews")
                                .relatedFeature("CAREER_ROADMAP")
                                .build()
                ))
                .weeklyGoals(List.of(
                        CareerAnalyticsDto.WeeklyGoalItem.builder()
                                .day("Day 1")
                                .title("Distributed Caching with Redis")
                                .description("Review cache-aside and write-through patterns")
                                .focusArea("System Design")
                                .build()
                ))
                .build();

        when(geminiService.analyzeCareerAnalytics(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(mockDto);
    }

    @Test
    public void testGenerateAnalyticsSuccessfully() {
        CareerAnalyticsResponse response = careerAnalyticsService.generateAnalytics(user1Email);

        assertNotNull(response);
        assertEquals(82, response.getOverallReadinessScore());
        assertEquals("READY", response.getReadinessLevel());
        assertFalse(response.getStrengths().isEmpty());
        assertEquals("Java", response.getStrengths().get(0));
        assertEquals(1, response.getWeaknesses().size());
        assertEquals("System Design", response.getWeaknesses().get(0).getSkill());
        assertEquals(4, response.getInterviewProgress().getTotalInterviews());
        assertEquals(12, response.getJobSearchProgress().getJobsMatched());
        assertEquals(50, response.getRoadmapProgress().getCompletionPercentage());
        assertEquals(1, response.getRecommendations().size());
        assertEquals(1, response.getWeeklyGoals().size());

        verify(geminiService, times(1))
                .analyzeCareerAnalytics(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    public void testExistingAnalyticsReturnedWithoutCallingGemini() {
        careerAnalyticsService.generateAnalytics(user1Email);

        // Second call should return cached result without calling Gemini
        CareerAnalyticsResponse cached = careerAnalyticsService.generateAnalytics(user1Email);

        assertNotNull(cached);
        assertEquals(82, cached.getOverallReadinessScore());
        verify(geminiService, times(1))
                .analyzeCareerAnalytics(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    public void testRegenerateForcesAICall() {
        careerAnalyticsService.generateAnalytics(user1Email);

        CareerAnalyticsDto updatedDto = CareerAnalyticsDto.builder()
                .overallReadinessScore(88)
                .readinessLevel("READY")
                .careerSummary("Upgraded score after system design practice.")
                .strengths(List.of("Java", "System Design"))
                .weaknesses(Collections.emptyList())
                .skillProgress(Collections.emptyList())
                .interviewProgress(new CareerAnalyticsDto.InterviewProgressSummary())
                .jobSearchProgress(new CareerAnalyticsDto.JobSearchProgressSummary())
                .roadmapProgress(new CareerAnalyticsDto.RoadmapProgressSummary())
                .recommendations(Collections.emptyList())
                .weeklyGoals(Collections.emptyList())
                .build();

        when(geminiService.analyzeCareerAnalytics(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(updatedDto);

        CareerAnalyticsResponse refreshed = careerAnalyticsService.regenerateAnalytics(user1Email);

        assertNotNull(refreshed);
        assertEquals(88, refreshed.getOverallReadinessScore());
        assertEquals(82, refreshed.getPreviousReadinessScore());
        verify(geminiService, times(2))
                .analyzeCareerAnalytics(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    public void testScoreBelowZeroIsClamped() {
        mockDto.setOverallReadinessScore(-25);
        when(geminiService.analyzeCareerAnalytics(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(mockDto);

        CareerAnalyticsResponse response = careerAnalyticsService.regenerateAnalytics(user1Email);

        assertNotNull(response);
        assertEquals(0, response.getOverallReadinessScore());
    }

    @Test
    public void testScoreAbove100IsClamped() {
        mockDto.setOverallReadinessScore(150);
        when(geminiService.analyzeCareerAnalytics(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(mockDto);

        CareerAnalyticsResponse response = careerAnalyticsService.regenerateAnalytics(user1Email);

        assertNotNull(response);
        assertEquals(100, response.getOverallReadinessScore());
    }

    @Test
    public void testNullCollectionsBecomeEmptyCollections() {
        CareerAnalyticsDto emptyCollectionsDto = CareerAnalyticsDto.builder()
                .overallReadinessScore(65)
                .readinessLevel("ALMOST READY")
                .careerSummary("Valid summary")
                .strengths(null)
                .weaknesses(null)
                .skillProgress(null)
                .interviewProgress(null)
                .jobSearchProgress(null)
                .roadmapProgress(null)
                .recommendations(null)
                .weeklyGoals(null)
                .build();

        when(geminiService.analyzeCareerAnalytics(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(emptyCollectionsDto);

        CareerAnalyticsResponse response = careerAnalyticsService.regenerateAnalytics(user1Email);

        assertNotNull(response);
        assertNotNull(response.getStrengths());
        assertNotNull(response.getWeaknesses());
        assertNotNull(response.getSkillProgress());
        assertNotNull(response.getInterviewProgress());
        assertNotNull(response.getJobSearchProgress());
        assertNotNull(response.getRoadmapProgress());
        assertNotNull(response.getRecommendations());
        assertNotNull(response.getWeeklyGoals());
    }

    @Test
    public void testInvalidAIJsonIsHandled() {
        when(geminiService.analyzeCareerAnalytics(any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new AIServiceException("AI Career Analytics produced a malformed response. Please try again."));

        assertThrows(AIServiceException.class, () -> {
            careerAnalyticsService.regenerateAnalytics(user1Email);
        });
    }

    @Test
    public void testMissingResumeDataIsHandled() {
        // user2 has no resume uploaded
        CareerAnalyticsResponse response = careerAnalyticsService.generateAnalytics(user2Email);

        assertNotNull(response);
        assertEquals(82, response.getOverallReadinessScore());
    }

    @Test
    public void testMissingInterviewDataIsHandled() {
        // user2 has no interview session history
        CareerAnalyticsResponse response = careerAnalyticsService.generateAnalytics(user2Email);

        assertNotNull(response);
        assertNotNull(response.getInterviewProgress());
    }

    @Test
    public void testGemini429IsHandledCorrectly() {
        when(geminiService.analyzeCareerAnalytics(any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new GeminiRateLimitException("Gemini usage limit reached. Please wait and try again later.", 45));

        GeminiRateLimitException ex = assertThrows(GeminiRateLimitException.class, () -> {
            careerAnalyticsService.regenerateAnalytics(user1Email);
        });

        assertEquals(45, ex.getRetryAfterSeconds());
        assertTrue(ex.getMessage().contains("Gemini usage limit reached"));
    }

    @Test
    public void testUnauthorizedUserCannotAccessAnotherUserAnalytics() {
        careerAnalyticsService.generateAnalytics(user1Email);

        // User2 has not generated analytics yet
        assertThrows(ResourceNotFoundException.class, () -> {
            careerAnalyticsService.getAnalytics(user2Email);
        });
    }

    @Test
    public void testDatabasePersistenceWorks() {
        CareerAnalyticsResponse generated = careerAnalyticsService.generateAnalytics(user1Email);

        User user1 = userRepository.findByEmail(user1Email).get();
        CareerAnalytics stored = careerAnalyticsRepository.findByUser(user1).orElse(null);

        assertNotNull(stored);
        assertEquals(generated.getId(), stored.getId());
        assertEquals(82, stored.getOverallReadinessScore());
        assertNotNull(stored.getGeneratedAt());
        assertNotNull(stored.getUpdatedAt());
    }

    @Test
    public void testCachedAnalyticsDoNotUnnecessarilyCallGemini() {
        careerAnalyticsService.generateAnalytics(user1Email);
        reset(geminiService);

        // Calling getAnalytics must NOT call geminiService
        CareerAnalyticsResponse response = careerAnalyticsService.getAnalytics(user1Email);

        assertNotNull(response);
        assertEquals(82, response.getOverallReadinessScore());
        verifyNoInteractions(geminiService);
    }

    @Test
    public void testEmptyCareerDataProducesValidResponse() {
        String newUserEmail = "brandnewcandidate@example.com";
        authService.register(new RegisterRequest("Brand New", newUserEmail, "Password123!"));

        CareerAnalyticsResponse response = careerAnalyticsService.generateAnalytics(newUserEmail);

        assertNotNull(response);
        assertEquals(82, response.getOverallReadinessScore());
    }
}
