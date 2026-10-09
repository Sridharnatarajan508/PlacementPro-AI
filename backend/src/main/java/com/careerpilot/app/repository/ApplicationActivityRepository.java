package com.careerpilot.app.repository;

import com.careerpilot.app.entity.ApplicationActivity;
import com.careerpilot.app.entity.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationActivityRepository extends JpaRepository<ApplicationActivity, Long> {

    List<ApplicationActivity> findByApplicationOrderByActivityDateDesc(JobApplication application);

    List<ApplicationActivity> findByApplicationOrderByCreatedAtDesc(JobApplication application);

    void deleteByApplication(JobApplication application);
}
