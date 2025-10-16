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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_document_media_id")
    private Media idDocumentMedia;

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

    // ID Document Validation
    @Column(name = "is_valid_id_document")
    private Boolean isValidIdDocument;

    @Column(name = "id_validation_confidence", precision = 5, scale = 2)
    private BigDecimal idValidationConfidence;

    // Face Comparison
    @Column(name = "face_match_similarity", precision = 5, scale = 4)
    private BigDecimal faceMatchSimilarity;

    @Column(name = "face_match_confidence", precision = 5, scale = 2)
    private BigDecimal faceMatchConfidence;

    @Column(name = "faces_match")
    private Boolean facesMatch;

    // Additional Metadata
    @Column(name = "verification_method", columnDefinition = "text")
    private String verificationMethod; // e.g., "FACE_RECOGNITION_PYTHON"

    @Column(name = "failure_reason", columnDefinition = "text")
    private String failureReason;
}
