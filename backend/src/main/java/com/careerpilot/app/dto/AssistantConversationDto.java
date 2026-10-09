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
public class AssistantConversationDto {

    private Long id;
    private String title;
    
    @Builder.Default
    private List<AssistantMessageDto> messages = Collections.emptyList();
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
