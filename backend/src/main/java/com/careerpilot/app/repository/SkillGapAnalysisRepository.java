package com.careerpilot.app.repository;

import com.careerpilot.app.entity.Job;
import com.careerpilot.app.entity.SkillGapAnalysis;
import com.careerpilot.app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SkillGapAnalysisRepository extends JpaRepository<SkillGapAnalysis, Long> {

    Optional<SkillGapAnalysis> findFirstByUserOrderByAnalyzedAtDesc(User user);

    Optional<SkillGapAnalysis> findFirstByUserAndTargetRoleIgnoreCaseOrderByAnalyzedAtDesc(User user, String targetRole);

    Optional<SkillGapAnalysis> findFirstByUserAndJobOrderByAnalyzedAtDesc(User user, Job job);

    List<SkillGapAnalysis> findByUserOrderByAnalyzedAtDesc(User user);

    Optional<SkillGapAnalysis> findByIdAndUser(Long id, User user);

    void deleteByIdAndUser(Long id, User user);
}
