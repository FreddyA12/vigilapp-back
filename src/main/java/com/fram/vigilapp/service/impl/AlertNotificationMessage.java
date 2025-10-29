package com.fram.vigilapp.service.impl;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AlertNotificationMessage {
    private String event; // "NEW_ALERT"
    private UUID alertId;
    private String alertTitle;
    private String alertCategory;
    private String alertDescription;
    private Double latitude;
    private Double longitude;
    private String createdByUserName;
    private Long timestamp; // milliseconds
}
