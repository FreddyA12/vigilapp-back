package com.fram.vigilapp.dto;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AlertStatsDto {
    private Long totalAlerts;
    private Long activeAlerts;
    private Long resolvedAlerts;
    private Long cancelledAlerts;
    private Map<String, Long> alertsByCategory; // Category -> Count
    private Map<String, Long> alertsByVerificationStatus; // Status -> Count
    private Double falseReportsPercentage;
    private Long totalUsers;
    private Long activeUsers;
    private String timeRange; // e.g., "24h", "7d", "30d"
}
