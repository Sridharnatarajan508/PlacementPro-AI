package com.careerpilot.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponse {

    private Long id;
    private Long userId;
    private String name;
    private String email;
    private String college;
    private String degree;
    private String branch;
    private Integer graduationYear;
    private BigDecimal cgpa;
    private String targetRole;
    private String preferredJobType;
    private String experienceLevel;
    private String skillsSummary;
    private Boolean onboardingComplete;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
