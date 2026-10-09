package com.careerpilot.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewQuestionResponse {

    private Long id;
    private Long sessionId;
    private Integer questionNumber;
    private String questionText;
    private String category;
    private String difficulty;
    private Integer totalQuestions;
    private Boolean answered;
}
