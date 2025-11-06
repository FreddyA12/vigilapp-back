package com.fram.vigilapp.service;

import com.fram.vigilapp.dto.AlertDto;
import com.fram.vigilapp.dto.SaveAlertDto;
import com.fram.vigilapp.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface AlertService {
    AlertDto createAlert(User user, SaveAlertDto saveAlertDto);
    AlertDto getAlertById(UUID alertId);
    List<AlertDto> getAlertsNearLocation(Double latitude, Double longitude, Integer radiusM, boolean activeOnly);
    List<AlertDto> getAlertsInUserZone(UUID userId);
    List<AlertDto> getUserAlerts(UUID userId);
    List<AlertDto> getAlertsByStatus(String status);
    List<AlertDto> getAlertsByCategoryAndStatus(String category, String status);
    AlertDto updateAlertStatus(UUID alertId, String newStatus);
    void deleteAlert(UUID alertId);

    /**
     * Get recent alerts with pagination
     */
    Page<AlertDto> getRecentAlerts(Pageable pageable);

    /**
     * Advanced search with multiple filters
     */
    List<AlertDto> searchAlerts(
            String query,
            String category,
            String status,
            String verificationStatus,
            UUID cityId,
            Integer minRadiusM,
            Integer maxRadiusM,
            OffsetDateTime dateFrom,
            OffsetDateTime dateTo,
            int skip,
            int limit
    );

    /**
     * Get heatmap data (alert density by grid)
     */
    List<com.fram.vigilapp.dto.HeatmapPointDto> getHeatmapData(
            Double swLat,
            Double swLon,
            Double neLat,
            Double neLon,
            Double gridSizeM
    );

    /**
     * Get alert statistics
     */
    com.fram.vigilapp.dto.AlertStatsDto getAlertStats(String timeRange, UUID cityId);
}
