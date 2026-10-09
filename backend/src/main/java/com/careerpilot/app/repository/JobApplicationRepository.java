package com.careerpilot.app.repository;

import com.careerpilot.app.entity.ApplicationStatus;
import com.careerpilot.app.entity.Job;
import com.careerpilot.app.entity.JobApplication;
import com.careerpilot.app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    List<JobApplication> findByUserOrderByUpdatedAtDesc(User user);

    Optional<JobApplication> findByIdAndUser(Long id, User user);

    Optional<JobApplication> findByUserAndJob(User user, Job job);

    boolean existsByUserAndJob(User user, Job job);

    List<JobApplication> findByUserAndStatusOrderByUpdatedAtDesc(User user, ApplicationStatus status);

    List<JobApplication> findByUserAndNextFollowUpDateIsNotNullOrderByNextFollowUpDateAsc(User user);

    List<JobApplication> findByUserAndInterviewDateIsNotNullOrderByInterviewDateAsc(User user);

    List<JobApplication> findByUserAndDeadlineIsNotNullOrderByDeadlineAsc(User user);

    long countByUser(User user);

    long countByUserAndStatus(User user, ApplicationStatus status);

    void deleteByUser(User user);
}
