package com.careerpilot.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpcomingTasksDto {

    @Builder.Default
    private List<JobApplicationDto> upcomingFollowUps = new ArrayList<>();

    @Builder.Default
    private List<JobApplicationDto> upcomingInterviews = new ArrayList<>();

    @Builder.Default
    private List<JobApplicationDto> approachingDeadlines = new ArrayList<>();
}
