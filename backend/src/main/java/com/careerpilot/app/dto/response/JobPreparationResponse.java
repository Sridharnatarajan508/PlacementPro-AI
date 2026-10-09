package com.careerpilot.app.dto.response;

import com.careerpilot.app.dto.JobPreparationDto;
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
public class JobPreparationResponse {

    private Long id;
    private Long jobId;
    private String jobTitle;
    private String company;
    private String location;
    private String experienceLevel;
    private Integer jobMatchScore;
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
    private List<JobPreparationDto.PreparationDayDto> preparationPlan;
    private List<JobPreparationDto.RecommendedQuestionDto> recommendedQuestions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
