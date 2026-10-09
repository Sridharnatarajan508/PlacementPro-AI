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
public class SkillGapDto {
    private String skillName;
    private String priority; // HIGH, MEDIUM, LOW
    private String currentLevel; // None, Beginner, Intermediate, Advanced
    private String requiredLevel; // Beginner, Intermediate, Advanced
    private String importance; // Critical, High, Medium, Low
    private String reason;
    private List<String> recommendedTechnologies;
    private List<String> practiceProjects;
    private Integer estimatedLearningHours;
    private String difficulty; // Beginner, Intermediate, Advanced
    private Integer learningOrder;
}
