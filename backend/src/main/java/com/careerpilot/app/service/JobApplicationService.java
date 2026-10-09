package com.careerpilot.app.service;

import com.careerpilot.app.dto.ApplicationActivityDto;
import com.careerpilot.app.dto.ApplicationStatsDto;
import com.careerpilot.app.dto.JobApplicationDto;
import com.careerpilot.app.dto.UpcomingTasksDto;
import com.careerpilot.app.entity.*;
import com.careerpilot.app.exception.ResourceNotFoundException;
import com.careerpilot.app.exception.UnauthorizedChatAccessException;
import com.careerpilot.app.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobApplicationService {

    private final JobApplicationRepository jobApplicationRepository;
    private final ApplicationActivityRepository applicationActivityRepository;
    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final JobMatchRepository jobMatchRepository;

    @Transactional
    public JobApplicationDto createApplication(Long jobId, String userEmail) {
        User user = getUserByEmail(userEmail);
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with ID: " + jobId));

        if (jobApplicationRepository.existsByUserAndJob(user, job)) {
            throw new IllegalStateException("This job is already being tracked in your application pipeline.");
        }

        JobApplication application = JobApplication.builder()
                .user(user)
                .job(job)
                .status(ApplicationStatus.SAVED)
                .company(job.getCompany())
                .jobTitle(job.getTitle())
                .location(job.getLocation())
                .jobUrl(null)
                .source("CAREER_COPILOT")
                .applicationDate(LocalDate.now())
                .build();

        application = jobApplicationRepository.save(application);

        ApplicationActivity activity = ApplicationActivity.builder()
                .application(application)
                .activityType(ApplicationActivityType.CREATED)
                .description("Tracked opportunity for " + job.getTitle() + " at " + job.getCompany() + ".")
                .activityDate(LocalDateTime.now())
                .build();
        applicationActivityRepository.save(activity);

        log.info("User {} tracked job ID {} as application ID {}", userEmail, jobId, application.getId());
        return mapToDto(application, user);
    }

    @Transactional
    public JobApplicationDto createCustomApplication(JobApplicationDto dto, String userEmail) {
        if (dto.getCompany() == null || dto.getCompany().isBlank()) {
            throw new IllegalArgumentException("Company name is required.");
        }
        if (dto.getJobTitle() == null || dto.getJobTitle().isBlank()) {
            throw new IllegalArgumentException("Job title is required.");
        }

        User user = getUserByEmail(userEmail);
        ApplicationStatus initialStatus = dto.getStatus() != null ? dto.getStatus() : ApplicationStatus.SAVED;

        JobApplication application = JobApplication.builder()
                .user(user)
                .job(null)
                .status(initialStatus)
                .company(dto.getCompany().trim())
                .jobTitle(dto.getJobTitle().trim())
                .location(dto.getLocation() != null ? dto.getLocation().trim() : null)
                .jobUrl(dto.getJobUrl() != null ? dto.getJobUrl().trim() : null)
                .recruiterName(dto.getRecruiterName())
                .recruiterEmail(dto.getRecruiterEmail())
                .source(dto.getSource() != null ? dto.getSource() : "MANUAL_ENTRY")
                .notes(dto.getNotes())
                .applicationDate(dto.getApplicationDate() != null ? dto.getApplicationDate() : LocalDate.now())
                .deadline(dto.getDeadline())
                .nextFollowUpDate(dto.getNextFollowUpDate())
                .interviewDate(dto.getInterviewDate())
                .build();

        application = jobApplicationRepository.save(application);

        ApplicationActivity activity = ApplicationActivity.builder()
                .application(application)
                .activityType(ApplicationActivityType.CREATED)
                .description("Created custom application for " + application.getJobTitle() + " at " + application.getCompany() + ".")
                .activityDate(LocalDateTime.now())
                .build();
        applicationActivityRepository.save(activity);

        log.info("User {} created custom application ID {}", userEmail, application.getId());
        return mapToDto(application, user);
    }

    @Transactional
    public JobApplicationDto updateApplication(Long applicationId, JobApplicationDto dto, String userEmail) {
        User user = getUserByEmail(userEmail);
        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Job application not found with ID: " + applicationId));

        if (!application.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedChatAccessException("You do not have permission to modify this application.");
        }

        // 1. Check for Status Change
        if (dto.getStatus() != null && dto.getStatus() != application.getStatus()) {
            ApplicationStatus oldStatus = application.getStatus();
            ApplicationStatus newStatus = dto.getStatus();
            application.setStatus(newStatus);

            ApplicationActivityType actType = resolveActivityTypeForStatus(newStatus);
            String desc = "Status changed from " + oldStatus + " to " + newStatus + ".";
            ApplicationActivity act = ApplicationActivity.builder()
                    .application(application)
                    .activityType(actType)
                    .description(desc)
                    .activityDate(LocalDateTime.now())
                    .build();
            applicationActivityRepository.save(act);
        }

        // 2. Check for Interview Scheduling
        if (dto.getInterviewDate() != null && !Objects.equals(dto.getInterviewDate(), application.getInterviewDate())) {
            ApplicationActivity act = ApplicationActivity.builder()
                    .application(application)
                    .activityType(ApplicationActivityType.INTERVIEW_SCHEDULED)
                    .description("Interview scheduled for " + dto.getInterviewDate() + ".")
                    .activityDate(LocalDateTime.now())
                    .build();
            applicationActivityRepository.save(act);
        }

        // 3. Check for Follow-Up Date Update
        if (dto.getNextFollowUpDate() != null && !Objects.equals(dto.getNextFollowUpDate(), application.getNextFollowUpDate())) {
            ApplicationActivity act = ApplicationActivity.builder()
                    .application(application)
                    .activityType(ApplicationActivityType.FOLLOW_UP)
                    .description("Follow-up reminder set for " + dto.getNextFollowUpDate() + ".")
                    .activityDate(LocalDateTime.now())
                    .build();
            applicationActivityRepository.save(act);
        }

        // 4. Check for Deadline Update
        if (dto.getDeadline() != null && !Objects.equals(dto.getDeadline(), application.getDeadline())) {
            ApplicationActivity act = ApplicationActivity.builder()
                    .application(application)
                    .activityType(ApplicationActivityType.DEADLINE_UPDATED)
                    .description("Application deadline updated to " + dto.getDeadline() + ".")
                    .activityDate(LocalDateTime.now())
                    .build();
            applicationActivityRepository.save(act);
        }

        // 5. Check for Note Updates
        if (dto.getNotes() != null && !Objects.equals(dto.getNotes(), application.getNotes()) && !dto.getNotes().isBlank()) {
            ApplicationActivity act = ApplicationActivity.builder()
                    .application(application)
                    .activityType(ApplicationActivityType.NOTE_ADDED)
                    .description("Updated notes: " + (dto.getNotes().length() > 60 ? dto.getNotes().substring(0, 57) + "..." : dto.getNotes()))
                    .activityDate(LocalDateTime.now())
                    .build();
            applicationActivityRepository.save(act);
        }

        // Update fields
        if (dto.getCompany() != null && !dto.getCompany().isBlank()) application.setCompany(dto.getCompany().trim());
        if (dto.getJobTitle() != null && !dto.getJobTitle().isBlank()) application.setJobTitle(dto.getJobTitle().trim());
        if (dto.getLocation() != null) application.setLocation(dto.getLocation().trim());
        if (dto.getJobUrl() != null) application.setJobUrl(dto.getJobUrl().trim());
        if (dto.getRecruiterName() != null) application.setRecruiterName(dto.getRecruiterName().trim());
        if (dto.getRecruiterEmail() != null) application.setRecruiterEmail(dto.getRecruiterEmail().trim());
        if (dto.getSource() != null) application.setSource(dto.getSource().trim());
        if (dto.getNotes() != null) application.setNotes(dto.getNotes());
        if (dto.getApplicationDate() != null) application.setApplicationDate(dto.getApplicationDate());
        application.setDeadline(dto.getDeadline());
        application.setNextFollowUpDate(dto.getNextFollowUpDate());
        application.setInterviewDate(dto.getInterviewDate());

        application.setUpdatedAt(LocalDateTime.now());
        application = jobApplicationRepository.save(application);

        log.info("User {} updated application ID {}", userEmail, applicationId);
        return mapToDto(application, user);
    }

    @Transactional
    public JobApplicationDto updateStatus(Long applicationId, ApplicationStatus newStatus, String userEmail) {
        User user = getUserByEmail(userEmail);
        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Job application not found with ID: " + applicationId));

        if (!application.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedChatAccessException("You do not have permission to modify this application.");
        }

        ApplicationStatus oldStatus = application.getStatus();
        if (oldStatus != newStatus) {
            application.setStatus(newStatus);
            application.setUpdatedAt(LocalDateTime.now());
            application = jobApplicationRepository.save(application);

            ApplicationActivityType actType = resolveActivityTypeForStatus(newStatus);
            String desc = "Status changed from " + oldStatus + " to " + newStatus + ".";
            ApplicationActivity act = ApplicationActivity.builder()
                    .application(application)
                    .activityType(actType)
                    .description(desc)
                    .activityDate(LocalDateTime.now())
                    .build();
            applicationActivityRepository.save(act);

            log.info("User {} changed status of application ID {} from {} to {}", userEmail, applicationId, oldStatus, newStatus);
        }

        return mapToDto(application, user);
    }

    @Transactional
    public void deleteApplication(Long applicationId, String userEmail) {
        User user = getUserByEmail(userEmail);
        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Job application not found with ID: " + applicationId));

        if (!application.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedChatAccessException("You do not have permission to delete this application.");
        }

        jobApplicationRepository.delete(application);
        log.info("User {} deleted application ID {}", userEmail, applicationId);
    }

    @Transactional(readOnly = true)
    public JobApplicationDto getApplication(Long applicationId, String userEmail) {
        User user = getUserByEmail(userEmail);
        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Job application not found with ID: " + applicationId));

        if (!application.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedChatAccessException("You do not have permission to view this application.");
        }

        return mapToDto(application, user);
    }

    @Transactional(readOnly = true)
    public List<JobApplicationDto> getAllApplications(String userEmail) {
        User user = getUserByEmail(userEmail);
        List<JobApplication> list = jobApplicationRepository.findByUserOrderByUpdatedAtDesc(user);
        return list.stream().map(app -> mapToDto(app, user)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<JobApplicationDto> getApplicationsByStatus(ApplicationStatus status, String userEmail) {
        User user = getUserByEmail(userEmail);
        List<JobApplication> list = jobApplicationRepository.findByUserAndStatusOrderByUpdatedAtDesc(user, status);
        return list.stream().map(app -> mapToDto(app, user)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ApplicationStatsDto getStatistics(String userEmail) {
        User user = getUserByEmail(userEmail);
        List<JobApplication> all = jobApplicationRepository.findByUserOrderByUpdatedAtDesc(user);

        long total = all.size();
        long saved = 0;
        long applied = 0;
        long screening = 0;
        long interview = 0;
        long offer = 0;
        long rejected = 0;
        long withdrawn = 0;

        for (JobApplication a : all) {
            switch (a.getStatus()) {
                case SAVED -> saved++;
                case APPLIED -> applied++;
                case SCREENING -> screening++;
                case INTERVIEW -> interview++;
                case OFFER -> offer++;
                case REJECTED -> rejected++;
                case WITHDRAWN -> withdrawn++;
            }
        }

        long active = applied + screening + interview;
        long totalApplied = total - saved;

        double responseRate = totalApplied > 0 ? ((double) (screening + interview + offer) * 100.0 / totalApplied) : 0.0;
        double interviewRate = totalApplied > 0 ? ((double) (interview + offer) * 100.0 / totalApplied) : 0.0;
        double offerRate = totalApplied > 0 ? ((double) offer * 100.0 / totalApplied) : 0.0;

        return ApplicationStatsDto.builder()
                .totalApplications(total)
                .savedCount(saved)
                .appliedCount(applied)
                .screeningCount(screening)
                .interviewCount(interview)
                .offerCount(offer)
                .rejectedCount(rejected)
                .withdrawnCount(withdrawn)
                .activeCount(active)
                .responseRate(Math.round(responseRate * 10.0) / 10.0)
                .interviewRate(Math.round(interviewRate * 10.0) / 10.0)
                .offerRate(Math.round(offerRate * 10.0) / 10.0)
                .build();
    }

    @Transactional(readOnly = true)
    public UpcomingTasksDto getUpcomingTasks(String userEmail) {
        User user = getUserByEmail(userEmail);

        List<JobApplicationDto> followUps = jobApplicationRepository
                .findByUserAndNextFollowUpDateIsNotNullOrderByNextFollowUpDateAsc(user)
                .stream().map(a -> mapToDto(a, user)).collect(Collectors.toList());

        List<JobApplicationDto> interviews = jobApplicationRepository
                .findByUserAndInterviewDateIsNotNullOrderByInterviewDateAsc(user)
                .stream().map(a -> mapToDto(a, user)).collect(Collectors.toList());

        List<JobApplicationDto> deadlines = jobApplicationRepository
                .findByUserAndDeadlineIsNotNullOrderByDeadlineAsc(user)
                .stream().map(a -> mapToDto(a, user)).collect(Collectors.toList());

        return UpcomingTasksDto.builder()
                .upcomingFollowUps(followUps)
                .upcomingInterviews(interviews)
                .approachingDeadlines(deadlines)
                .build();
    }

    private ApplicationActivityType resolveActivityTypeForStatus(ApplicationStatus status) {
        return switch (status) {
            case OFFER -> ApplicationActivityType.OFFER_RECEIVED;
            case REJECTED -> ApplicationActivityType.REJECTED;
            case WITHDRAWN -> ApplicationActivityType.WITHDRAWN;
            case INTERVIEW -> ApplicationActivityType.INTERVIEW_SCHEDULED;
            default -> ApplicationActivityType.STATUS_CHANGED;
        };
    }

    private JobApplicationDto mapToDto(JobApplication app, User user) {
        Integer matchScore = null;
        String matchRecommendation = null;

        if (app.getJob() != null) {
            Optional<JobMatch> jm = jobMatchRepository.findByUserAndJob(user, app.getJob());
            if (jm.isPresent()) {
                matchScore = jm.get().getOverallScore();
                matchRecommendation = jm.get().getRecommendation();
            }
        }

        List<ApplicationActivity> activities = applicationActivityRepository.findByApplicationOrderByCreatedAtDesc(app);
        List<ApplicationActivityDto> timeline = activities.stream().map(a -> ApplicationActivityDto.builder()
                .id(a.getId())
                .activityType(a.getActivityType())
                .description(a.getDescription())
                .activityDate(a.getActivityDate() != null ? a.getActivityDate() : a.getCreatedAt())
                .createdAt(a.getCreatedAt())
                .build()).collect(Collectors.toList());

        return JobApplicationDto.builder()
                .id(app.getId())
                .jobId(app.getJob() != null ? app.getJob().getId() : null)
                .company(app.getCompany())
                .jobTitle(app.getJobTitle())
                .status(app.getStatus())
                .applicationDate(app.getApplicationDate())
                .deadline(app.getDeadline())
                .nextFollowUpDate(app.getNextFollowUpDate())
                .interviewDate(app.getInterviewDate())
                .jobUrl(app.getJobUrl())
                .location(app.getLocation())
                .recruiterName(app.getRecruiterName())
                .recruiterEmail(app.getRecruiterEmail())
                .source(app.getSource())
                .notes(app.getNotes())
                .matchScore(matchScore)
                .matchRecommendation(matchRecommendation)
                .activityTimeline(timeline)
                .createdAt(app.getCreatedAt())
                .updatedAt(app.getUpdatedAt())
                .build();
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }
}
