package com.careerpilot.app.application;

import com.careerpilot.app.dto.ApplicationIntelligenceDto;
import com.careerpilot.app.dto.ApplicationStatsDto;
import com.careerpilot.app.dto.JobApplicationDto;
import com.careerpilot.app.dto.UpcomingTasksDto;
import com.careerpilot.app.entity.*;
import com.careerpilot.app.exception.ResourceNotFoundException;
import com.careerpilot.app.exception.UnauthorizedChatAccessException;
import com.careerpilot.app.repository.*;
import com.careerpilot.app.service.ApplicationIntelligenceService;
import com.careerpilot.app.service.GeminiService;
import com.careerpilot.app.service.JobApplicationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JobApplicationServiceTest {

    @Mock
    private JobApplicationRepository jobApplicationRepository;
    @Mock
    private ApplicationActivityRepository applicationActivityRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JobRepository jobRepository;
    @Mock
    private JobMatchRepository jobMatchRepository;
    @Mock
    private ProfileRepository profileRepository;
    @Mock
    private ResumeAnalysisRepository resumeAnalysisRepository;
    @Mock
    private SkillGapAnalysisRepository skillGapAnalysisRepository;
    @Mock
    private JobPreparationRepository jobPreparationRepository;
    @Mock
    private InterviewSessionRepository interviewSessionRepository;
    @Mock
    private CareerAnalyticsRepository careerAnalyticsRepository;
    @Mock
    private GeminiService geminiService;

    private JobApplicationService jobApplicationService;
    private ApplicationIntelligenceService applicationIntelligenceService;
    private ObjectMapper objectMapper;

    private User testUser;
    private User otherUser;
    private Job testJob;
    private JobApplication testApp;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        jobApplicationService = new JobApplicationService(
                jobApplicationRepository,
                applicationActivityRepository,
                userRepository,
                jobRepository,
                jobMatchRepository
        );

        applicationIntelligenceService = new ApplicationIntelligenceService(
                userRepository,
                jobApplicationRepository,
                profileRepository,
                resumeAnalysisRepository,
                jobMatchRepository,
                skillGapAnalysisRepository,
                jobPreparationRepository,
                interviewSessionRepository,
                careerAnalyticsRepository,
                geminiService,
                objectMapper
        );

        testUser = User.builder().id(1L).email("candidate@example.com").name("Sridhar").build();
        otherUser = User.builder().id(2L).email("hacker@example.com").name("Attacker").build();

        testJob = Job.builder()
                .id(100L)
                .title("Senior Java Engineer")
                .company("Acme Corp")
                .location("Bengaluru")
                .jobType("FULL_TIME")
                .experienceLevel("Experienced")
                .requiredSkills("Java, Spring Boot, MySQL")
                .build();

        testApp = JobApplication.builder()
                .id(500L)
                .user(testUser)
                .job(testJob)
                .status(ApplicationStatus.SAVED)
                .company("Acme Corp")
                .jobTitle("Senior Java Engineer")
                .location("Bengaluru")
                .source("CAREER_COPILOT")
                .applicationDate(LocalDate.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("1. Successfully create job application with SAVED status and CREATED activity")
    void testCreateApplication_Success() {
        when(userRepository.findByEmail("candidate@example.com")).thenReturn(Optional.of(testUser));
        when(jobRepository.findById(100L)).thenReturn(Optional.of(testJob));
        when(jobApplicationRepository.existsByUserAndJob(testUser, testJob)).thenReturn(false);
        when(jobApplicationRepository.save(any(JobApplication.class))).thenAnswer(i -> {
            JobApplication a = i.getArgument(0);
            a.setId(500L);
            return a;
        });

        JobApplicationDto dto = jobApplicationService.createApplication(100L, "candidate@example.com");

        assertNotNull(dto);
        assertEquals(500L, dto.getId());
        assertEquals("Senior Java Engineer", dto.getJobTitle());
        assertEquals("Acme Corp", dto.getCompany());
        assertEquals(ApplicationStatus.SAVED, dto.getStatus());

        verify(applicationActivityRepository, times(1)).save(argThat(act ->
                act.getActivityType() == ApplicationActivityType.CREATED &&
                act.getDescription().contains("Senior Java Engineer")
        ));
    }

    @Test
    @DisplayName("2. Prevent duplicate job applications for the same user")
    void testCreateApplication_Duplicate_ThrowsException() {
        when(userRepository.findByEmail("candidate@example.com")).thenReturn(Optional.of(testUser));
        when(jobRepository.findById(100L)).thenReturn(Optional.of(testJob));
        when(jobApplicationRepository.existsByUserAndJob(testUser, testJob)).thenReturn(true);

        assertThrows(IllegalStateException.class, () ->
                jobApplicationService.createApplication(100L, "candidate@example.com")
        );
        verify(jobApplicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("3. Create custom external job application")
    void testCreateCustomApplication_Success() {
        when(userRepository.findByEmail("candidate@example.com")).thenReturn(Optional.of(testUser));
        when(jobApplicationRepository.save(any(JobApplication.class))).thenAnswer(i -> {
            JobApplication a = i.getArgument(0);
            a.setId(501L);
            return a;
        });

        JobApplicationDto input = JobApplicationDto.builder()
                .company("Google")
                .jobTitle("Cloud Architect")
                .location("Hyderabad")
                .source("LINKEDIN")
                .status(ApplicationStatus.APPLIED)
                .build();

        JobApplicationDto res = jobApplicationService.createCustomApplication(input, "candidate@example.com");

        assertNotNull(res);
        assertEquals("Google", res.getCompany());
        assertEquals("Cloud Architect", res.getJobTitle());
        assertEquals(ApplicationStatus.APPLIED, res.getStatus());
        assertEquals("LINKEDIN", res.getSource());
    }

    @Test
    @DisplayName("4. Custom application throws error when company or job title is missing")
    void testCreateCustomApplication_MissingFields_ThrowsException() {
        JobApplicationDto input = JobApplicationDto.builder().company("").jobTitle("Engineer").build();
        assertThrows(IllegalArgumentException.class, () ->
                jobApplicationService.createCustomApplication(input, "candidate@example.com")
        );
    }

    @Test
    @DisplayName("5. Get application successfully with match score and timeline")
    void testGetApplication_Success() {
        when(userRepository.findByEmail("candidate@example.com")).thenReturn(Optional.of(testUser));
        when(jobApplicationRepository.findById(500L)).thenReturn(Optional.of(testApp));

        JobMatch jm = JobMatch.builder().user(testUser).job(testJob).overallScore(88).recommendation("STRONG_MATCH").build();
        when(jobMatchRepository.findByUserAndJob(testUser, testJob)).thenReturn(Optional.of(jm));

        ApplicationActivity act = ApplicationActivity.builder()
                .id(1L)
                .application(testApp)
                .activityType(ApplicationActivityType.CREATED)
                .description("Tracked opportunity.")
                .createdAt(LocalDateTime.now())
                .build();
        when(applicationActivityRepository.findByApplicationOrderByCreatedAtDesc(testApp)).thenReturn(List.of(act));

        JobApplicationDto dto = jobApplicationService.getApplication(500L, "candidate@example.com");

        assertNotNull(dto);
        assertEquals(88, dto.getMatchScore());
        assertEquals("STRONG_MATCH", dto.getMatchRecommendation());
        assertEquals(1, dto.getActivityTimeline().size());
    }

    @Test
    @DisplayName("6. User isolation: User B cannot get User A's application")
    void testGetApplication_UnauthorizedAccess_ThrowsException() {
        when(userRepository.findByEmail("hacker@example.com")).thenReturn(Optional.of(otherUser));
        when(jobApplicationRepository.findById(500L)).thenReturn(Optional.of(testApp));

        assertThrows(UnauthorizedChatAccessException.class, () ->
                jobApplicationService.getApplication(500L, "hacker@example.com")
        );
    }

    @Test
    @DisplayName("7. Update application fields and log activities for status, deadline, interview and notes")
    void testUpdateApplication_Success() {
        when(userRepository.findByEmail("candidate@example.com")).thenReturn(Optional.of(testUser));
        when(jobApplicationRepository.findById(500L)).thenReturn(Optional.of(testApp));
        when(jobApplicationRepository.save(any(JobApplication.class))).thenAnswer(i -> i.getArgument(0));

        LocalDateTime interview = LocalDateTime.now().plusDays(5);
        LocalDate followUp = LocalDate.now().plusDays(3);
        LocalDate deadline = LocalDate.now().plusDays(10);

        JobApplicationDto updateDto = JobApplicationDto.builder()
                .status(ApplicationStatus.INTERVIEW)
                .interviewDate(interview)
                .nextFollowUpDate(followUp)
                .deadline(deadline)
                .notes("Reviewed system design topics and concurrency models.")
                .recruiterName("Jane Doe")
                .recruiterEmail("jane@acme.com")
                .build();

        JobApplicationDto updated = jobApplicationService.updateApplication(500L, updateDto, "candidate@example.com");

        assertNotNull(updated);
        assertEquals(ApplicationStatus.INTERVIEW, updated.getStatus());
        assertEquals("Jane Doe", updated.getRecruiterName());
        assertEquals("jane@acme.com", updated.getRecruiterEmail());

        // Verify activity creation for status, interview, follow up, deadline, and notes
        verify(applicationActivityRepository, atLeast(4)).save(any(ApplicationActivity.class));
    }

    @Test
    @DisplayName("8. Update status directly and log specific activity type")
    void testUpdateStatus_Success() {
        when(userRepository.findByEmail("candidate@example.com")).thenReturn(Optional.of(testUser));
        when(jobApplicationRepository.findById(500L)).thenReturn(Optional.of(testApp));
        when(jobApplicationRepository.save(any(JobApplication.class))).thenAnswer(i -> i.getArgument(0));

        JobApplicationDto updated = jobApplicationService.updateStatus(500L, ApplicationStatus.OFFER, "candidate@example.com");

        assertNotNull(updated);
        assertEquals(ApplicationStatus.OFFER, updated.getStatus());

        verify(applicationActivityRepository, times(1)).save(argThat(act ->
                act.getActivityType() == ApplicationActivityType.OFFER_RECEIVED
        ));
    }

    @Test
    @DisplayName("9. User isolation: User B cannot update User A's application status")
    void testUpdateStatus_Unauthorized_ThrowsException() {
        when(userRepository.findByEmail("hacker@example.com")).thenReturn(Optional.of(otherUser));
        when(jobApplicationRepository.findById(500L)).thenReturn(Optional.of(testApp));

        assertThrows(UnauthorizedChatAccessException.class, () ->
                jobApplicationService.updateStatus(500L, ApplicationStatus.OFFER, "hacker@example.com")
        );
    }

    @Test
    @DisplayName("10. Successfully delete candidate's application")
    void testDeleteApplication_Success() {
        when(userRepository.findByEmail("candidate@example.com")).thenReturn(Optional.of(testUser));
        when(jobApplicationRepository.findById(500L)).thenReturn(Optional.of(testApp));

        jobApplicationService.deleteApplication(500L, "candidate@example.com");

        verify(jobApplicationRepository, times(1)).delete(testApp);
    }

    @Test
    @DisplayName("11. User isolation: User B cannot delete User A's application")
    void testDeleteApplication_Unauthorized_ThrowsException() {
        when(userRepository.findByEmail("hacker@example.com")).thenReturn(Optional.of(otherUser));
        when(jobApplicationRepository.findById(500L)).thenReturn(Optional.of(testApp));

        assertThrows(UnauthorizedChatAccessException.class, () ->
                jobApplicationService.deleteApplication(500L, "hacker@example.com")
        );
        verify(jobApplicationRepository, never()).delete(any());
    }

    @Test
    @DisplayName("12. Correctly calculate application funnel statistics and conversion rates")
    void testGetStatistics_Calculations() {
        when(userRepository.findByEmail("candidate@example.com")).thenReturn(Optional.of(testUser));

        JobApplication app1 = JobApplication.builder().status(ApplicationStatus.SAVED).build();
        JobApplication app2 = JobApplication.builder().status(ApplicationStatus.APPLIED).build();
        JobApplication app3 = JobApplication.builder().status(ApplicationStatus.SCREENING).build();
        JobApplication app4 = JobApplication.builder().status(ApplicationStatus.INTERVIEW).build();
        JobApplication app5 = JobApplication.builder().status(ApplicationStatus.OFFER).build();
        JobApplication app6 = JobApplication.builder().status(ApplicationStatus.REJECTED).build();

        when(jobApplicationRepository.findByUserOrderByUpdatedAtDesc(testUser))
                .thenReturn(List.of(app1, app2, app3, app4, app5, app6));

        ApplicationStatsDto stats = jobApplicationService.getStatistics("candidate@example.com");

        assertNotNull(stats);
        assertEquals(6, stats.getTotalApplications());
        assertEquals(1, stats.getSavedCount());
        assertEquals(1, stats.getAppliedCount());
        assertEquals(1, stats.getScreeningCount());
        assertEquals(1, stats.getInterviewCount());
        assertEquals(1, stats.getOfferCount());
        assertEquals(1, stats.getRejectedCount());
        assertEquals(3, stats.getActiveCount()); // applied + screening + interview

        // Total applied = 5 (6 total - 1 saved)
        // Response = (1 screening + 1 interview + 1 offer) / 5 = 60.0%
        // Interview = (1 interview + 1 offer) / 5 = 40.0%
        // Offer = 1 offer / 5 = 20.0%
        assertEquals(60.0, stats.getResponseRate());
        assertEquals(40.0, stats.getInterviewRate());
        assertEquals(20.0, stats.getOfferRate());
    }

    @Test
    @DisplayName("13. Retrieve upcoming tasks grouped by follow-ups, interviews, and deadlines")
    void testGetUpcomingTasks() {
        when(userRepository.findByEmail("candidate@example.com")).thenReturn(Optional.of(testUser));

        JobApplication fApp = JobApplication.builder().id(1L).jobTitle("Backend Dev").nextFollowUpDate(LocalDate.now().plusDays(2)).build();
        JobApplication iApp = JobApplication.builder().id(2L).jobTitle("Fullstack Lead").interviewDate(LocalDateTime.now().plusDays(3)).build();
        JobApplication dApp = JobApplication.builder().id(3L).jobTitle("DevOps").deadline(LocalDate.now().plusDays(4)).build();

        when(jobApplicationRepository.findByUserAndNextFollowUpDateIsNotNullOrderByNextFollowUpDateAsc(testUser))
                .thenReturn(List.of(fApp));
        when(jobApplicationRepository.findByUserAndInterviewDateIsNotNullOrderByInterviewDateAsc(testUser))
                .thenReturn(List.of(iApp));
        when(jobApplicationRepository.findByUserAndDeadlineIsNotNullOrderByDeadlineAsc(testUser))
                .thenReturn(List.of(dApp));

        UpcomingTasksDto tasks = jobApplicationService.getUpcomingTasks("candidate@example.com");

        assertNotNull(tasks);
        assertEquals(1, tasks.getUpcomingFollowUps().size());
        assertEquals(1, tasks.getUpcomingInterviews().size());
        assertEquals(1, tasks.getApproachingDeadlines().size());
    }

    @Test
    @DisplayName("14. AI Application Strategy analysis generated and cached")
    void testAnalyzeApplication_GeneratesAndCaches() {
        when(userRepository.findByEmail("candidate@example.com")).thenReturn(Optional.of(testUser));
        when(jobApplicationRepository.findById(500L)).thenReturn(Optional.of(testApp));

        ApplicationIntelligenceDto mockAi = ApplicationIntelligenceDto.builder()
                .readinessScore(85)
                .priority("HIGH")
                .summary("Candidate is highly aligned for this Senior Java position.")
                .strengths(List.of("Strong Spring Boot knowledge", "Relational database expertise"))
                .risks(List.of("Missing Kubernetes experience"))
                .recommendedActions(List.of("Review containerization fundamentals"))
                .followUpAdvice(List.of("Follow up 5 days post-submission"))
                .interviewAdvice(List.of("Prepare database indexing deep-dive"))
                .build();

        when(geminiService.analyzeApplicationStrategy(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(mockAi);

        ApplicationIntelligenceDto result = applicationIntelligenceService.analyzeApplication(500L, "candidate@example.com");

        assertNotNull(result);
        assertEquals(85, result.getReadinessScore());
        assertEquals("HIGH", result.getPriority());
        assertFalse(result.getIsCached());

        verify(jobApplicationRepository, times(1)).save(argThat(a ->
                a.getAiAnalysisJson() != null && a.getAiAnalysisJson().contains("Senior Java position")
        ));
    }

    @Test
    @DisplayName("15. AI Application Strategy returns cached analysis if present")
    void testAnalyzeApplication_ReturnsCached() {
        testApp.setAiAnalysisJson("{\"readinessScore\":90,\"priority\":\"HIGH\",\"summary\":\"Cached intelligence.\" }");

        when(userRepository.findByEmail("candidate@example.com")).thenReturn(Optional.of(testUser));
        when(jobApplicationRepository.findById(500L)).thenReturn(Optional.of(testApp));

        ApplicationIntelligenceDto result = applicationIntelligenceService.analyzeApplication(500L, "candidate@example.com");

        assertNotNull(result);
        assertEquals(90, result.getReadinessScore());
        assertTrue(result.getIsCached());
        verify(geminiService, never()).analyzeApplicationStrategy(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("16. AI Application Strategy regeneration bypasses cache and updates record")
    void testRegenerateAnalysis_Success() {
        testApp.setAiAnalysisJson("{\"readinessScore\":50,\"summary\":\"Old analysis.\" }");

        when(userRepository.findByEmail("candidate@example.com")).thenReturn(Optional.of(testUser));
        when(jobApplicationRepository.findById(500L)).thenReturn(Optional.of(testApp));

        ApplicationIntelligenceDto mockFresh = ApplicationIntelligenceDto.builder()
                .readinessScore(88)
                .priority("HIGH")
                .summary("Freshly regenerated strategy.")
                .build();

        when(geminiService.analyzeApplicationStrategy(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(mockFresh);

        ApplicationIntelligenceDto result = applicationIntelligenceService.regenerateAnalysis(500L, "candidate@example.com");

        assertNotNull(result);
        assertEquals(88, result.getReadinessScore());
        assertFalse(result.getIsCached());
        verify(geminiService, times(1)).analyzeApplicationStrategy(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("17. User isolation: Unauthorized user cannot trigger AI application analysis")
    void testAnalyzeApplication_Unauthorized_ThrowsException() {
        when(userRepository.findByEmail("hacker@example.com")).thenReturn(Optional.of(otherUser));
        when(jobApplicationRepository.findById(500L)).thenReturn(Optional.of(testApp));

        assertThrows(UnauthorizedChatAccessException.class, () ->
                applicationIntelligenceService.analyzeApplication(500L, "hacker@example.com")
        );
        verify(geminiService, never()).analyzeApplicationStrategy(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("18. Filter applications by status returns matching records")
    void testGetApplicationsByStatus() {
        when(userRepository.findByEmail("candidate@example.com")).thenReturn(Optional.of(testUser));
        when(jobApplicationRepository.findByUserAndStatusOrderByUpdatedAtDesc(testUser, ApplicationStatus.INTERVIEW))
                .thenReturn(List.of(testApp));

        List<JobApplicationDto> list = jobApplicationService.getApplicationsByStatus(ApplicationStatus.INTERVIEW, "candidate@example.com");

        assertNotNull(list);
        assertEquals(1, list.size());
    }
}
