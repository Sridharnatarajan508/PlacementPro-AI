package com.careerpilot.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobSkillGapDto {

    private String jobTitle;
    private String company;
    private List<String> missingSkills;
}
