package com.careerpilot.app.service;

import com.careerpilot.app.dto.request.JobSearchRequest;
import com.careerpilot.app.dto.response.JobResponse;
import com.careerpilot.app.entity.Job;
import com.careerpilot.app.entity.JobMatch;
import com.careerpilot.app.entity.SavedJob;
import com.careerpilot.app.entity.User;
import com.careerpilot.app.exception.DuplicateResourceException;
import com.careerpilot.app.exception.ResourceNotFoundException;
import com.careerpilot.app.repository.JobMatchRepository;
import com.careerpilot.app.repository.JobRepository;
import com.careerpilot.app.repository.SavedJobRepository;
import com.careerpilot.app.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobService {

    private final JobRepository jobRepository;
    private final SavedJobRepository savedJobRepository;
    private final JobMatchRepository jobMatchRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<JobResponse> getJobs(JobSearchRequest request, String userEmail) {
        long startTime = System.currentTimeMillis();
        log.info("[DIAGNOSTIC] GET /api/jobs started (user: {})", userEmail != null ? userEmail : "anonymous");

        Specification<Job> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
                String kw = "%" + request.getKeyword().trim().toLowerCase() + "%";
                Predicate titleMatch = cb.like(cb.lower(root.get("title")), kw);
                Predicate companyMatch = cb.like(cb.lower(root.get("company")), kw);
                Predicate descMatch = cb.like(cb.lower(root.get("description")), kw);
                Predicate skillsMatch = cb.like(cb.lower(root.get("requiredSkills")), kw);
                predicates.add(cb.or(titleMatch, companyMatch, descMatch, skillsMatch));
            }

            if (request.getLocation() != null && !request.getLocation().trim().isEmpty()) {
                String loc = "%" + request.getLocation().trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("location")), loc));
            }

            if (request.getJobType() != null && !request.getJobType().trim().isEmpty() && !request.getJobType().equalsIgnoreCase("ALL")) {
                predicates.add(cb.equal(cb.upper(root.get("jobType")), request.getJobType().trim().toUpperCase()));
            }

            if (request.getExperienceLevel() != null && !request.getExperienceLevel().trim().isEmpty() && !request.getExperienceLevel().equalsIgnoreCase("ALL")) {
                predicates.add(cb.equal(cb.upper(root.get("experienceLevel")), request.getExperienceLevel().trim().toUpperCase()));
            }

            query.orderBy(cb.desc(root.get("postedDate")), cb.desc(root.get("createdAt")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        long dbStart = System.currentTimeMillis();
        List<Job> jobs = jobRepository.findAll(spec);

        User user = (userEmail != null && !userEmail.trim().isEmpty()) ? userRepository.findByEmail(userEmail).orElse(null) : null;
        Set<Long> savedJobIds = Collections.emptySet();
        Map<Long, JobMatch> matchesMap = Collections.emptyMap();

        if (user != null) {
            List<SavedJob> saved = savedJobRepository.findByUserOrderByCreatedAtDesc(user);
            savedJobIds = saved.stream().map(s -> s.getJob().getId()).collect(Collectors.toSet());

            List<JobMatch> matches = jobMatchRepository.findByUser(user);
            matchesMap = matches.stream().collect(Collectors.toMap(m -> m.getJob().getId(), m -> m, (m1, m2) -> m1));
        }
        long dbDuration = System.currentTimeMillis() - dbStart;

        final Set<Long> finalSavedIds = savedJobIds;
        final Map<Long, JobMatch> finalMatches = matchesMap;

        List<JobResponse> responses = jobs.stream()
                .map(job -> toResponse(job, finalSavedIds.contains(job.getId()), finalMatches.get(job.getId())))
                .collect(Collectors.toList());

        if (request.getMinimumMatchScore() != null && request.getMinimumMatchScore() > 0) {
            responses = responses.stream()
                    .filter(res -> res.getMatchScore() != null && res.getMatchScore() >= request.getMinimumMatchScore())
                    .collect(Collectors.toList());
        }

        long totalTime = System.currentTimeMillis() - startTime;
        log.info("[DIAGNOSTIC] GET /api/jobs completed in {} ms (DB queries: {} ms, returned {} jobs)",
                totalTime, dbDuration, responses.size());

        return responses;
    }

    @Transactional(readOnly = true)
    public JobResponse getJobById(Long id, String userEmail) {
        long startTime = System.currentTimeMillis();
        log.info("[DIAGNOSTIC] GET /api/jobs/{} started (user: {})", id, userEmail != null ? userEmail : "anonymous");

        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", id));

        User user = (userEmail != null && !userEmail.trim().isEmpty()) ? userRepository.findByEmail(userEmail).orElse(null) : null;
        boolean isSaved = false;
        JobMatch match = null;

        if (user != null) {
            isSaved = savedJobRepository.existsByUserAndJob(user, job);
            match = jobMatchRepository.findByUserAndJob(user, job).orElse(null);
        }

        long totalTime = System.currentTimeMillis() - startTime;
        log.info("[DIAGNOSTIC] GET /api/jobs/{} completed in {} ms (Saved: {}, Matched: {})",
                id, totalTime, isSaved, match != null);

        return toResponse(job, isSaved, match);
    }

    @Transactional
    public JobResponse saveJob(Long jobId, String userEmail) {
        long startTime = System.currentTimeMillis();
        log.info("[DIAGNOSTIC] POST /api/jobs/{}/save started for user {}", jobId, userEmail);

        User user = findUserByEmail(userEmail);
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", jobId));

        if (savedJobRepository.existsByUserAndJob(user, job)) {
            throw new DuplicateResourceException("Job is already saved");
        }

        SavedJob savedJob = SavedJob.builder()
                .user(user)
                .job(job)
                .build();

        savedJobRepository.save(savedJob);
        JobMatch match = jobMatchRepository.findByUserAndJob(user, job).orElse(null);

        long totalTime = System.currentTimeMillis() - startTime;
        log.info("[DIAGNOSTIC] POST /api/jobs/{}/save completed in {} ms (user: {})",
                jobId, totalTime, userEmail);

        return toResponse(job, true, match);
    }

    @Transactional
    public void unsaveJob(Long jobId, String userEmail) {
        long startTime = System.currentTimeMillis();
        log.info("[DIAGNOSTIC] DELETE /api/jobs/{}/save started for user {}", jobId, userEmail);

        User user = findUserByEmail(userEmail);
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", jobId));

        SavedJob savedJob = savedJobRepository.findByUserAndJob(user, job)
                .orElseThrow(() -> new ResourceNotFoundException("SavedJob", "jobId", jobId));

        savedJobRepository.delete(savedJob);

        long totalTime = System.currentTimeMillis() - startTime;
        log.info("[DIAGNOSTIC] DELETE /api/jobs/{}/save completed in {} ms (user: {})",
                jobId, totalTime, userEmail);
    }

    @Transactional(readOnly = true)
    public List<JobResponse> getSavedJobs(String userEmail) {
        long startTime = System.currentTimeMillis();
        log.info("[DIAGNOSTIC] GET /api/jobs/saved started for user {}", userEmail);

        long dbStart = System.currentTimeMillis();
        User user = findUserByEmail(userEmail);
        List<SavedJob> savedJobs = savedJobRepository.findByUserOrderByCreatedAtDesc(user);

        List<JobMatch> matches = jobMatchRepository.findByUser(user);
        Map<Long, JobMatch> matchesMap = matches.stream()
                .collect(Collectors.toMap(m -> m.getJob().getId(), m -> m, (m1, m2) -> m1));
        long dbDuration = System.currentTimeMillis() - dbStart;

        List<JobResponse> responses = savedJobs.stream()
                .map(s -> toResponse(s.getJob(), true, matchesMap.get(s.getJob().getId())))
                .collect(Collectors.toList());

        long totalTime = System.currentTimeMillis() - startTime;
        log.info("[DIAGNOSTIC] GET /api/jobs/saved completed in {} ms (DB queries: {} ms, returned {} saved jobs)",
                totalTime, dbDuration, responses.size());

        return responses;
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    public JobResponse toResponse(Job job, boolean isSaved, JobMatch match) {
        return JobResponse.builder()
                .id(job.getId())
                .title(job.getTitle())
                .company(job.getCompany())
                .location(job.getLocation())
                .jobType(job.getJobType())
                .experienceLevel(job.getExperienceLevel())
                .salaryMin(job.getSalaryMin())
                .salaryMax(job.getSalaryMax())
                .description(job.getDescription())
                .requiredSkills(job.getRequiredSkills())
                .preferredSkills(job.getPreferredSkills())
                .applicationUrl(job.getApplicationUrl())
                .postedDate(job.getPostedDate())
                .createdAt(job.getCreatedAt())
                .isSaved(isSaved)
                .matchScore(match != null ? match.getOverallScore() : null)
                .matchRecommendation(match != null ? match.getRecommendation() : null)
                .build();
    }
}
