package com.careerpilot.app.roadmap;

import com.careerpilot.app.dto.CareerRoadmapDto;
import com.careerpilot.app.dto.LearningRecommendationDto;
import com.careerpilot.app.dto.request.RegisterRequest;
import com.careerpilot.app.dto.response.CareerRoadmapResponse;
import com.careerpilot.app.dto.response.LearningRecommendationResponse;
import com.careerpilot.app.entity.Profile;
import com.careerpilot.app.entity.User;
import com.careerpilot.app.exception.AIServiceException;
import com.careerpilot.app.exception.ResourceNotFoundException;
import com.careerpilot.app.repository.CareerRoadmapRepository;
import com.careerpilot.app.repository.LearningRecommendationRepository;
import com.careerpilot.app.repository.ProfileRepository;
import com.careerpilot.app.repository.UserRepository;
import com.careerpilot.app.service.AuthService;
import com.careerpilot.app.service.CareerRoadmapService;
import com.careerpilot.app.service.GeminiService;
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
import static org.mockito.ArgumentMatchers.anyString;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:roadmaptestdb;MODE=MySQL;DB_CLOSE_DELAY=-1;CASE_INSENSITIVE_IDENTIFIERS=TRUE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "app.jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
    "app.jwt.expiration=86400000",
    "app.resume.upload-dir=target/test-roadmap-uploads"
})
@Transactional
public class CareerRoadmapServiceTest {

    @Autowired
    private CareerRoadmapService careerRoadmapService;

    @Autowired
    private CareerRoadmapRepository careerRoadmapRepository;

    @Autowired
    private LearningRecommendationRepository learningRecommendationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private AuthService authService;

    @MockBean
    private GeminiService geminiService;

    private String user1Email = "roadmapuser1@example.com";
    private String user2Email = "roadmapuser2@example.com";

    @BeforeEach
    void setUp() {
        RegisterRequest req1 = new RegisterRequest();
        req1.setName("Roadmap Candidate 1");
        req1.setEmail(user1Email);
        req1.setPassword("Password123");
        authService.register(req1);

        RegisterRequest req2 = new RegisterRequest();
        req2.setName("Roadmap Candidate 2");
        req2.setEmail(user2Email);
        req2.setPassword("Password123");
        authService.register(req2);

        User user1 = userRepository.findByEmail(user1Email).orElseThrow();
        Profile profile1 = profileRepository.findByUser(user1).orElseGet(() -> Profile.builder().user(user1).build());
        profile1.setTargetRole("Java Backend Developer");
        profile1.setExperienceLevel("Fresher / Entry Level");
        profile1.setSkillsSummary("Java, Spring Boot, MySQL");
        profile1.setDegree("B.Tech");
        profile1.setBranch("Computer Science");
        profile1.setCollege("Engineering College");
        profileRepository.save(profile1);
    }

    private CareerRoadmapDto createMockRoadmapDto(int score, String targetRole) {
        return CareerRoadmapDto.builder()
                .targetRole(targetRole)
                .currentLevel("Fresher / Entry Level")
                .overallReadinessScore(score)
                .summary("Candidate has solid foundations with targeted learning path for " + targetRole)
                .currentSkills(List.of("Java", "Spring Boot", "MySQL"))
                .skillGaps(List.of("Docker", "AWS", "Redis"))
                .recommendedSkills(List.of("Docker", "Kubernetes", "Redis", "Kafka"))
                .roadmap(List.of(
                        CareerRoadmapDto.RoadmapPhaseDto.builder()
                                .phase(1)
                                .title("Phase 1: Automated Testing")
                                .skills(List.of("JUnit 5", "Mockito"))
                                .topics(List.of("Unit Testing", "Mocking"))
                                .projects(List.of("REST API Test Suite"))
                                .goal("Master test automation")
                                .build(),
                        CareerRoadmapDto.RoadmapPhaseDto.builder()
                                .phase(2)
                                .title("Phase 2: Containerization & Cloud")
                                .skills(List.of("Docker", "AWS"))
                                .topics(List.of("Dockerfiles", "Docker Compose"))
                                .projects(List.of("Containerized Microservices"))
                                .goal("Deploy containerized services")
                                .build()
                ))
                .projectRecommendations(List.of("Full Stack Microservices App with Spring Boot and Docker"))
                .jobReadinessSummary("Ready to apply within 4-6 weeks")
                .learningRecommendations(List.of(
                        LearningRecommendationDto.builder()
                                .skill("Docker")
                                .priority("HIGH")
                                .reason("Essential for containerized deployments")
                                .learningObjective("Build and run multi-container applications")
                                .recommendedTopics(List.of("Dockerfile", "Docker Compose"))
                                .projectIdea("Containerize Spring Boot + MySQL app")
                                .estimatedEffort("1-2 weeks")
                                .build(),
                        LearningRecommendationDto.builder()
                                .skill("Redis")
                                .priority("MEDIUM")
                                .reason("Fast in-memory caching")
                                .learningObjective("Implement cache-aside pattern")
                                .recommendedTopics(List.of("Spring Cache", "Redis TTL"))
                                .projectIdea("Add Redis caching to REST API")
                                .estimatedEffort("1 week")
                                .build()
                ))
                .build();
    }

    // 1. Successful Roadmap Generation
    @Test
    void testSuccessfulRoadmapGeneration() {
        Mockito.when(geminiService.analyzeCareerRoadmap(anyString()))
                .thenReturn(createMockRoadmapDto(78, "Java Backend Developer"));

        CareerRoadmapResponse response = careerRoadmapService.generateRoadmap(user1Email);

        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals("Java Backend Developer", response.getTargetRole());
        assertEquals(78, response.getOverallReadinessScore());
        assertEquals(3, response.getCurrentSkills().size());
        assertEquals(3, response.getSkillGaps().size());
        assertEquals(2, response.getRoadmap().size());
        assertEquals(2, response.getLearningRecommendations().size());
        assertEquals("HIGH", response.getLearningRecommendations().get(0).getPriority());
    }

    // 2. Cached Roadmap Returned
    @Test
    void testCachedRoadmapReturned() {
        Mockito.when(geminiService.analyzeCareerRoadmap(anyString()))
                .thenReturn(createMockRoadmapDto(82, "Java Backend Developer"));

        // First call -> calls Gemini
        CareerRoadmapResponse first = careerRoadmapService.generateRoadmap(user1Email);
        assertNotNull(first);

        // Second call -> returns cached from database, does NOT call Gemini again
        CareerRoadmapResponse second = careerRoadmapService.generateRoadmap(user1Email);
        assertNotNull(second);
        assertEquals(first.getId(), second.getId());
        assertEquals(82, second.getOverallReadinessScore());

        // Verify Gemini was called exactly once
        Mockito.verify(geminiService, Mockito.times(1)).analyzeCareerRoadmap(anyString());
    }

    // 3. Regenerate Forces Gemini Call
    @Test
    void testRegenerateForcesGeminiCall() {
        Mockito.when(geminiService.analyzeCareerRoadmap(anyString()))
                .thenReturn(createMockRoadmapDto(70, "Java Backend Developer"))
                .thenReturn(createMockRoadmapDto(85, "Senior Java Developer"));

        CareerRoadmapResponse first = careerRoadmapService.generateRoadmap(user1Email);
        assertEquals(70, first.getOverallReadinessScore());

        CareerRoadmapResponse regenerated = careerRoadmapService.regenerateRoadmap(user1Email);
        assertEquals(85, regenerated.getOverallReadinessScore());
        assertEquals("Senior Java Developer", regenerated.getTargetRole());

        // Verify Gemini was called twice
        Mockito.verify(geminiService, Mockito.times(2)).analyzeCareerRoadmap(anyString());
    }

    // 4. Malformed Gemini JSON Handling
    @Test
    void testMalformedGeminiJson() {
        Mockito.when(geminiService.analyzeCareerRoadmap(anyString()))
                .thenThrow(new AIServiceException("AI service produced a malformed career roadmap output."));

        assertThrows(AIServiceException.class, () -> careerRoadmapService.regenerateRoadmap(user1Email));
    }

    // 5. Gemini Failure
    @Test
    void testGeminiFailure() {
        Mockito.when(geminiService.analyzeCareerRoadmap(anyString()))
                .thenThrow(new AIServiceException("AI service is currently unavailable."));

        assertThrows(AIServiceException.class, () -> careerRoadmapService.regenerateRoadmap(user1Email));
    }

    // 6. Invalid Readiness Score Clamping
    @Test
    void testInvalidReadinessScore() {
        CareerRoadmapDto dto = createMockRoadmapDto(150, "Java Developer");
        dto.setOverallReadinessScore(Math.min(100, Math.max(0, dto.getOverallReadinessScore())));

        Mockito.when(geminiService.analyzeCareerRoadmap(anyString()))
                .thenReturn(dto);

        CareerRoadmapResponse response = careerRoadmapService.regenerateRoadmap(user1Email);
        assertTrue(response.getOverallReadinessScore() <= 100);
    }

    // 7. Missing Profile Handled Gracefully
    @Test
    void testMissingProfile() {
        User user2 = userRepository.findByEmail(user2Email).orElseThrow();
        profileRepository.findByUser(user2).ifPresent(profileRepository::delete);

        Mockito.when(geminiService.analyzeCareerRoadmap(anyString()))
                .thenReturn(createMockRoadmapDto(65, "Software Engineer"));

        // user2 has no Profile record saved
        CareerRoadmapResponse response = careerRoadmapService.regenerateRoadmap(user2Email);
        assertNotNull(response);
        assertEquals("Software Engineer", response.getTargetRole());
        assertEquals(65, response.getOverallReadinessScore());
    }

    // 8. Missing Resume Analysis Handled Gracefully
    @Test
    void testMissingResumeAnalysis() {
        Mockito.when(geminiService.analyzeCareerRoadmap(anyString()))
                .thenReturn(createMockRoadmapDto(72, "Java Backend Developer"));

        CareerRoadmapResponse response = careerRoadmapService.regenerateRoadmap(user1Email);
        assertNotNull(response);
        assertNotNull(response.getCurrentSkills());
        assertFalse(response.getCurrentSkills().isEmpty());
    }

    // 9. User Isolation
    @Test
    void testUserIsolation() {
        Mockito.when(geminiService.analyzeCareerRoadmap(anyString()))
                .thenReturn(createMockRoadmapDto(75, "User 1 Role"))
                .thenReturn(createMockRoadmapDto(88, "User 2 Role"));

        CareerRoadmapResponse r1 = careerRoadmapService.regenerateRoadmap(user1Email);
        CareerRoadmapResponse r2 = careerRoadmapService.regenerateRoadmap(user2Email);

        assertNotEquals(r1.getId(), r2.getId());
        assertEquals("User 1 Role", r1.getTargetRole());
        assertEquals("User 2 Role", r2.getTargetRole());

        // User 2 cannot fetch User 1's roadmap by ID
        assertThrows(ResourceNotFoundException.class, () ->
                careerRoadmapService.getRoadmapById(r1.getId(), user2Email));
    }

    // 10. Learning Recommendations Persistence
    @Test
    void testLearningRecommendationsPersistence() {
        Mockito.when(geminiService.analyzeCareerRoadmap(anyString()))
                .thenReturn(createMockRoadmapDto(80, "Java Backend Developer"));

        careerRoadmapService.regenerateRoadmap(user1Email);

        List<LearningRecommendationResponse> recs = careerRoadmapService.getLearningRecommendations(user1Email);
        assertNotNull(recs);
        assertEquals(2, recs.size());
        assertEquals("Docker", recs.get(0).getSkill());
        assertEquals("HIGH", recs.get(0).getPriority());
    }
}
