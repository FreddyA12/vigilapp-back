package com.fram.vigilapp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "alert_validations")
public class AlertValidation {
    @Id
    @GeneratedValue
    @Column(columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_id", nullable = false)
    private Alert alert;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "vote_type", columnDefinition = "text")
    private String voteType; // CONFIRM | FALSE

    @Column(name = "created_at", columnDefinition = "timestamptz")
    private OffsetDateTime createdAt;
}
