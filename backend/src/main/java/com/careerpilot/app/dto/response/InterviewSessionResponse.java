package com.careerpilot.app.dto.response;

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
public class InterviewSessionResponse {

    private Long id;
    private String targetRole;
    private Long targetJobId;
    private String targetJobTitle;
    private String targetJobCompany;
    private String interviewType;
    private String difficulty;
    private Integer totalQuestions;
    private Integer completedQuestions;
    private Integer overallScore;
    private Integer technicalScore;
    private Integer communicationScore;
    private Integer confidenceScore;
    private Integer problemSolvingScore;
    private String status;
    private String aiSummary;
    private List<String> strengths;
    private List<String> weaknesses;
    private List<String> recommendations;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private List<QuestionDetailDto> questions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionDetailDto {
        private Long id;
        private Integer questionNumber;
        private String questionText;
        private String category;
        private String difficulty;
        private String userAnswer;
        private Integer score;
        private Integer technicalScore;
        private Integer relevanceScore;
        private Integer communicationScore;
        private Integer confidenceScore;
        private String aiFeedback;
        private List<String> strengths;
        private List<String> improvements;
        private String idealAnswer;
        private LocalDateTime answeredAt;
    }
}
