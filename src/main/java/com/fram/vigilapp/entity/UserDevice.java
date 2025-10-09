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
@Table(name = "user_devices")
public class UserDevice {
    @Id
    @GeneratedValue
    @Column(columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "provider", columnDefinition = "text")
    private String provider; // FCM/APNS/etc

    @Column(name = "token", columnDefinition = "text")
    private String token;

    @Column(name = "created_at", columnDefinition = "timestamptz")
    private OffsetDateTime createdAt;

    @Column(name = "last_seen_at", columnDefinition = "timestamptz")
    private OffsetDateTime lastSeenAt;
}
