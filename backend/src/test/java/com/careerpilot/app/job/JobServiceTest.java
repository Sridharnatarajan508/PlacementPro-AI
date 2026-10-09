package com.careerpilot.app.job;

import com.careerpilot.app.dto.request.JobSearchRequest;
import com.careerpilot.app.dto.request.RegisterRequest;
import com.careerpilot.app.dto.response.JobResponse;
import com.careerpilot.app.entity.Job;
import com.careerpilot.app.exception.DuplicateResourceException;
import com.careerpilot.app.exception.ResourceNotFoundException;
import com.careerpilot.app.repository.JobRepository;
import com.careerpilot.app.service.AuthService;
import com.careerpilot.app.service.JobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:jobtestdb;MODE=MySQL;DB_CLOSE_DELAY=-1;CASE_INSENSITIVE_IDENTIFIERS=TRUE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "app.jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
    "app.jwt.expiration=86400000",
    "app.resume.upload-dir=target/test-job-uploads"
})
@Transactional
public class JobServiceTest {

    @Autowired
    private JobService jobService;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private AuthService authService;

    private String user1Email = "jobuser1@example.com";
    private String user2Email = "jobuser2@example.com";
    private Long job1Id;
    private Long job2Id;

    @BeforeEach
    void setUp() {
        RegisterRequest user1 = new RegisterRequest();
        user1.setName("Job Candidate 1");
        user1.setEmail(user1Email);
        user1.setPassword("Password123");
        authService.register(user1);

        RegisterRequest user2 = new RegisterRequest();
        user2.setName("Job Candidate 2");
        user2.setEmail(user2Email);
        user2.setPassword("Password123");
        authService.register(user2);

        Job job1 = jobRepository.save(Job.builder()
                .title("Java Software Engineer")
                .company("Apex Systems")
                .location("Bengaluru, India")
                .jobType("FULL_TIME")
                .experienceLevel("FRESHER")
                .salaryMin(new BigDecimal("600000"))
                .salaryMax(new BigDecimal("900000"))
                .description("Build scalable Java backend services.")
                .requiredSkills("Java, Spring Boot, MySQL")
                .preferredSkills("Docker, AWS")
                .postedDate(LocalDate.now())
                .build());
        job1Id = job1.getId();

        Job job2 = jobRepository.save(Job.builder()
                .title("Frontend React Developer")
                .company("WebWorks")
                .location("Remote")
                .jobType("CONTRACT")
                .experienceLevel("JUNIOR")
                .salaryMin(new BigDecimal("500000"))
                .salaryMax(new BigDecimal("800000"))
                .description("Build modern React user interfaces.")
                .requiredSkills("React, JavaScript, CSS")
                .preferredSkills("TypeScript, Tailwind")
                .postedDate(LocalDate.now().minusDays(1))
                .build());
        job2Id = job2.getId();
    }

    @Test
    void getJobs_NoFilters_ReturnsAllJobs() {
        JobSearchRequest request = new JobSearchRequest();
        List<JobResponse> jobs = jobService.getJobs(request, user1Email);

        assertNotNull(jobs);
        assertTrue(jobs.size() >= 2);
    }

    @Test
    void getJobs_WithKeywordFilter_ReturnsMatchingJobs() {
        JobSearchRequest request = JobSearchRequest.builder()
                .keyword("Java")
                .build();
        List<JobResponse> jobs = jobService.getJobs(request, user1Email);

        assertNotNull(jobs);
        assertFalse(jobs.isEmpty());
        assertTrue(jobs.stream().allMatch(j ->
                j.getTitle().contains("Java") ||
                j.getRequiredSkills().contains("Java") ||
                j.getDescription().contains("Java")));
    }

    @Test
    void getJobs_WithJobTypeFilter_ReturnsFilteredJobs() {
        JobSearchRequest request = JobSearchRequest.builder()
                .jobType("CONTRACT")
                .build();
        List<JobResponse> jobs = jobService.getJobs(request, user1Email);

        assertNotNull(jobs);
        assertTrue(jobs.stream().allMatch(j -> j.getJobType().equalsIgnoreCase("CONTRACT")));
    }

    @Test
    void getJobById_ValidId_ReturnsJob() {
        JobResponse job = jobService.getJobById(job1Id, user1Email);

        assertNotNull(job);
        assertEquals("Java Software Engineer", job.getTitle());
        assertEquals("Apex Systems", job.getCompany());
        assertFalse(job.getIsSaved());
    }

    @Test
    void getJobById_InvalidId_ThrowsResourceNotFound() {
        assertThrows(ResourceNotFoundException.class, () ->
                jobService.getJobById(99999L, user1Email));
    }

    @Test
    void saveJob_Success_And_GetSavedJobs() {
        JobResponse saved = jobService.saveJob(job1Id, user1Email);
        assertTrue(saved.getIsSaved());

        List<JobResponse> savedList = jobService.getSavedJobs(user1Email);
        assertEquals(1, savedList.size());
        assertEquals(job1Id, savedList.get(0).getId());
    }

    @Test
    void saveJob_DuplicateSave_ThrowsDuplicateResourceException() {
        jobService.saveJob(job1Id, user1Email);

        assertThrows(DuplicateResourceException.class, () ->
                jobService.saveJob(job1Id, user1Email));
    }

    @Test
    void unsaveJob_Success() {
        jobService.saveJob(job1Id, user1Email);
        assertEquals(1, jobService.getSavedJobs(user1Email).size());

        jobService.unsaveJob(job1Id, user1Email);
        assertEquals(0, jobService.getSavedJobs(user1Email).size());
    }

    @Test
    void userIsolation_UserTwoCannotSeeUserOneSavedJobs() {
        jobService.saveJob(job1Id, user1Email);

        // User 2's saved jobs list must be empty
        List<JobResponse> user2Saved = jobService.getSavedJobs(user2Email);
        assertTrue(user2Saved.isEmpty());

        // User 2 attempting to unsave a job they didn't save must throw ResourceNotFoundException
        assertThrows(ResourceNotFoundException.class, () ->
                jobService.unsaveJob(job1Id, user2Email));
    }
}
