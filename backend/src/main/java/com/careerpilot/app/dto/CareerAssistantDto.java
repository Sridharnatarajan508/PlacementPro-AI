package com.careerpilot.app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CareerAssistantDto {

    private String message;
    private String answer; // alias for backwards compatibility
    
    @Builder.Default
    private List<ActionItem> suggestedActions = Collections.emptyList();
    
    @Builder.Default
    private List<String> contextUsed = Collections.emptyList();

    // Legacy fields for backward compatibility
    @Builder.Default
    private List<String> relevantSkills = Collections.emptyList();
    
    @Builder.Default
    private List<String> relevantJobs = Collections.emptyList();
    
    @Builder.Default
    private List<String> preparationSuggestions = Collections.emptyList();

    public String getEffectiveMessage() {
        if (message != null && !message.isBlank()) {
            return message;
        }
        return answer != null ? answer : "";
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ActionItem {
        private String label;
        private String route;
    }
}
