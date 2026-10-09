package com.careerpilot.app.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewStartRequest {

    private String targetRole;

    private Long targetJobId;

    @Builder.Default
    private String interviewType = "MIXED"; // TECHNICAL, BEHAVIORAL, HR, MIXED

    @Builder.Default
    private String difficulty = "MEDIUM"; // EASY, MEDIUM, HARD

    @Min(value = 5, message = "Minimum 5 questions")
    @Max(value = 15, message = "Maximum 15 questions")
    @Builder.Default
    private Integer totalQuestions = 10; // 5, 10, 15
}
