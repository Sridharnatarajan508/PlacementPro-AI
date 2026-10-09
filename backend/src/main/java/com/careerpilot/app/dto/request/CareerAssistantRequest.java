package com.careerpilot.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareerAssistantRequest {

    @NotBlank(message = "Message must not be empty")
    @Size(max = 2000, message = "Message must not exceed 2000 characters")
    private String message;

    private Long chatId;
}
