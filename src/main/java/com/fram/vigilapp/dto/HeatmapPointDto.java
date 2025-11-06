package com.fram.vigilapp.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HeatmapPointDto {
    private Double latitude;
    private Double longitude;
    private Integer count; // Number of alerts in this grid cell
    private Double intensity; // Normalized intensity (0.0 to 1.0)
    private String mostCommonCategory; // Most common category in this cell (optional)
}
