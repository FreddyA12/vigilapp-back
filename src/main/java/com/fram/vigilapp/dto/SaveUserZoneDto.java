package com.fram.vigilapp.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SaveUserZoneDto {
    @NotNull(message = "La latitud central es requerida")
    @Min(value = -90, message = "La latitud debe estar entre -90 y 90")
    @Max(value = 90, message = "La latitud debe estar entre -90 y 90")
    private Double centerLatitude;

    @NotNull(message = "La longitud central es requerida")
    @Min(value = -180, message = "La longitud debe estar entre -180 y 180")
    @Max(value = 180, message = "La longitud debe estar entre -180 y 180")
    private Double centerLongitude;

    @NotNull(message = "El radio es requerido")
    @Min(value = 100, message = "El radio mínimo es 100 metros")
    @Max(value = 50000, message = "El radio máximo es 50,000 metros (50 km)")
    private Integer radiusM;
}
