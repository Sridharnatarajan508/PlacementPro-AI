package com.careerpilot.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningRecommendationResponse {

    private String skill;
    private String priority;
    private String reason;
    private String learningObjective;
    private List<String> recommendedTopics;
    private String projectIdea;
    private String estimatedEffort;
}
