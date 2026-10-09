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
public class LearningRecommendationDto {

    private String skill;
    private String priority; // HIGH, MEDIUM, LOW
    private String reason;
    private String learningObjective;
    private List<String> recommendedTopics;
    private String projectIdea;
    private String estimatedEffort;
}
