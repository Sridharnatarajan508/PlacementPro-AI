package com.careerpilot.app.dto.response;

import com.careerpilot.app.dto.CareerAssistantDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareerAssistantResponse {

    private Long conversationId;
    private String message;
    
    @Builder.Default
    private List<CareerAssistantDto.ActionItem> suggestedActions = Collections.emptyList();
    
    @Builder.Default
    private List<String> contextUsed = Collections.emptyList();
    
    @Builder.Default
    private Boolean rateLimited = false;

    // Backwards compatibility fields
    private Long chatId;
    private String chatTitle;
    private String answer;
    
    @Builder.Default
    private List<String> relevantSkills = Collections.emptyList();
    
    @Builder.Default
    private List<String> relevantJobs = Collections.emptyList();
    
    @Builder.Default
    private List<String> preparationSuggestions = Collections.emptyList();
    
    private LocalDateTime createdAt;
}
