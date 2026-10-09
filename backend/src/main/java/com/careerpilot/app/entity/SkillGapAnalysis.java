package com.careerpilot.app.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "skill_gap_analyses", indexes = {
    @Index(name = "idx_skill_gap_user", columnList = "user_id"),
    @Index(name = "idx_skill_gap_role", columnList = "target_role"),
    @Index(name = "idx_skill_gap_job", columnList = "job_id"),
    @Index(name = "idx_skill_gap_analyzed_at", columnList = "analyzed_at")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkillGapAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id")
    private Job job;

    @Column(name = "target_role", nullable = false)
    private String targetRole;

    @Column(name = "readiness_score", nullable = false)
    private Integer readinessScore;

    @Column(name = "skill_coverage_score", nullable = false)
    private Integer skillCoverageScore;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "current_skills_json", columnDefinition = "LONGTEXT")
    private String currentSkillsJson;

    @Column(name = "required_skills_json", columnDefinition = "LONGTEXT")
    private String requiredSkillsJson;

    @Column(name = "missing_skills_json", columnDefinition = "LONGTEXT")
    private String missingSkillsJson;

    @Column(name = "skill_gaps_json", columnDefinition = "LONGTEXT")
    private String skillGapsJson;

    @Column(name = "learning_plan_json", columnDefinition = "LONGTEXT")
    private String learningPlanJson;

    @Column(name = "raw_analysis_json", columnDefinition = "LONGTEXT")
    private String rawAnalysisJson;

    @Column(name = "analyzed_at", nullable = false)
    private LocalDateTime analyzedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
