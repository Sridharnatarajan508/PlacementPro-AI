package com.careerpilot.app.job;

import com.careerpilot.app.dto.request.RegisterRequest;
import com.careerpilot.app.dto.response.JobMatchDto;
import com.careerpilot.app.dto.response.JobMatchResponse;
import com.careerpilot.app.dto.response.JobRecommendationResponse;
import com.careerpilot.app.entity.Job;
import com.careerpilot.app.exception.AIServiceException;
import com.careerpilot.app.exception.ResourceNotFoundException;
import com.careerpilot.app.repository.JobRepository;
import com.careerpilot.app.service.AuthService;
import com.careerpilot.app.service.GeminiService;
import com.careerpilot.app.service.JobMatchingService;
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
import static org.mockito.ArgumentMatchers.*;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:jobmatchtestdb;MODE=MySQL;DB_CLOSE_DELAY=-1;CASE_INSENSITIVE_IDENTIFIERS=TRUE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "app.jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
    "app.jwt.expiration=86400000",
    "app.resume.upload-dir=target/test-jobmatch-uploads"
})
@Transactional
public class JobMatchingServiceTest {

    @Autowired
    private JobMatchingService jobMatchingService;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private AuthService authService;

    @MockBean
    private GeminiService geminiService;

    private String user1Email = "matchcandidate1@example.com";
    private String user2Email = "matchcandidate2@example.com";
    private Long job1Id;
    private Long job2Id;

    @BeforeEach
    void setUp() {
        RegisterRequest user1 = new RegisterRequest();
        user1.setName("Match Candidate 1");
        user1.setEmail(user1Email);
        user1.setPassword("Password123");
        authService.register(user1);

        RegisterRequest user2 = new RegisterRequest();
        user2.setName("Match Candidate 2");
        user2.setEmail(user2Email);
        user2.setPassword("Password123");
        authService.register(user2);

        Job job1 = jobRepository.save(Job.builder()
                .title("Java Backend Developer")
                .company("CloudCore Tech")
                .location("Bengaluru, India")
                .jobType("FULL_TIME")
                .experienceLevel("FRESHER")
                .salaryMin(new BigDecimal("700000"))
                .salaryMax(new BigDecimal("1000000"))
                .description("Build high performance Java APIs.")
                .requiredSkills("Java, Spring Boot, MySQL")
                .preferredSkills("Docker, AWS")
                .postedDate(LocalDate.now())
                .build());
        job1Id = job1.getId();

        Job job2 = jobRepository.save(Job.builder()
                .title("Python Data Engineer")
                .company("DataMatrix")
                .location("Hyderabad, India")
                .jobType("FULL_TIME")
                .experienceLevel("JUNIOR")
                .salaryMin(new BigDecimal("650000"))
                .salaryMax(new BigDecimal("950000"))
                .description("Build big data pipelines with Python.")
                .requiredSkills("Python, SQL, Spark")
                .preferredSkills("AWS, Snowflake")
                .postedDate(LocalDate.now().minusDays(2))
                .build());
        job2Id = job2.getId();
    }

    private JobMatchDto mockJobMatchDto(int overall, String rec) {
        return JobMatchDto.builder()
                .overallScore(overall)
                .skillScore(overall + 5)
                .experienceScore(overall)
                .educationScore(100)
                .matchingSkills(List.of("Java", "Spring Boot", "MySQL"))
                .missingSkills(List.of("Docker", "AWS"))
                .recommendation(rec)
                .explanation("Candidate has strong Java and Spring Boot background matching role requirements.")
                .build();
    }

    @Test
    void matchJob_Success() {
        Mockito.when(geminiService.analyzeJobMatch(anyString(), any()))
                .thenReturn(mockJobMatchDto(88, "STRONG_MATCH"));

        JobMatchResponse response = jobMatchingService.matchJob(job1Id, user1Email);

        assertNotNull(response.getId());
        assertEquals(job1Id, response.getJobId());
        assertEquals(88, response.getOverallScore());
        assertEquals(93, response.getSkillScore());
        assertEquals(88, response.getExperienceScore());
        assertEquals(100, response.getEducationScore());
        assertEquals("STRONG_MATCH", response.getRecommendation());
        assertEquals(3, response.getMatchingSkills().size());
        assertEquals(2, response.getMissingSkills().size());
        assertTrue(response.getAiExplanation().contains("Java"));
    }

    @Test
    void matchJob_CachedResponse_DoesNotCallGeminiAgain() {
        Mockito.when(geminiService.analyzeJobMatch(anyString(), any()))
                .thenReturn(mockJobMatchDto(88, "STRONG_MATCH"));

        // First matching request -> calls Gemini
        JobMatchResponse first = jobMatchingService.matchJob(job1Id, user1Email);
        assertNotNull(first);

        // Second matching request -> must return cached from DB without calling Gemini
        JobMatchResponse second = jobMatchingService.matchJob(job1Id, user1Email);
        assertNotNull(second);
        assertEquals(first.getId(), second.getId());
        assertEquals(88, second.getOverallScore());

        // Verify Gemini was called exactly ONCE
        Mockito.verify(geminiService, Mockito.times(1)).analyzeJobMatch(anyString(), any());
    }

    @Test
    void matchJob_UserIsolation_UserTwoGetsDistinctMatch() {
        Mockito.when(geminiService.analyzeJobMatch(anyString(), any()))
                .thenReturn(mockJobMatchDto(88, "STRONG_MATCH"))
                .thenReturn(mockJobMatchDto(72, "GOOD_MATCH"));

        JobMatchResponse user1Match = jobMatchingService.matchJob(job1Id, user1Email);
        JobMatchResponse user2Match = jobMatchingService.matchJob(job1Id, user2Email);

        assertNotNull(user1Match);
        assertNotNull(user2Match);
        assertNotEquals(user1Match.getId(), user2Match.getId());
    }

    @Test
    void matchJob_GeminiServiceFailure_ThrowsAIServiceException() {
        Mockito.when(geminiService.analyzeJobMatch(anyString(), any()))
                .thenThrow(new AIServiceException("Gemini quota exceeded."));

        assertThrows(AIServiceException.class, () ->
                jobMatchingService.matchJob(job1Id, user1Email));
    }

    @Test
    void getMatchForJob_NonExistentMatch_ThrowsResourceNotFoundException() {
        assertThrows(ResourceNotFoundException.class, () ->
                jobMatchingService.getMatchForJob(job1Id, user1Email));
    }

    @Test
    void getRecommendedJobs_ReturnsRankedRecommendations() {
        Mockito.when(geminiService.analyzeJobMatch(anyString(), any()))
                .thenReturn(mockJobMatchDto(90, "STRONG_MATCH"));

        jobMatchingService.matchJob(job1Id, user1Email);

        List<JobRecommendationResponse> recommended = jobMatchingService.getRecommendedJobs(user1Email);

        assertNotNull(recommended);
        assertFalse(recommended.isEmpty());
        // First recommended job must have the highest match score
        assertEquals(job1Id, recommended.get(0).getJob().getId());
        assertNotNull(recommended.get(0).getMatch());
        assertEquals(90, recommended.get(0).getMatch().getOverallScore());
    }
}
