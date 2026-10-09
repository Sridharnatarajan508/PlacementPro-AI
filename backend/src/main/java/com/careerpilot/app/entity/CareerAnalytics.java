package com.careerpilot.app.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "career_analytics",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_career_analytics_user", columnNames = {"user_id"})
    },
    indexes = {
        @Index(name = "idx_career_analytics_user", columnList = "user_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareerAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    @Builder.Default
    private Integer overallReadinessScore = 0; // 0 to 100

    @Column
    private Integer previousReadinessScore;

    @Column(length = 50, nullable = false)
    @Builder.Default
    private String readinessLevel = "NEEDS PREPARATION"; // READY, ALMOST READY, NEEDS IMPROVEMENT, NEEDS PREPARATION

    @Column(columnDefinition = "LONGTEXT")
    private String careerSummary;

    @Column(columnDefinition = "LONGTEXT")
    private String strengthsJson;

    @Column(columnDefinition = "LONGTEXT")
    private String weaknessesJson;

    @Column(columnDefinition = "LONGTEXT")
    private String skillProgressJson;

    @Column(columnDefinition = "LONGTEXT")
    private String interviewProgressJson;

    @Column(columnDefinition = "LONGTEXT")
    private String jobSearchProgressJson;

    @Column(columnDefinition = "LONGTEXT")
    private String roadmapProgressJson;

    @Column(columnDefinition = "LONGTEXT")
    private String recommendationsJson;

    @Column(columnDefinition = "LONGTEXT")
    private String weeklyGoalsJson;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime generatedAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
