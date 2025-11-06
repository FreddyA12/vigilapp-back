package com.fram.vigilapp.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SaveNotificationDto {
    @NotNull(message = "El ID de alerta es requerido")
    private String alertId;

    @NotNull(message = "El ID de usuario es requerido")
    private String userId;

    @NotNull(message = "El canal es requerido")
    private String channel; // PUSH | EMAIL | SMS

    // Optional fields
    private String status; // Default: QUEUED
}
