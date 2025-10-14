package com.fram.vigilapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IdValidationResponse {
    @JsonProperty("is_id_document")
    private Boolean isIdDocument;

    @JsonProperty("confidence")
    private Double confidence;

    @JsonProperty("aspect_ratio")
    private Double aspectRatio;

    @JsonProperty("reasons")
    private List<String> reasons;
}
