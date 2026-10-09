package com.careerpilot.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationStatsDto {

    private long totalApplications;
    private long savedCount;
    private long appliedCount;
    private long screeningCount;
    private long interviewCount;
    private long offerCount;
    private long rejectedCount;
    private long withdrawnCount;
    private long activeCount;
    
    private double responseRate;   // (Screening + Interview + Offer) / (Total - Saved) * 100
    private double interviewRate;  // (Interview + Offer) / (Total - Saved) * 100
    private double offerRate;      // Offer / (Total - Saved) * 100
}
