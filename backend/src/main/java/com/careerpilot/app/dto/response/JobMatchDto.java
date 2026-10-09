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
public class JobMatchDto {
    private Integer overallScore;
    private Integer skillScore;
    private Integer experienceScore;
    private Integer educationScore;
    private List<String> matchingSkills;
    private List<String> missingSkills;
    private String recommendation;
    private String explanation;
}
