package com.careerpilot.app.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobSearchRequest {
    private String keyword;
    private String location;
    private String jobType;
    private String experienceLevel;
    private Integer minimumMatchScore;
}
