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
@Table(name = "notifications")
public class Notification {
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

    @Column(name = "channel", columnDefinition = "text")
    private String channel; // PUSH | EMAIL | SMS

    @Column(name = "status", columnDefinition = "text")
    private String status; // QUEUED | SENT | DELIVERED | FAILED

    @Column(name = "sent_at", columnDefinition = "timestamptz")
    private OffsetDateTime sentAt;

    @Column(name = "delivered_at", columnDefinition = "timestamptz")
    private OffsetDateTime deliveredAt;

    @Column(name = "read_at", columnDefinition = "timestamptz")
    private OffsetDateTime readAt;

    @Column(name = "deleted_at", columnDefinition = "timestamptz")
    private OffsetDateTime deletedAt;

    @Column(name = "created_at", columnDefinition = "timestamptz")
    private OffsetDateTime createdAt;
}
