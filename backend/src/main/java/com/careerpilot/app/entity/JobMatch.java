package com.careerpilot.app.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "job_matches", uniqueConstraints = {
    @UniqueConstraint(name = "uk_job_match_user_job", columnNames = {"user_id", "job_id"})
}, indexes = {
    @Index(name = "idx_job_match_user", columnList = "user_id"),
    @Index(name = "idx_job_match_job", columnList = "job_id"),
    @Index(name = "idx_job_match_score", columnList = "overall_score")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(name = "overall_score", nullable = false)
    private Integer overallScore;

    @Column(name = "skill_score", nullable = false)
    private Integer skillScore;

    @Column(name = "experience_score", nullable = false)
    private Integer experienceScore;

    @Column(name = "education_score", nullable = false)
    private Integer educationScore;

    @Column(name = "matching_skills_json", columnDefinition = "TEXT")
    private String matchingSkillsJson;

    @Column(name = "missing_skills_json", columnDefinition = "TEXT")
    private String missingSkillsJson;

    @Column(name = "recommendation", nullable = false)
    private String recommendation;

    @Column(name = "ai_explanation", columnDefinition = "TEXT")
    private String aiExplanation;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
