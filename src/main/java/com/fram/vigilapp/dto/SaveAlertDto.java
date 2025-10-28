package com.fram.vigilapp.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SaveAlertDto {
    @NotNull(message = "La categoría es requerida")
    @Pattern(regexp = "EMERGENCY|PRECAUTION|INFO|COMMUNITY", message = "Categoría inválida")
    private String category;

    @NotBlank(message = "El título es requerido")
    @Size(min = 5, max = 200, message = "El título debe tener entre 5 y 200 caracteres")
    private String title;

    @NotBlank(message = "La descripción es requerida")
    @Size(min = 10, max = 2000, message = "La descripción debe tener entre 10 y 2000 caracteres")
    private String description;

    private Boolean isAnonymous;

    private String address;

    private UUID cityId;

    @NotNull(message = "La latitud es requerida")
    @Min(value = -90, message = "La latitud debe estar entre -90 y 90")
    @Max(value = 90, message = "La latitud debe estar entre -90 y 90")
    private Double latitude;

    @NotNull(message = "La longitud es requerida")
    @Min(value = -180, message = "La longitud debe estar entre -180 y 180")
    @Max(value = 180, message = "La longitud debe estar entre -180 y 180")
    private Double longitude;

    @Min(value = 50, message = "El radio mínimo es 50 metros")
    @Max(value = 10000, message = "El radio máximo es 10,000 metros (10 km)")
    private Integer radiusM;
}
