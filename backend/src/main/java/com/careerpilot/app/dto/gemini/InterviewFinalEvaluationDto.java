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
public class InterviewFinalEvaluationDto {

    private Integer overallScore;
    private Integer technicalScore;
    private Integer communicationScore;
    private Integer confidenceScore;
    private Integer problemSolvingScore;
    private String summary;
    private List<String> strengths;
    private List<String> weaknesses;
    private List<String> recommendations;
}
