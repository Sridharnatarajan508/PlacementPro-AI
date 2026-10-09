package com.careerpilot.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoadmapTaskResponse {

    private Long id;
    private String title;
    private String description;
    private String category;
    private String priority;
    private Integer estimatedHours;
    private Integer weekNumber;
    private Boolean completed;
    private LocalDateTime completedAt;
    private String resourceUrl;
}
