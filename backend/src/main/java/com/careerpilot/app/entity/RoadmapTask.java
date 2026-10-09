package com.careerpilot.app.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "roadmap_tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoadmapTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roadmap_id", nullable = false)
    private CareerRoadmap roadmap;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 50)
    private String category; // SKILL, PROJECT, INTERVIEW, CERTIFICATION, DSA, SYSTEM_DESIGN, SOFT_SKILL

    @Column(nullable = false, length = 20)
    private String priority; // HIGH, MEDIUM, LOW

    private Integer estimatedHours;

    @Column(nullable = false)
    private Integer weekNumber;

    @Column(nullable = false)
    @Builder.Default
    private Boolean completed = false;

    private LocalDateTime completedAt;

    private String resourceUrl;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
