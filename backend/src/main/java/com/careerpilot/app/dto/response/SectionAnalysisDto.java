package com.careerpilot.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectionAnalysisDto {
    private String summary;
    private String education;
    private String skills;
    private String projects;
    private String experience;
    private String certifications;
}
