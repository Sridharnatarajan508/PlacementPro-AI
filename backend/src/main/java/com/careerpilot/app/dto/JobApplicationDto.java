package com.careerpilot.app.dto;

import com.careerpilot.app.entity.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobApplicationDto {

    private Long id;
    private Long jobId;
    private String company;
    private String jobTitle;
    private ApplicationStatus status;
    private LocalDate applicationDate;
    private LocalDate deadline;
    private LocalDate nextFollowUpDate;
    private LocalDateTime interviewDate;
    private String jobUrl;
    private String location;
    private String recruiterName;
    private String recruiterEmail;
    private String source;
    private String notes;
    private Integer matchScore;
    private String matchRecommendation;
    
    @Builder.Default
    private List<ApplicationActivityDto> activityTimeline = new ArrayList<>();
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
