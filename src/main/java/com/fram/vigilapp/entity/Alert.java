package com.fram.vigilapp.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "alerts")
public class Alert {
    @Id
    @GeneratedValue
    @Column(columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdByUser;

    @Column(name = "category", columnDefinition = "text")
    private String category; // EMERGENCY | PRECAUTION | INFO | COMMUNITY

    @Column(name = "status", columnDefinition = "text")
    private String status; // ACTIVE | RESOLVED | CANCELLED | EXPIRED

    @Column(name = "verification_status", columnDefinition = "text")
    private String verificationStatus; // PENDING | VERIFIED | REJECTED

    @Column(name = "title", columnDefinition = "text")
    private String title;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "is_anonymous")
    private Boolean isAnonymous;

    @Column(name = "address", columnDefinition = "text")
    private String address;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id")
    private City city;

    @JdbcTypeCode(SqlTypes.GEOMETRY)
    @Column(name = "geometry", columnDefinition = "geography(Point,4326)")
    private Point geometry;

    @Column(name = "radius_m")
    private Integer radiusM;

    @CreationTimestamp
    @Column(name = "created_at", columnDefinition = "timestamptz")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", columnDefinition = "timestamptz")
    private OffsetDateTime updatedAt;

    @Column(name = "resolved_at", columnDefinition = "timestamptz")
    private OffsetDateTime resolvedAt;
}
