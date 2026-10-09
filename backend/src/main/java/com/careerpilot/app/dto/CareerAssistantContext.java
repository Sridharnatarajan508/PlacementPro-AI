package com.careerpilot.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareerAssistantContext {

    private Map<String, Object> profile;
    private Map<String, Object> resumeAnalysis;
    private Map<String, Object> skillGapAnalysis;
    private List<Map<String, Object>> topJobMatches;
    private List<Map<String, Object>> savedJobs;
    private Map<String, Object> careerRoadmap;
    private Map<String, Object> mockInterviewSummary;
    private List<Map<String, Object>> jobPreparations;
    private Map<String, Object> careerAnalytics;
    private Map<String, Object> jobApplications;
    
    @Builder.Default
    private List<String> activeContextSources = Collections.emptyList();
}
