package com.careerpilot.app.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "resume_analyses", indexes = {
    @Index(name = "idx_analysis_resume_id", columnList = "resume_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resume_id", nullable = false, unique = true)
    private Resume resume;

    @Column(name = "overall_score", nullable = false)
    private Integer overallScore;

    @Column(name = "ats_score", nullable = false)
    private Integer atsScore;

    @Column(name = "target_role")
    private String targetRole;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "strengths_json", columnDefinition = "LONGTEXT")
    private String strengthsJson;

    @Column(name = "weaknesses_json", columnDefinition = "LONGTEXT")
    private String weaknessesJson;

    @Column(name = "detected_skills_json", columnDefinition = "LONGTEXT")
    private String detectedSkillsJson;

    @Column(name = "missing_skills_json", columnDefinition = "LONGTEXT")
    private String missingSkillsJson;

    @Column(name = "suggestions_json", columnDefinition = "LONGTEXT")
    private String suggestionsJson;

    @Column(name = "section_analysis_json", columnDefinition = "LONGTEXT")
    private String sectionAnalysisJson;

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
