package com.careerpilot.app.dto;

import com.careerpilot.app.entity.ApplicationActivityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationActivityDto {

    private Long id;
    private ApplicationActivityType activityType;
    private String description;
    private LocalDateTime activityDate;
    private LocalDateTime createdAt;
}
