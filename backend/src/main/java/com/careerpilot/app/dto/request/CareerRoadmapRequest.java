package com.careerpilot.app.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareerRoadmapRequest {

    @Size(max = 100, message = "Target role must not exceed 100 characters")
    private String targetRole;

    @Min(value = 2, message = "Duration must be at least 2 weeks")
    @Max(value = 52, message = "Duration must not exceed 52 weeks")
    private Integer preferredDurationWeeks;

    private String currentLevel;
}
