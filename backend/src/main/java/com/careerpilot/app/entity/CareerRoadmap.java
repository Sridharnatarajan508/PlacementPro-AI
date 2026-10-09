package com.careerpilot.app.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "career_roadmaps")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareerRoadmap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String targetRole;

    private String currentLevel;

    @Column(nullable = false)
    @Builder.Default
    private Integer overallReadinessScore = 0; // 0 to 100

    @Column(columnDefinition = "LONGTEXT")
    private String summary;

    @Column(columnDefinition = "LONGTEXT")
    private String currentSkillsJson;

    @Column(columnDefinition = "LONGTEXT")
    private String skillGapsJson;

    @Column(columnDefinition = "LONGTEXT")
    private String recommendedSkillsJson;

    @Column(columnDefinition = "LONGTEXT")
    private String roadmapJson;

    @Column(columnDefinition = "LONGTEXT")
    private String projectRecommendationsJson;

    @Column(columnDefinition = "LONGTEXT")
    private String jobReadinessSummary;

    // Optional helper fields for backwards compatibility
    @Column(nullable = false)
    @Builder.Default
    private Integer overallProgress = 0; // 0 to 100

    @Column(columnDefinition = "LONGTEXT")
    private String aiSummary;

    private Integer estimatedDurationWeeks;

    @OneToMany(mappedBy = "roadmap", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RoadmapTask> tasks = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void addTask(RoadmapTask task) {
        tasks.add(task);
        task.setRoadmap(this);
    }

    public void removeTask(RoadmapTask task) {
        tasks.remove(task);
        task.setRoadmap(null);
    }
}
