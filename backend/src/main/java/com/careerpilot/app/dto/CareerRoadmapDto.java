package com.careerpilot.app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CareerRoadmapDto {

    private String targetRole;
    private String currentLevel;
    private Integer overallReadinessScore;
    private String summary;
    private List<String> currentSkills;
    private List<String> skillGaps;
    private List<String> recommendedSkills;
    private List<RoadmapPhaseDto> roadmap;
    private List<String> projectRecommendations;
    private String jobReadinessSummary;
    private List<LearningRecommendationDto> learningRecommendations;

    // Optional legacy helper fields
    private Integer estimatedDurationWeeks;
    private List<SkillRecommendationDto> skillsToLearn;
    private List<RoadmapProjectDto> projects;
    private List<InterviewTopicDto> interviewTopics;
    private List<CertificationDto> certifications;
    private List<WeeklyPlanDto> weeklyPlan;
    private List<RoadmapMilestoneDto> milestones;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RoadmapPhaseDto {
        private Integer phase;
        private String title;
        private List<String> skills;
        private List<String> topics;
        private List<String> projects;
        private String goal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SkillRecommendationDto {
        private String name;
        private String category;
        private String priority; // HIGH, MEDIUM, LOW
        private Integer estimatedHours;
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RoadmapProjectDto {
        private String title;
        private String description;
        private List<String> skillsDemonstrated;
        private String difficulty; // Beginner, Intermediate, Advanced
        private Integer estimatedHours;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InterviewTopicDto {
        private String topicName;
        private String category; // DSA, JAVA, SPRING_BOOT, SQL, SYSTEM_DESIGN, BEHAVIORAL
        private List<String> keyQuestions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CertificationDto {
        private String name;
        private String provider;
        private String importance; // Recommended, Optional, High-Value
        private String url;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WeeklyPlanDto {
        private Integer weekNumber;
        private String theme;
        private String goals;
        private List<WeeklyTaskDto> tasks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WeeklyTaskDto {
        private String title;
        private String description;
        private String category;
        private String priority;
        private Integer estimatedHours;
        private String resourceUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RoadmapMilestoneDto {
        private String timeframe;
        private String title;
        private String description;
        private String targetOutcome;
    }
}
