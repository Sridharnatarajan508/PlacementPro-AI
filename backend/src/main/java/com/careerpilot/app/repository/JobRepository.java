package com.careerpilot.app.repository;

import com.careerpilot.app.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {

    List<Job> findTop15ByOrderByPostedDateDescCreatedAtDesc();

    List<Job> findTop20ByOrderByPostedDateDescCreatedAtDesc();

    List<Job> findByTitleContainingIgnoreCaseOrRequiredSkillsContainingIgnoreCase(String title, String skills);
}
