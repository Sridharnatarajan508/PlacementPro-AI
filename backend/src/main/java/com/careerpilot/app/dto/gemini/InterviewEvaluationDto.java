package com.careerpilot.app.dto.gemini;

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
public class InterviewEvaluationDto {

    private Integer score; // 0-100
    private Integer technicalScore; // 0-100
    private Integer relevanceScore; // 0-100
    private Integer communicationScore; // 0-100
    private Integer confidenceScore; // 0-100
    private String feedback;
    private List<String> strengths;
    private List<String> improvements;
    private String idealAnswer;
}
