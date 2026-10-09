package com.careerpilot.app.dto.response;

import com.careerpilot.app.dto.CareerRoadmapDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareerRoadmapResponse {

    private Long id;
    private String targetRole;
    private String currentLevel;
    private Integer overallReadinessScore;
    private String summary;
    private List<String> currentSkills;
    private List<String> skillGaps;
    private List<String> recommendedSkills;
    private List<CareerRoadmapDto.RoadmapPhaseDto> roadmap;
    private List<String> projectRecommendations;
    private String jobReadinessSummary;
    private List<LearningRecommendationResponse> learningRecommendations;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Optional legacy helper fields for backwards compatibility
    private Integer overallProgress;
    private String aiSummary;
    private Integer estimatedDurationWeeks;
    private List<RoadmapTaskResponse> tasks;
    private CareerRoadmapDto roadmapDetails;
}
