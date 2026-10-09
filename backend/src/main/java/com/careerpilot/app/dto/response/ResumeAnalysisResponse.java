package com.careerpilot.app.dto.response;

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
public class ResumeAnalysisResponse {
    private Long id;
    private Long resumeId;
    private Integer overallScore;
    private Integer atsScore;
    private String targetRole;
    private String summary;
    private List<String> strengths;
    private List<String> weaknesses;
    private List<DetectedSkillDto> detectedSkills;
    private List<MissingSkillDto> missingSkills;
    private List<String> suggestions;
    private SectionAnalysisDto sectionAnalysis;
    private LocalDateTime analyzedAt;
}
