package com.careerpilot.app.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "learning_recommendations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LearningRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String skill;

    @Column(nullable = false, length = 20)
    private String priority; // HIGH, MEDIUM, LOW

    @Column(columnDefinition = "LONGTEXT")
    private String reason;

    @Column(columnDefinition = "LONGTEXT")
    private String learningObjective;

    @Column(columnDefinition = "LONGTEXT")
    private String recommendedTopicsJson;

    @Column(columnDefinition = "LONGTEXT")
    private String projectIdea;

    @Column(length = 100)
    private String estimatedEffort;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
