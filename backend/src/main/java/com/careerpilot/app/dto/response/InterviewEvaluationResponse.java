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
public class InterviewEvaluationResponse {

    private Long questionId;
    private Integer questionNumber;
    private Integer score;
    private Integer technicalScore;
    private Integer relevanceScore;
    private Integer communicationScore;
    private Integer confidenceScore;
    private String feedback;
    private List<String> strengths;
    private List<String> improvements;
    private String idealAnswer;
    private InterviewQuestionResponse nextQuestion;
    private Boolean isComplete;
    private InterviewSessionResponse finalSessionResult;
}
