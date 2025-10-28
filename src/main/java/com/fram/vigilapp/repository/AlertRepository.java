package com.fram.vigilapp.repository;

import com.fram.vigilapp.entity.Alert;
import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AlertRepository extends JpaRepository<Alert, UUID> {

    @Query(value = "SELECT a.*, ST_Distance(a.geometry::geography, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography) as distance " +
            "FROM alerts a " +
            "WHERE a.status = 'ACTIVE' " +
            "AND ST_DWithin(a.geometry::geography, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography, :radiusM) " +
            "ORDER BY distance",
            nativeQuery = true)
    List<Alert> findActiveAlertsWithinRadius(@Param("latitude") Double latitude,
                                             @Param("longitude") Double longitude,
                                             @Param("radiusM") Integer radiusM);

    @Query(value = "SELECT a.*, ST_Distance(a.geometry::geography, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography) as distance " +
            "FROM alerts a " +
            "WHERE ST_DWithin(a.geometry::geography, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography, :radiusM) " +
            "ORDER BY distance",
            nativeQuery = true)
    List<Alert> findAllAlertsWithinRadius(@Param("latitude") Double latitude,
                                          @Param("longitude") Double longitude,
                                          @Param("radiusM") Integer radiusM);

    @Query(value = "SELECT ST_Distance(a.geometry::geography, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography) " +
            "FROM alerts a " +
            "WHERE a.id = :alertId",
            nativeQuery = true)
    Double calculateDistanceFromPoint(@Param("alertId") UUID alertId,
                                      @Param("latitude") Double latitude,
                                      @Param("longitude") Double longitude);

    List<Alert> findByCreatedByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Alert> findByStatusOrderByCreatedAtDesc(String status);

    List<Alert> findByCategoryAndStatusOrderByCreatedAtDesc(String category, String status);
}
