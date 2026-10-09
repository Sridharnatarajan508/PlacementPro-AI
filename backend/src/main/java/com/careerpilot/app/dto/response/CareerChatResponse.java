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
public class CareerChatResponse {

    private Long id;
    private String title;
    private List<CareerChatMessageDto> messages;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
