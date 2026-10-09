package com.careerpilot.app.repository;

import com.careerpilot.app.entity.Job;
import com.careerpilot.app.entity.JobMatch;
import com.careerpilot.app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobMatchRepository extends JpaRepository<JobMatch, Long> {

    Optional<JobMatch> findByUserAndJob(User user, Job job);

    Optional<JobMatch> findByUserIdAndJobId(Long userId, Long jobId);

    List<JobMatch> findByUser(User user);

    List<JobMatch> findByUserOrderByOverallScoreDesc(User user);

    void deleteByUserAndJob(User user, Job job);
}
