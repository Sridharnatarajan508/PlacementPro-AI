package com.careerpilot.app.repository;

import com.careerpilot.app.entity.Job;
import com.careerpilot.app.entity.SavedJob;
import com.careerpilot.app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedJobRepository extends JpaRepository<SavedJob, Long> {

    boolean existsByUserAndJob(User user, Job job);

    Optional<SavedJob> findByUserAndJob(User user, Job job);

    List<SavedJob> findByUserOrderByCreatedAtDesc(User user);

    void deleteByUserAndJob(User user, Job job);
}
