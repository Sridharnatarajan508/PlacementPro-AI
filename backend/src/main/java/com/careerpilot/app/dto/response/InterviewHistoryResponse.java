package com.careerpilot.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewHistoryResponse {

    private Long id;
    private String targetRole;
    private String targetJobTitle;
    private String interviewType;
    private String difficulty;
    private Integer overallScore;
    private Integer technicalScore;
    private Integer communicationScore;
    private Integer totalQuestions;
    private Integer completedQuestions;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
