package com.careerpilot.app.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillGapAnalyzeRequest {
    private Long jobId;
    private String targetRole;
    @Builder.Default
    private Boolean forceRefresh = false;
}
