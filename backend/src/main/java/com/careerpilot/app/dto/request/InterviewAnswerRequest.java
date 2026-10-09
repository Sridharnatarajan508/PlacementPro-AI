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
public class InterviewAnswerRequest {

    @NotBlank(message = "Answer cannot be blank")
    @Size(max = 10000, message = "Answer must not exceed 10000 characters")
    private String answer;
}
