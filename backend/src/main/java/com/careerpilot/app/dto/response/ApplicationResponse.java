package com.careerpilot.app.dto.response;

import com.careerpilot.app.dto.ApplicationStatsDto;
import com.careerpilot.app.dto.JobApplicationDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationResponse {

    @Builder.Default
    private boolean success = true;

    private String message;
    private JobApplicationDto application;
    private ApplicationStatsDto statistics;
}
