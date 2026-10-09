package com.careerpilot.app.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "job_preparations",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_job_prep_user_job", columnNames = {"user_id", "job_id"})
    },
    indexes = {
        @Index(name = "idx_job_prep_user", columnList = "user_id"),
        @Index(name = "idx_job_prep_job", columnList = "job_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobPreparation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(nullable = false)
    @Builder.Default
    private Integer readinessScore = 0; // 0 to 100

    @Column(length = 30)
    @Builder.Default
    private String preparationLevel = "INTERMEDIATE"; // BEGINNER, INTERMEDIATE, ADVANCED

    @Column(columnDefinition = "LONGTEXT")
    private String summary;

    @Column(columnDefinition = "LONGTEXT")
    private String strengthsJson;

    @Column(columnDefinition = "LONGTEXT")
    private String skillGapsJson;

    @Column(columnDefinition = "LONGTEXT")
    private String technicalTopicsJson;

    @Column(columnDefinition = "LONGTEXT")
    private String interviewTopicsJson;

    @Column(columnDefinition = "LONGTEXT")
    private String codingTopicsJson;

    @Column(columnDefinition = "LONGTEXT")
    private String projectTopicsJson;

    @Column(columnDefinition = "LONGTEXT")
    private String behavioralTopicsJson;

    @Column(columnDefinition = "LONGTEXT")
    private String preparationPlanJson;

    @Column(columnDefinition = "LONGTEXT")
    private String recommendedQuestionsJson;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
