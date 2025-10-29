package com.fram.vigilapp.repository;

import com.fram.vigilapp.entity.Alert;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
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

    /**
     * Find recent alerts with pagination
     */
    Page<Alert> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    /**
     * Find all active alerts ordered by creation date (for recent alerts)
     */
    Page<Alert> findByStatusIn(List<String> statuses, Pageable pageable);

    /**
     * Get alerts within a geographic bounding box for heatmap
     */
    @Query(value = "SELECT a.* FROM alerts a " +
            "WHERE a.status = 'ACTIVE' " +
            "AND ST_Within(a.geometry::geography, ST_MakeEnvelope(:minLon, :minLat, :maxLon, :maxLat, 4326)::geography)",
            nativeQuery = true)
    List<Alert> findAlertsInBounds(
            @Param("minLat") Double minLat,
            @Param("minLon") Double minLon,
            @Param("maxLat") Double maxLat,
            @Param("maxLon") Double maxLon
    );

    /**
     * Count alerts by category within time range
     */
    @Query("SELECT COUNT(a) FROM Alert a WHERE a.category = :category AND a.createdAt >= :dateFrom AND a.createdAt <= :dateTo")
    long countByCategory(@Param("category") String category,
                         @Param("dateFrom") OffsetDateTime dateFrom,
                         @Param("dateTo") OffsetDateTime dateTo);

    /**
     * Find alerts within time range
     */
    List<Alert> findByCreatedAtBetweenOrderByCreatedAtDesc(OffsetDateTime dateFrom, OffsetDateTime dateTo);

    /**
     * Count alerts by verification status
     */
    long countByVerificationStatus(String verificationStatus);

    /**
     * Count alerts by city
     */
    @Query("SELECT COUNT(a) FROM Alert a WHERE a.city.id = :cityId")
    long countByCity(@Param("cityId") UUID cityId);
}
