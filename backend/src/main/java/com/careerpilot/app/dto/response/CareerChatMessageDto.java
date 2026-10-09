package com.careerpilot.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareerChatMessageDto {

    private Long id;
    private String role; // "USER" or "ASSISTANT"
    private String message;
    private List<String> suggestedActions;
    private List<String> relevantSkills;
    private List<String> relevantJobs;
    private List<String> preparationSuggestions;
    private LocalDateTime createdAt;
}
