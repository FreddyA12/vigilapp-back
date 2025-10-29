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
    private Integer intensity; // Number of alerts in this grid cell
    private String mostCommonCategory; // Most common category in this cell (optional)
}
