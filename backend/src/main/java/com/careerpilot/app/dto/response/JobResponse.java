package com.careerpilot.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobResponse {
    private Long id;
    private String title;
    private String company;
    private String location;
    private String jobType;
    private String experienceLevel;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private String description;
    private String requiredSkills;
    private String preferredSkills;
    private String applicationUrl;
    private LocalDate postedDate;
    private LocalDateTime createdAt;
    private Boolean isSaved;
    private Integer matchScore;
    private String matchRecommendation;
}
