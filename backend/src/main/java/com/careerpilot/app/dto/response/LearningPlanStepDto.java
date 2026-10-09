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
public class LearningPlanStepDto {
    private Integer step;
    private String phaseTitle;
    private String skillName;
    private String description;
    private Integer estimatedHours;
    private List<String> keyTopics;
    private String milestoneProject;
}
