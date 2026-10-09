package com.careerpilot.app.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "interview_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String targetRole;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_job_id")
    private Job targetJob;

    @Column(nullable = false, length = 30)
    private String interviewType; // TECHNICAL, BEHAVIORAL, HR, MIXED

    @Column(nullable = false, length = 20)
    private String difficulty; // EASY, MEDIUM, HARD

    @Column(nullable = false)
    private Integer totalQuestions;

    @Column(nullable = false)
    @Builder.Default
    private Integer completedQuestions = 0;

    private Integer overallScore; // 0-100

    private Integer technicalScore; // 0-100

    private Integer communicationScore; // 0-100

    private Integer confidenceScore; // 0-100

    private Integer problemSolvingScore; // 0-100

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "IN_PROGRESS"; // IN_PROGRESS, COMPLETED, ABANDONED

    @Column(columnDefinition = "LONGTEXT")
    private String aiSummary;

    @Column(columnDefinition = "LONGTEXT")
    private String strengthsJson;

    @Column(columnDefinition = "LONGTEXT")
    private String weaknessesJson;

    @Column(columnDefinition = "LONGTEXT")
    private String recommendationsJson;

    @CreationTimestamp
    @Column(name = "started_at", updatable = false)
    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InterviewQuestion> questions = new ArrayList<>();

    public void addQuestion(InterviewQuestion question) {
        questions.add(question);
        question.setSession(this);
    }

    public void removeQuestion(InterviewQuestion question) {
        questions.remove(question);
        question.setSession(null);
    }
}
