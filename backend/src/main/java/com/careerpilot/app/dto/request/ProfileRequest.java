package com.careerpilot.app.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProfileRequest {

    private String college;
    private String degree;
    private String branch;

    @Min(value = 2000, message = "Graduation year must be after 2000")
    @Max(value = 2035, message = "Graduation year must be before 2035")
    private Integer graduationYear;

    @DecimalMin(value = "0.0", message = "CGPA cannot be negative")
    @DecimalMax(value = "10.0", message = "CGPA cannot exceed 10.0")
    private BigDecimal cgpa;

    private String targetRole;
    private String preferredJobType;
    private String experienceLevel;
    private String skillsSummary;
    private Boolean onboardingComplete;
}
