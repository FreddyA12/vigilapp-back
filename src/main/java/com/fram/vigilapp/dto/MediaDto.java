package com.fram.vigilapp.dto;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MediaDto {
    private UUID id;
    private String url;
    private String mimeType;
    private Boolean wasBlurred;
    private OffsetDateTime createdAt;
}
