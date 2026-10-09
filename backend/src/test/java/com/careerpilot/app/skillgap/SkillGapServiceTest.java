package com.careerpilot.app.skillgap;

import com.careerpilot.app.dto.request.RegisterRequest;
import com.careerpilot.app.dto.request.SkillGapAnalyzeRequest;
import com.careerpilot.app.dto.response.LearningPlanStepDto;
import com.careerpilot.app.dto.response.SkillGapAnalysisResponse;
import com.careerpilot.app.dto.response.SkillGapDto;
import com.careerpilot.app.dto.response.SkillGapGeminiDto;
import com.careerpilot.app.entity.Job;
import com.careerpilot.app.entity.Profile;
import com.careerpilot.app.exception.AIServiceException;
import com.careerpilot.app.exception.ResourceNotFoundException;
import com.careerpilot.app.repository.JobRepository;
import com.careerpilot.app.repository.ProfileRepository;
import com.careerpilot.app.repository.UserRepository;
import com.careerpilot.app.service.AuthService;
import com.careerpilot.app.service.GeminiService;
import com.careerpilot.app.service.SkillGapService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:skillgaptestdb;MODE=MySQL;DB_CLOSE_DELAY=-1;CASE_INSENSITIVE_IDENTIFIERS=TRUE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "app.jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
    "app.jwt.expiration=86400000",
    "app.resume.upload-dir=target/test-skillgap-uploads"
})
@Transactional
public class SkillGapServiceTest {

    @Autowired
    private SkillGapService skillGapService;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private JobRepository jobRepository;

    @MockBean
    private GeminiService geminiService;

    private String user1Email = "skillgapuser1@example.com";
    private String user2Email = "skillgapuser2@example.com";
    private Long jobId;

    @BeforeEach
    void setUp() {
        RegisterRequest req1 = new RegisterRequest();
        req1.setName("Skill Candidate 1");
        req1.setEmail(user1Email);
        req1.setPassword("Password123");
        authService.register(req1);

        RegisterRequest req2 = new RegisterRequest();
        req2.setName("Skill Candidate 2");
        req2.setEmail(user2Email);
        req2.setPassword("Password123");
        authService.register(req2);

        var user1 = userRepository.findByEmail(user1Email).orElseThrow();
        var profile = profileRepository.findByUser(user1).orElseGet(() -> Profile.builder().user(user1).build());
        profile.setTargetRole("Java Backend Developer");
        profile.setExperienceLevel("FRESHER");
        profile.setSkillsSummary("Java, Spring Boot, MySQL, Git");
        profileRepository.save(profile);

        Job job = jobRepository.save(Job.builder()
                .title("Full Stack Java Engineer")
                .company("CloudForge Corp")
                .location("Bengaluru, India")
                .jobType("FULL_TIME")
                .experienceLevel("FRESHER")
                .salaryMin(new BigDecimal("700000"))
                .salaryMax(new BigDecimal("1000000"))
                .description("Build full stack Java solutions with Spring Boot and React.")
                .requiredSkills("Java, Spring Boot, React, MySQL")
                .preferredSkills("Docker, AWS")
                .postedDate(LocalDate.now())
                .build());
        jobId = job.getId();
    }

    private SkillGapGeminiDto mockSkillGapDto() {
        SkillGapDto gap = SkillGapDto.builder()
                .skillName("Docker & Containers")
                .priority("HIGH")
                .currentLevel("None")
                .requiredLevel("Intermediate")
                .importance("Critical")
                .reason("Needed for microservices deployment.")
                .recommendedTechnologies(List.of("Docker", "Docker Compose"))
                .practiceProjects(List.of("Containerize a Spring Boot web service"))
                .estimatedLearningHours(25)
                .difficulty("Intermediate")
                .learningOrder(1)
                .build();

        LearningPlanStepDto step = LearningPlanStepDto.builder()
                .step(1)
                .phaseTitle("Phase 1: Containerization")
                .skillName("Docker")
                .description("Learn Docker basics, Dockerfile, and multi-container setups.")
                .estimatedHours(25)
                .keyTopics(List.of("Images", "Containers", "Docker Compose"))
                .milestoneProject("Deploy multi-container app")
                .build();

        return SkillGapGeminiDto.builder()
                .readinessScore(78)
                .skillCoverageScore(72)
                .summary("Candidate has solid Java foundations but needs containerization skills.")
                .currentSkills(List.of("Java", "Spring Boot", "MySQL", "Git"))
                .requiredSkills(List.of("Java", "Spring Boot", "MySQL", "Git", "Docker", "AWS"))
                .missingSkills(List.of("Docker", "AWS"))
                .skillGaps(List.of(gap))
                .learningPlan(List.of(step))
                .build();
    }

    @Test
    void analyzeSkillGap_Success_TargetRole() {
        Mockito.when(geminiService.analyzeSkillGap(anyString(), anyString(), any()))
                .thenReturn(mockSkillGapDto());

        SkillGapAnalyzeRequest request = SkillGapAnalyzeRequest.builder()
                .targetRole("Java Backend Developer")
                .build();

        SkillGapAnalysisResponse response = skillGapService.analyzeSkillGap(request, user1Email);

        assertNotNull(response.getId());
        assertEquals("Java Backend Developer", response.getTargetRole());
        assertEquals(78, response.getReadinessScore());
        assertEquals(72, response.getSkillCoverageScore());
        assertEquals(4, response.getCurrentSkills().size());
        assertEquals(2, response.getMissingSkills().size());
        assertEquals(1, response.getSkillGaps().size());
        assertEquals(1, response.getLearningPlan().size());
        assertEquals("HIGH", response.getSkillGaps().get(0).getPriority());
    }

    @Test
    void analyzeSkillGap_Success_Job() {
        Mockito.when(geminiService.analyzeSkillGap(anyString(), anyString(), any()))
                .thenReturn(mockSkillGapDto());

        SkillGapAnalyzeRequest request = SkillGapAnalyzeRequest.builder()
                .jobId(jobId)
                .build();

        SkillGapAnalysisResponse response = skillGapService.analyzeSkillGap(request, user1Email);

        assertNotNull(response.getId());
        assertEquals(jobId, response.getJobId());
        assertEquals("Full Stack Java Engineer", response.getJobTitle());
        assertEquals("CloudForge Corp", response.getJobCompany());
    }

    @Test
    void analyzeSkillGap_Cached_DoesNotCallGeminiAgain() {
        Mockito.when(geminiService.analyzeSkillGap(anyString(), anyString(), any()))
                .thenReturn(mockSkillGapDto());

        SkillGapAnalyzeRequest request = SkillGapAnalyzeRequest.builder()
                .targetRole("Java Backend Developer")
                .build();

        SkillGapAnalysisResponse first = skillGapService.analyzeSkillGap(request, user1Email);
        assertNotNull(first);

        // Second request without forceRefresh -> must return from cache
        SkillGapAnalysisResponse second = skillGapService.analyzeSkillGap(request, user1Email);
        assertNotNull(second);
        assertEquals(first.getId(), second.getId());

        // Gemini should only have been called ONCE
        Mockito.verify(geminiService, Mockito.times(1)).analyzeSkillGap(anyString(), anyString(), any());
    }

    @Test
    void analyzeSkillGap_ForceRefresh_CallsGeminiAgain() {
        Mockito.when(geminiService.analyzeSkillGap(anyString(), anyString(), any()))
                .thenReturn(mockSkillGapDto());

        SkillGapAnalyzeRequest request1 = SkillGapAnalyzeRequest.builder()
                .targetRole("Java Backend Developer")
                .build();
        skillGapService.analyzeSkillGap(request1, user1Email);

        // Second request with forceRefresh=true -> must call Gemini again
        SkillGapAnalyzeRequest request2 = SkillGapAnalyzeRequest.builder()
                .targetRole("Java Backend Developer")
                .forceRefresh(true)
                .build();
        skillGapService.analyzeSkillGap(request2, user1Email);

        Mockito.verify(geminiService, Mockito.times(2)).analyzeSkillGap(anyString(), anyString(), any());
    }

    @Test
    void analyzeSkillGap_MissingTargetRoleAndProfile_ThrowsException() {
        SkillGapAnalyzeRequest request = SkillGapAnalyzeRequest.builder().build();

        // User 2 has no profile with targetRole
        assertThrows(IllegalArgumentException.class, () ->
                skillGapService.analyzeSkillGap(request, user2Email));
    }

    @Test
    void analyzeSkillGap_MissingJob_ThrowsResourceNotFoundException() {
        SkillGapAnalyzeRequest request = SkillGapAnalyzeRequest.builder()
                .jobId(99999L)
                .build();

        assertThrows(ResourceNotFoundException.class, () ->
                skillGapService.analyzeSkillGap(request, user1Email));
    }

    @Test
    void analyzeSkillGap_GeminiFailure_ThrowsAIServiceException() {
        Mockito.when(geminiService.analyzeSkillGap(anyString(), anyString(), any()))
                .thenThrow(new AIServiceException("AI quota exceeded"));

        SkillGapAnalyzeRequest request = SkillGapAnalyzeRequest.builder()
                .targetRole("Java Backend Developer")
                .build();

        assertThrows(AIServiceException.class, () ->
                skillGapService.analyzeSkillGap(request, user1Email));
    }

    @Test
    void getLatestSkillGap_Success() {
        Mockito.when(geminiService.analyzeSkillGap(anyString(), anyString(), any()))
                .thenReturn(mockSkillGapDto());

        SkillGapAnalyzeRequest request = SkillGapAnalyzeRequest.builder()
                .targetRole("Java Backend Developer")
                .build();
        skillGapService.analyzeSkillGap(request, user1Email);

        SkillGapAnalysisResponse latest = skillGapService.getLatestSkillGap(user1Email);
        assertNotNull(latest);
        assertEquals(78, latest.getReadinessScore());
    }

    @Test
    void getSkillGapHistory_Success() {
        Mockito.when(geminiService.analyzeSkillGap(anyString(), anyString(), any()))
                .thenReturn(mockSkillGapDto());

        SkillGapAnalyzeRequest request = SkillGapAnalyzeRequest.builder()
                .targetRole("Java Backend Developer")
                .build();
        skillGapService.analyzeSkillGap(request, user1Email);

        List<SkillGapAnalysisResponse> history = skillGapService.getSkillGapHistory(user1Email);
        assertNotNull(history);
        assertEquals(1, history.size());
    }

    @Test
    void deleteSkillGap_Success() {
        Mockito.when(geminiService.analyzeSkillGap(anyString(), anyString(), any()))
                .thenReturn(mockSkillGapDto());

        SkillGapAnalyzeRequest request = SkillGapAnalyzeRequest.builder()
                .targetRole("Java Backend Developer")
                .build();
        SkillGapAnalysisResponse response = skillGapService.analyzeSkillGap(request, user1Email);

        skillGapService.deleteSkillGap(response.getId(), user1Email);

        assertThrows(ResourceNotFoundException.class, () ->
                skillGapService.getLatestSkillGap(user1Email));
    }

    @Test
    void userIsolation_UserTwoCannotDeleteUserOneAnalysis() {
        Mockito.when(geminiService.analyzeSkillGap(anyString(), anyString(), any()))
                .thenReturn(mockSkillGapDto());

        SkillGapAnalyzeRequest request = SkillGapAnalyzeRequest.builder()
                .targetRole("Java Backend Developer")
                .build();
        SkillGapAnalysisResponse response = skillGapService.analyzeSkillGap(request, user1Email);

        // User 2 trying to delete User 1's analysis must throw ResourceNotFoundException
        assertThrows(ResourceNotFoundException.class, () ->
                skillGapService.deleteSkillGap(response.getId(), user2Email));
    }
}
