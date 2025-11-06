package com.fram.vigilapp.dto;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotificationDto {
    private UUID id;
    private UUID alertId;
    private String alertTitle;
    private String alertCategory;
    private UUID userId;
    private String channel; // PUSH | EMAIL | SMS
    private String status; // QUEUED | SENT | DELIVERED | FAILED
    private OffsetDateTime sentAt;
    private OffsetDateTime deliveredAt;
    private OffsetDateTime createdAt;
}
