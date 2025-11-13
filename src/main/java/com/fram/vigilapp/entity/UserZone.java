package com.fram.vigilapp.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Polygon;

import java.util.UUID;

@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "user_zones")
public class UserZone {
    @Id
    @GeneratedValue
    @Column(columnDefinition = "UUID")
    private UUID id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @JdbcTypeCode(SqlTypes.GEOMETRY)
    @Column(name = "geometry", columnDefinition = "geography(Polygon,4326)")
    private Polygon geometry;

    @Column(name = "radius_m")
    private Integer radiusM;
}
