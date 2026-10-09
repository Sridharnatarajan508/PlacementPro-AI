package com.careerpilot.app.repository;

import com.careerpilot.app.entity.Resume;
import com.careerpilot.app.entity.ResumeAnalysis;
import com.careerpilot.app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ResumeAnalysisRepository extends JpaRepository<ResumeAnalysis, Long> {

    Optional<ResumeAnalysis> findByResume(Resume resume);

    Optional<ResumeAnalysis> findByResumeId(Long resumeId);

    @Query("SELECT ra FROM ResumeAnalysis ra WHERE ra.resume.id = :resumeId AND ra.resume.user = :user")
    Optional<ResumeAnalysis> findByResumeIdAndUser(@Param("resumeId") Long resumeId, @Param("user") User user);

    @Query("SELECT ra FROM ResumeAnalysis ra WHERE ra.resume.user = :user ORDER BY ra.id DESC")
    java.util.List<ResumeAnalysis> findByResumeUserOrderByIdDesc(@Param("user") User user);

    void deleteByResume(Resume resume);
}
