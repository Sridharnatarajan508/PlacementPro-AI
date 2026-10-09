package com.careerpilot.app.dto.response;

import com.careerpilot.app.dto.CareerAnalyticsDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CareerAnalyticsResponse {

    private Long id;

    @Builder.Default
    private Integer overallReadinessScore = 0;

    private Integer previousReadinessScore;

    @Builder.Default
    private String readinessLevel = "NEEDS PREPARATION";

    private String careerSummary;

    @Builder.Default
    private List<String> strengths = new ArrayList<>();

    @Builder.Default
    private List<CareerAnalyticsDto.WeaknessItem> weaknesses = new ArrayList<>();

    @Builder.Default
    private List<CareerAnalyticsDto.SkillProgressItem> skillProgress = new ArrayList<>();

    @Builder.Default
    private CareerAnalyticsDto.InterviewProgressSummary interviewProgress = new CareerAnalyticsDto.InterviewProgressSummary();

    @Builder.Default
    private CareerAnalyticsDto.JobSearchProgressSummary jobSearchProgress = new CareerAnalyticsDto.JobSearchProgressSummary();

    @Builder.Default
    private CareerAnalyticsDto.RoadmapProgressSummary roadmapProgress = new CareerAnalyticsDto.RoadmapProgressSummary();

    @Builder.Default
    private List<CareerAnalyticsDto.RecommendationItem> recommendations = new ArrayList<>();

    @Builder.Default
    private List<CareerAnalyticsDto.WeeklyGoalItem> weeklyGoals = new ArrayList<>();

    private LocalDateTime generatedAt;

    private LocalDateTime updatedAt;
}
