package com.fram.vigilapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FaceVerificationResponse {
    private Boolean match;
    private Double distance;
    private Double similarity;
    private Double threshold;
    private String model;
}
