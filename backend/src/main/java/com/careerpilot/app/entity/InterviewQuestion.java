package com.careerpilot.app.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "interview_questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private InterviewSession session;

    @Column(nullable = false)
    private Integer questionNumber;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(nullable = false, length = 50)
    private String category; // TECHNICAL, BEHAVIORAL, HR, DSA, SYSTEM_DESIGN, PROJECT

    @Column(columnDefinition = "TEXT")
    private String expectedTopicsJson;

    @Column(nullable = false, length = 20)
    private String difficulty; // EASY, MEDIUM, HARD

    @Column(columnDefinition = "LONGTEXT")
    private String userAnswer;

    private Integer score; // 0-100

    private Integer technicalScore; // 0-100

    private Integer relevanceScore; // 0-100

    private Integer communicationScore; // 0-100

    private Integer confidenceScore; // 0-100

    @Column(columnDefinition = "LONGTEXT")
    private String aiFeedback;

    @Column(columnDefinition = "LONGTEXT")
    private String strengthsJson;

    @Column(columnDefinition = "LONGTEXT")
    private String improvementsJson;

    @Column(columnDefinition = "LONGTEXT")
    private String idealAnswer;

    private LocalDateTime answeredAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
