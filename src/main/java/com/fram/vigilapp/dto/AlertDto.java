package com.fram.vigilapp.dto;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AlertDto {
    private UUID id;
    private UUID createdByUserId;
    private String createdByUserName;
    private String category;
    private String status;
    private String verificationStatus;
    private String title;
    private String description;
    private Boolean isAnonymous;
    private String address;
    private UUID cityId;
    private String cityName;
    private Double latitude;
    private Double longitude;
    private Integer radiusM;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime resolvedAt;
    private Double distanceFromUserM;
}
