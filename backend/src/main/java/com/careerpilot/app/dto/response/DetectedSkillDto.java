package com.careerpilot.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetectedSkillDto {
    private String name;
    private String category; // e.g. "Programming Languages", "Frameworks", "Databases", "Tools"
    private Double confidence; // e.g. 0.95
}
