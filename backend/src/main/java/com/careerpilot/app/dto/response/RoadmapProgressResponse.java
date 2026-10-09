package com.careerpilot.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoadmapProgressResponse {

    private Integer totalTasks;
    private Integer completedTasks;
    private Integer progressPercentage;
    private Integer completedHours;
    private Integer remainingHours;
}
