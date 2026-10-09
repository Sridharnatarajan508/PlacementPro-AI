package com.careerpilot.app.repository;

import com.careerpilot.app.entity.Job;
import com.careerpilot.app.entity.JobPreparation;
import com.careerpilot.app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobPreparationRepository extends JpaRepository<JobPreparation, Long> {

    Optional<JobPreparation> findByUserAndJob(User user, Job job);

    Optional<JobPreparation> findByUserAndJobId(User user, Long jobId);

    List<JobPreparation> findByUserOrderByUpdatedAtDesc(User user);

    void deleteByUserAndJob(User user, Job job);
}
