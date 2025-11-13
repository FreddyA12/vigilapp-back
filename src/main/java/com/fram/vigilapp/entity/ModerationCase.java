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
@Table(name = "moderation_cases")
public class ModerationCase {
    @Id
    @GeneratedValue
    @Column(columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_id")
    private Alert alert;

    @Column(name = "status", columnDefinition = "text")
    private String status; // OPEN | IN_REVIEW | VERIFIED_TRUE | VERIFIED_FALSE | CLOSED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opened_by_user_id")
    private User openedByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_user_id")
    private User assignedToUser;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "opened_at", columnDefinition = "timestamptz")
    private OffsetDateTime openedAt;

    @Column(name = "closed_at", columnDefinition = "timestamptz")
    private OffsetDateTime closedAt;
}
