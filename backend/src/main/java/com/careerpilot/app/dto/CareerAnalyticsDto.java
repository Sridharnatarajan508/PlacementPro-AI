package com.careerpilot.app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CareerAnalyticsDto {

    @Builder.Default
    private Integer overallReadinessScore = 0;

    @Builder.Default
    private String readinessLevel = "NEEDS PREPARATION"; // READY, ALMOST READY, NEEDS IMPROVEMENT, NEEDS PREPARATION

    private String careerSummary;

    @Builder.Default
    private List<String> strengths = new ArrayList<>();

    @Builder.Default
    private List<WeaknessItem> weaknesses = new ArrayList<>();

    @Builder.Default
    private List<SkillProgressItem> skillProgress = new ArrayList<>();

    @Builder.Default
    private InterviewProgressSummary interviewProgress = new InterviewProgressSummary();

    @Builder.Default
    private JobSearchProgressSummary jobSearchProgress = new JobSearchProgressSummary();

    @Builder.Default
    private RoadmapProgressSummary roadmapProgress = new RoadmapProgressSummary();

    @Builder.Default
    private List<RecommendationItem> recommendations = new ArrayList<>();

    @Builder.Default
    private List<WeeklyGoalItem> weeklyGoals = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WeaknessItem {
        private String skill;
        private String currentStatus;
        @Builder.Default
        private String priority = "MEDIUM"; // HIGH, MEDIUM, LOW
        private String recommendedAction;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SkillProgressItem {
        private String skill;
        @Builder.Default
        private String category = "TECHNICAL";
        @Builder.Default
        private String status = "DEVELOPING"; // STRONG, DEVELOPING, MISSING
        @Builder.Default
        private Integer proficiencyPercentage = 50;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InterviewProgressSummary {
        @Builder.Default
        private Double averageScore = 0.0;
        @Builder.Default
        private Double technicalScore = 0.0;
        @Builder.Default
        private Double behavioralScore = 0.0;
        @Builder.Default
        private Double communicationScore = 0.0;
        @Builder.Default
        private Integer totalInterviews = 0;
        @Builder.Default
        private String trend = "NOT_STARTED"; // IMPROVING, STABLE, NEEDS_PRACTICE, NOT_STARTED
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JobSearchProgressSummary {
        @Builder.Default
        private Integer jobsMatched = 0;
        @Builder.Default
        private Integer savedJobs = 0;
        @Builder.Default
        private Integer appliedJobs = 0;
        @Builder.Default
        private Integer preparationSessions = 0;
        @Builder.Default
        private Double avgMatchScore = 0.0;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RoadmapProgressSummary {
        @Builder.Default
        private Integer completedMilestones = 0;
        @Builder.Default
        private Integer totalMilestones = 0;
        private String currentMilestone;
        private String upcomingMilestone;
        @Builder.Default
        private Integer completionPercentage = 0;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RecommendationItem {
        @Builder.Default
        private String priority = "MEDIUM"; // HIGH, MEDIUM, LOW
        private String action;
        private String reason;
        @Builder.Default
        private String relatedFeature = "CAREER_ROADMAP"; // SKILL_GAP, CAREER_ROADMAP, MOCK_INTERVIEW, JOB_PREPARATION, RESUME_ANALYZER, JOB_MATCHER
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WeeklyGoalItem {
        private String day; // e.g. "Day 1", "Day 2", etc.
        private String title;
        private String description;
        private String focusArea;
    }
}
