package com.fram.vigilapp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "identity_verifications")
public class IdentityVerification {
    @Id
    @GeneratedValue
    @Column(columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selfie_media_id")
    private Media selfieMedia;

    @Column(name = "provider", columnDefinition = "text")
    private String provider;

    @Column(name = "status", columnDefinition = "text")
    private String status; // PENDING | VERIFIED | REJECTED

    @Column(name = "created_at", columnDefinition = "timestamptz")
    private OffsetDateTime createdAt;

    @Column(name = "decided_at", columnDefinition = "timestamptz")
    private OffsetDateTime decidedAt;

    @Column(name = "liveness_score", precision = 5, scale = 2)
    private BigDecimal livenessScore;

    @Column(name = "match_score", precision = 5, scale = 2)
    private BigDecimal matchScore;
}
