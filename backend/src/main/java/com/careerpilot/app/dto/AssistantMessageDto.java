package com.careerpilot.app.dto;

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
public class AssistantMessageDto {

    private Long id;
    private String role;
    private String content;
    private String message; // alias for backwards compatibility
    
    @Builder.Default
    private List<CareerAssistantDto.ActionItem> suggestedActions = Collections.emptyList();
    
    @Builder.Default
    private List<String> contextUsed = Collections.emptyList();
    
    private LocalDateTime createdAt;
}
