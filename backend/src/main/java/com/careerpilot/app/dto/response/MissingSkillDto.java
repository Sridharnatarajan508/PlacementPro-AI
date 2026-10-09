package com.careerpilot.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MissingSkillDto {
    private String name;
    private String reason;
    private String priority; // HIGH, MEDIUM, LOW
}
