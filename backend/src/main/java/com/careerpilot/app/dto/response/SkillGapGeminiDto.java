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
public class SkillGapGeminiDto {
    private Integer readinessScore;
    private Integer skillCoverageScore;
    private String summary;
    private List<String> currentSkills;
    private List<String> requiredSkills;
    private List<String> missingSkills;
    private List<SkillGapDto> skillGaps;
    private List<LearningPlanStepDto> learningPlan;
}
