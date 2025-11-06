package com.fram.vigilapp.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserZoneDto {
    private UUID id;
    private UUID userId;
    private Double centerLatitude;
    private Double centerLongitude;
    private Integer radiusM;
}
