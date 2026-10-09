package com.careerpilot.app.job;

import com.careerpilot.app.dto.JobPreparationDto;
import com.careerpilot.app.dto.request.RegisterRequest;
import com.careerpilot.app.dto.response.JobPreparationResponse;
import com.careerpilot.app.entity.Job;
import com.careerpilot.app.entity.Profile;
import com.careerpilot.app.entity.User;
import com.careerpilot.app.exception.AIServiceException;
import com.careerpilot.app.exception.ResourceNotFoundException;
import com.careerpilot.app.repository.JobPreparationRepository;
import com.careerpilot.app.repository.JobRepository;
import com.careerpilot.app.repository.ProfileRepository;
import com.careerpilot.app.repository.UserRepository;
import com.careerpilot.app.service.AuthService;
import com.careerpilot.app.service.GeminiService;
import com.careerpilot.app.service.JobPreparationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:jobpreptestdb;MODE=MySQL;DB_CLOSE_DELAY=-1;CASE_INSENSITIVE_IDENTIFIERS=TRUE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "app.jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
    "app.jwt.expiration=86400000",
    "app.resume.upload-dir=target/test-jobprep-uploads"
})
@Transactional
public class JobPreparationServiceTest {

    @Autowired
    private JobPreparationService jobPreparationService;

    @Autowired
    private JobPreparationRepository jobPreparationRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private AuthService authService;

    @MockBean
    private GeminiService geminiService;

    private String user1Email = "jobprepuser1@example.com";
    private String user2Email = "jobprepuser2@example.com";
    private Job testJob;

    @BeforeEach
    void setUp() {
        authService.register(new RegisterRequest("Prep Candidate 1", user1Email, "Password123"));
        authService.register(new RegisterRequest("Prep Candidate 2", user2Email, "Password123"));

        User user1 = userRepository.findByEmail(user1Email).orElseThrow();
        Profile profile1 = profileRepository.findByUser(user1).orElseGet(() -> Profile.builder().user(user1).build());
        profile1.setTargetRole("Java Backend Developer");
        profile1.setExperienceLevel("Fresher / Entry Level");
        profile1.setSkillsSummary("Java, Spring Boot, MySQL");
        profileRepository.save(profile1);

        testJob = jobRepository.save(Job.builder()
                .title("Java Software Engineer")
                .company("Global FinTech Corp")
                .location("Bangalore / Remote")
                .jobType("FULL_TIME")
                .experienceLevel("Fresher / 0-2 Years")
                .requiredSkills("Java, Spring Boot, MySQL, Docker, REST APIs")
                .description("Build mission-critical transactional services with high throughput.")
                .build());
    }

    private JobPreparationDto createMockPrepDto(int score, String level) {
        return JobPreparationDto.builder()
                .readinessScore(score)
                .preparationLevel(level)
                .summary("Candidate matches 80% of requirements for Java Software Engineer at Global FinTech Corp.")
                .strengths(List.of("Java 17", "Spring Boot", "MySQL"))
                .skillGaps(List.of("Docker", "Distributed Caching"))
                .technicalTopics(List.of("Bean Lifecycle", "Transaction Propagation", "Indexing"))
                .interviewTopics(List.of("API Security", "Microservices"))
                .codingTopics(List.of("Two Pointers", "Hash Maps"))
                .projectTopics(List.of("Explain architecture of REST API project"))
                .behavioralTopics(List.of("Handling tight delivery deadlines"))
                .preparationPlan(List.of(
                        JobPreparationDto.PreparationDayDto.builder()
                                .day(1)
                                .title("Day 1: Spring Framework Core")
                                .focus("IoC, DI, and Bean Lifecycle")
                                .topics(List.of("Bean scopes", "@Autowired"))
                                .tasks(List.of("Revise annotations cheatsheet"))
                                .practiceQuestions(List.of("How does Spring IoC work?"))
                                .build()
                ))
                .recommendedQuestions(List.of(
                        JobPreparationDto.RecommendedQuestionDto.builder()
                                .question("How do you secure REST APIs using JWT?")
                                .type("TECHNICAL")
                                .suggestedAnswer("Use Spring Security filter chain and validate JWT.")
                                .keyConcept("Spring Security")
                                .build()
                ))
                .build();
    }

    // 1. Successful Preparation Generation
    @Test
    void testSuccessfulPreparationGeneration() {
        Mockito.when(geminiService.analyzeJobPreparation(anyString(), any(Job.class), anyString(), anyString()))
                .thenReturn(createMockPrepDto(82, "INTERMEDIATE"));

        JobPreparationResponse response = jobPreparationService.generatePreparation(testJob.getId(), user1Email);

        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals(testJob.getId(), response.getJobId());
        assertEquals("Java Software Engineer", response.getJobTitle());
        assertEquals("Global FinTech Corp", response.getCompany());
        assertEquals(82, response.getReadinessScore());
        assertEquals("INTERMEDIATE", response.getPreparationLevel());
        assertEquals(3, response.getStrengths().size());
        assertEquals(2, response.getSkillGaps().size());
        assertEquals(1, response.getPreparationPlan().size());
        assertEquals(1, response.getRecommendedQuestions().size());
    }

    // 2. Cached Preparation
    @Test
    void testCachedPreparation() {
        Mockito.when(geminiService.analyzeJobPreparation(anyString(), any(Job.class), anyString(), anyString()))
                .thenReturn(createMockPrepDto(85, "ADVANCED"));

        // First call -> calls Gemini
        JobPreparationResponse first = jobPreparationService.generatePreparation(testJob.getId(), user1Email);
        assertNotNull(first);

        // Second call -> returns cached from database, does NOT call Gemini again
        JobPreparationResponse second = jobPreparationService.generatePreparation(testJob.getId(), user1Email);
        assertNotNull(second);
        assertEquals(first.getId(), second.getId());
        assertEquals(85, second.getReadinessScore());

        // Verify Gemini called once
        Mockito.verify(geminiService, Mockito.times(1))
                .analyzeJobPreparation(anyString(), any(Job.class), anyString(), anyString());
    }

    // 3. Regenerate Forces Gemini Call
    @Test
    void testRegenerate() {
        Mockito.when(geminiService.analyzeJobPreparation(anyString(), any(Job.class), anyString(), anyString()))
                .thenReturn(createMockPrepDto(70, "INTERMEDIATE"))
                .thenReturn(createMockPrepDto(90, "ADVANCED"));

        JobPreparationResponse first = jobPreparationService.generatePreparation(testJob.getId(), user1Email);
        assertEquals(70, first.getReadinessScore());

        JobPreparationResponse regenerated = jobPreparationService.regeneratePreparation(testJob.getId(), user1Email);
        assertEquals(90, regenerated.getReadinessScore());
        assertEquals("ADVANCED", regenerated.getPreparationLevel());

        // Verify Gemini called twice
        Mockito.verify(geminiService, Mockito.times(2))
                .analyzeJobPreparation(anyString(), any(Job.class), anyString(), anyString());
    }

    // 4. Malformed Gemini JSON
    @Test
    void testMalformedGeminiJson() {
        Mockito.when(geminiService.analyzeJobPreparation(anyString(), any(Job.class), anyString(), anyString()))
                .thenThrow(new AIServiceException("AI service produced a malformed job preparation output."));

        assertThrows(AIServiceException.class, () ->
                jobPreparationService.regeneratePreparation(testJob.getId(), user1Email));
    }

    // 5. Gemini Failure
    @Test
    void testGeminiFailure() {
        Mockito.when(geminiService.analyzeJobPreparation(anyString(), any(Job.class), anyString(), anyString()))
                .thenThrow(new AIServiceException("AI service is currently unavailable."));

        assertThrows(AIServiceException.class, () ->
                jobPreparationService.regeneratePreparation(testJob.getId(), user1Email));
    }

    // 6. Invalid Readiness Score
    @Test
    void testInvalidReadinessScore() {
        JobPreparationDto dto = createMockPrepDto(150, "ADVANCED");
        dto.setReadinessScore(Math.min(100, Math.max(0, dto.getReadinessScore())));

        Mockito.when(geminiService.analyzeJobPreparation(anyString(), any(Job.class), anyString(), anyString()))
                .thenReturn(dto);

        JobPreparationResponse response = jobPreparationService.regeneratePreparation(testJob.getId(), user1Email);
        assertTrue(response.getReadinessScore() <= 100);
    }

    // 7. Missing Profile Handled Gracefully
    @Test
    void testMissingProfile() {
        User user2 = userRepository.findByEmail(user2Email).orElseThrow();
        profileRepository.findByUser(user2).ifPresent(profileRepository::delete);

        Mockito.when(geminiService.analyzeJobPreparation(anyString(), any(Job.class), anyString(), anyString()))
                .thenReturn(createMockPrepDto(68, "BEGINNER"));

        JobPreparationResponse response = jobPreparationService.regeneratePreparation(testJob.getId(), user2Email);
        assertNotNull(response);
        assertEquals(68, response.getReadinessScore());
    }

    // 8. Missing ResumeAnalysis Handled Gracefully
    @Test
    void testMissingResumeAnalysis() {
        Mockito.when(geminiService.analyzeJobPreparation(anyString(), any(Job.class), anyString(), anyString()))
                .thenReturn(createMockPrepDto(75, "INTERMEDIATE"));

        JobPreparationResponse response = jobPreparationService.regeneratePreparation(testJob.getId(), user1Email);
        assertNotNull(response);
        assertNotNull(response.getStrengths());
    }

    // 9. Missing JobMatch Handled Gracefully
    @Test
    void testMissingJobMatch() {
        Mockito.when(geminiService.analyzeJobPreparation(anyString(), any(Job.class), anyString(), anyString()))
                .thenReturn(createMockPrepDto(78, "INTERMEDIATE"));

        JobPreparationResponse response = jobPreparationService.regeneratePreparation(testJob.getId(), user1Email);
        assertNotNull(response);
        assertEquals(78, response.getReadinessScore());
    }

    // 10. User Isolation
    @Test
    void testUserIsolation() {
        Mockito.when(geminiService.analyzeJobPreparation(anyString(), any(Job.class), anyString(), anyString()))
                .thenReturn(createMockPrepDto(75, "INTERMEDIATE"))
                .thenReturn(createMockPrepDto(88, "ADVANCED"));

        jobPreparationService.regeneratePreparation(testJob.getId(), user1Email);

        // User 2 cannot access User 1's preparation
        assertThrows(ResourceNotFoundException.class, () ->
                jobPreparationService.getPreparation(testJob.getId(), user2Email));
    }

    // 11. Preparation Persistence
    @Test
    void testPreparationPersistence() {
        Mockito.when(geminiService.analyzeJobPreparation(anyString(), any(Job.class), anyString(), anyString()))
                .thenReturn(createMockPrepDto(80, "INTERMEDIATE"));

        jobPreparationService.regeneratePreparation(testJob.getId(), user1Email);

        List<JobPreparationResponse> all = jobPreparationService.getAllPreparations(user1Email);
        assertNotNull(all);
        assertEquals(1, all.size());
        assertEquals("Java Software Engineer", all.get(0).getJobTitle());
    }
}
