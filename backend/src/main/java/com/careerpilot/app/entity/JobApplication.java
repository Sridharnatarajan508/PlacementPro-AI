package com.careerpilot.app.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "job_applications",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_job", columnNames = {"user_id", "job_id"})
    },
    indexes = {
        @Index(name = "idx_job_app_user", columnList = "user_id"),
        @Index(name = "idx_job_app_status", columnList = "status"),
        @Index(name = "idx_job_app_updated", columnList = "updated_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id")
    private Job job;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.SAVED;

    @Column(name = "application_date")
    private LocalDate applicationDate;

    @Column(name = "deadline")
    private LocalDate deadline;

    @Column(name = "next_follow_up_date")
    private LocalDate nextFollowUpDate;

    @Column(name = "interview_date")
    private LocalDateTime interviewDate;

    @Column(length = 200)
    private String company;

    @Column(name = "job_title", length = 200)
    private String jobTitle;

    @Column(name = "job_url", length = 500)
    private String jobUrl;

    @Column(length = 150)
    private String location;

    @Column(columnDefinition = "LONGTEXT")
    private String notes;

    @Column(name = "recruiter_name", length = 150)
    private String recruiterName;

    @Column(name = "recruiter_email", length = 150)
    private String recruiterEmail;

    @Column(length = 100)
    @Builder.Default
    private String source = "CAREER_COPILOT";

    @Column(name = "ai_analysis_json", columnDefinition = "LONGTEXT")
    private String aiAnalysisJson;

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("createdAt DESC")
    @Builder.Default
    private List<ApplicationActivity> activities = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void addActivity(ApplicationActivity activity) {
        if (this.activities == null) {
            this.activities = new ArrayList<>();
        }
        this.activities.add(activity);
        activity.setApplication(this);
    }
}
