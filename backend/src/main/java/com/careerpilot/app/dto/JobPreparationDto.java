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
public class JobPreparationDto {

    private Integer readinessScore;
    private String preparationLevel;
    private String summary;
    private List<String> strengths;
    private List<String> skillGaps;
    private List<String> technicalTopics;
    private List<String> interviewTopics;
    private List<String> codingTopics;
    private List<String> projectTopics;
    private List<String> behavioralTopics;
    private List<PreparationDayDto> preparationPlan;
    private List<RecommendedQuestionDto> recommendedQuestions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PreparationDayDto {
        private Integer day;
        private String title;
        private String focus;
        private List<String> topics;
        private List<String> tasks;
        private List<String> practiceQuestions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RecommendedQuestionDto {
        private String question;
        private String type; // TECHNICAL, CODING, BEHAVIORAL, PROJECT
        private String suggestedAnswer;
        private String keyConcept;
    }
}
