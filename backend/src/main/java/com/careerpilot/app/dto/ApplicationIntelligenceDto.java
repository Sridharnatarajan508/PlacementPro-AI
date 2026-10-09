package com.careerpilot.app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApplicationIntelligenceDto {

    private Integer readinessScore;
    private String summary;
    
    @Builder.Default
    private List<String> strengths = new ArrayList<>();
    
    @Builder.Default
    private List<String> risks = new ArrayList<>();
    
    @Builder.Default
    private List<String> recommendedActions = new ArrayList<>();
    
    @Builder.Default
    private List<String> followUpAdvice = new ArrayList<>();
    
    @Builder.Default
    private List<String> interviewAdvice = new ArrayList<>();

    @Builder.Default
    private List<String> resumeAdvice = new ArrayList<>();

    private String priority; // HIGH, MEDIUM, LOW
    private String applicationStatus;
    private Boolean isCached;
}
