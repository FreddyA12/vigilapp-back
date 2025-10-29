package com.fram.vigilapp.service.impl;

import com.fram.vigilapp.dto.AlertDto;
import com.fram.vigilapp.dto.AlertStatsDto;
import com.fram.vigilapp.dto.HeatmapPointDto;
import com.fram.vigilapp.dto.SaveAlertDto;
import com.fram.vigilapp.entity.Alert;
import com.fram.vigilapp.entity.City;
import com.fram.vigilapp.entity.User;
import com.fram.vigilapp.entity.UserZone;
import com.fram.vigilapp.repository.AlertRepository;
import com.fram.vigilapp.repository.CityRepository;
import com.fram.vigilapp.repository.UserRepository;
import com.fram.vigilapp.repository.UserZoneRepository;
import com.fram.vigilapp.service.AlertService;
import com.fram.vigilapp.service.AlertNotificationService;
import com.fram.vigilapp.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AlertServiceImpl implements AlertService {

    private final AlertRepository alertRepository;
    private final UserZoneRepository userZoneRepository;
    private final CityRepository cityRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AlertNotificationService alertNotificationService;
    private final GeometryFactory geometryFactory = new GeometryFactory();

    @Override
    @Transactional
    public AlertDto createAlert(User user, SaveAlertDto saveAlertDto) {
        Point alertPoint = geometryFactory.createPoint(
                new Coordinate(saveAlertDto.getLongitude(), saveAlertDto.getLatitude())
        );
        alertPoint.setSRID(4326);

        City city = null;
        if (saveAlertDto.getCityId() != null) {
            city = cityRepository.findById(saveAlertDto.getCityId()).orElse(null);
        }

        Integer radiusM = saveAlertDto.getRadiusM() != null ? saveAlertDto.getRadiusM() : 1000;

        Alert alert = Alert.builder()
                .createdByUser(user)
                .category(saveAlertDto.getCategory())
                .status("ACTIVE")
                .verificationStatus("PENDING")
                .title(saveAlertDto.getTitle())
                .description(saveAlertDto.getDescription())
                .isAnonymous(saveAlertDto.getIsAnonymous() != null ? saveAlertDto.getIsAnonymous() : false)
                .address(saveAlertDto.getAddress())
                .city(city)
                .geometry(alertPoint)
                .radiusM(radiusM)
                .build();

        alert = alertRepository.save(alert);
        AlertDto alertDto = mapToDto(alert, null);

        // Notify users in zone (asynchronously in a real implementation)
        try {
            notificationService.notifyUsersInZone(alert, "PUSH");
        } catch (Exception e) {
            // Log error but don't fail alert creation if notifications fail
            System.err.println("Error saving notifications for alert: " + e.getMessage());
        }

        // Enviar notificación por WebSocket en tiempo real a usuarios conectados
        try {
            alertNotificationService.notifyNewAlert(alert, alertDto);
        } catch (Exception e) {
            // Log error but don't fail alert creation
            System.err.println("Error sending WebSocket notification for alert: " + e.getMessage());
        }

        return alertDto;
    }

    @Override
    @Transactional(readOnly = true)
    public AlertDto getAlertById(UUID alertId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alerta no encontrada"));

        return mapToDto(alert, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertDto> getAlertsNearLocation(Double latitude, Double longitude, Integer radiusM, boolean activeOnly) {
        List<Alert> alerts;

        if (activeOnly) {
            alerts = alertRepository.findActiveAlertsWithinRadius(latitude, longitude, radiusM);
        } else {
            alerts = alertRepository.findAllAlertsWithinRadius(latitude, longitude, radiusM);
        }

        return alerts.stream()
                .map(alert -> {
                    Double distance = alertRepository.calculateDistanceFromPoint(
                            alert.getId(), latitude, longitude
                    );
                    return mapToDto(alert, distance);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertDto> getAlertsInUserZone(UUID userId) {
        UserZone userZone = userZoneRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("El usuario no tiene una zona configurada"));

        Coordinate centroid = userZone.getGeometry().getCentroid().getCoordinate();
        Double centerLat = centroid.y;
        Double centerLon = centroid.x;
        Integer radiusM = userZone.getRadiusM();

        return getAlertsNearLocation(centerLat, centerLon, radiusM, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertDto> getUserAlerts(UUID userId) {
        List<Alert> alerts = alertRepository.findByCreatedByUserIdOrderByCreatedAtDesc(userId);

        return alerts.stream()
                .map(alert -> mapToDto(alert, null))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertDto> getAlertsByStatus(String status) {
        List<Alert> alerts = alertRepository.findByStatusOrderByCreatedAtDesc(status);

        return alerts.stream()
                .map(alert -> mapToDto(alert, null))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertDto> getAlertsByCategoryAndStatus(String category, String status) {
        List<Alert> alerts = alertRepository.findByCategoryAndStatusOrderByCreatedAtDesc(category, status);

        return alerts.stream()
                .map(alert -> mapToDto(alert, null))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AlertDto updateAlertStatus(UUID alertId, String newStatus) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alerta no encontrada"));

        alert.setStatus(newStatus);

        if ("RESOLVED".equals(newStatus) || "CANCELLED".equals(newStatus)) {
            alert.setResolvedAt(OffsetDateTime.now());
        }

        alert = alertRepository.save(alert);

        return mapToDto(alert, null);
    }

    @Override
    @Transactional
    public void deleteAlert(UUID alertId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alerta no encontrada"));

        alertRepository.delete(alert);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AlertDto> getRecentAlerts(Pageable pageable) {
        Page<Alert> alertPage = alertRepository.findByStatusIn(
                List.of("ACTIVE", "RESOLVED"),
                pageable
        );

        List<AlertDto> dtos = alertPage.getContent().stream()
                .map(alert -> mapToDto(alert, null))
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, alertPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertDto> searchAlerts(
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
            int limit) {

        List<Alert> allAlerts = alertRepository.findAll();

        // Apply filters
        List<Alert> filtered = allAlerts.stream()
                .filter(alert -> query == null || query.isEmpty() ||
                        alert.getTitle().toLowerCase().contains(query.toLowerCase()) ||
                        alert.getDescription().toLowerCase().contains(query.toLowerCase()))
                .filter(alert -> category == null || alert.getCategory().equals(category))
                .filter(alert -> status == null || alert.getStatus().equals(status))
                .filter(alert -> verificationStatus == null || alert.getVerificationStatus().equals(verificationStatus))
                .filter(alert -> cityId == null || (alert.getCity() != null && alert.getCity().getId().equals(cityId)))
                .filter(alert -> minRadiusM == null || alert.getRadiusM() >= minRadiusM)
                .filter(alert -> maxRadiusM == null || alert.getRadiusM() <= maxRadiusM)
                .filter(alert -> dateFrom == null || alert.getCreatedAt().isAfter(dateFrom))
                .filter(alert -> dateTo == null || alert.getCreatedAt().isBefore(dateTo))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .skip(skip)
                .limit(limit)
                .collect(Collectors.toList());

        return filtered.stream()
                .map(alert -> mapToDto(alert, null))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<HeatmapPointDto> getHeatmapData(
            Double swLat,
            Double swLon,
            Double neLat,
            Double neLon,
            Double gridSizeM) {

        // Get all active alerts within bounds
        List<Alert> alerts = alertRepository.findAlertsInBounds(swLat, swLon, neLat, neLon);

        // Convert grid size from meters to degrees (approximate: 1 degree ~ 111 km)
        Double gridSizeDegrees = gridSizeM / 111320.0;

        Map<String, HeatmapPointDto> gridMap = new HashMap<>();

        // Populate grid cells with alert counts
        for (Alert alert : alerts) {
            Double lat = alert.getGeometry().getY();
            Double lon = alert.getGeometry().getX();

            // Calculate grid cell coordinates
            Double gridLat = Math.floor(lat / gridSizeDegrees) * gridSizeDegrees;
            Double gridLon = Math.floor(lon / gridSizeDegrees) * gridSizeDegrees;

            String gridKey = gridLat + "," + gridLon;

            HeatmapPointDto point = gridMap.getOrDefault(gridKey, HeatmapPointDto.builder()
                    .latitude(gridLat + gridSizeDegrees / 2) // Center of cell
                    .longitude(gridLon + gridSizeDegrees / 2)
                    .intensity(0)
                    .build());

            point.setIntensity(point.getIntensity() + 1);
            gridMap.put(gridKey, point);
        }

        return gridMap.values().stream()
                .sorted((a, b) -> b.getIntensity().compareTo(a.getIntensity()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AlertStatsDto getAlertStats(String timeRange, UUID cityId) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime dateFrom;

        // Parse time range
        switch (timeRange.toLowerCase()) {
            case "24h":
                dateFrom = now.minus(24, ChronoUnit.HOURS);
                break;
            case "7d":
                dateFrom = now.minus(7, ChronoUnit.DAYS);
                break;
            case "30d":
                dateFrom = now.minus(30, ChronoUnit.DAYS);
                break;
            default:
                dateFrom = now.minus(7, ChronoUnit.DAYS);
        }

        // Get all alerts in time range
        List<Alert> alerts = alertRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(dateFrom, now);

        // Filter by city if provided
        if (cityId != null) {
            alerts = alerts.stream()
                    .filter(a -> a.getCity() != null && a.getCity().getId().equals(cityId))
                    .collect(Collectors.toList());
        }

        // Calculate stats
        long totalAlerts = alerts.size();
        long activeAlerts = alerts.stream().filter(a -> "ACTIVE".equals(a.getStatus())).count();
        long resolvedAlerts = alerts.stream().filter(a -> "RESOLVED".equals(a.getStatus())).count();
        long cancelledAlerts = alerts.stream().filter(a -> "CANCELLED".equals(a.getStatus())).count();

        // Count by category
        Map<String, Long> byCategory = new HashMap<>();
        alerts.stream()
                .collect(Collectors.groupingBy(Alert::getCategory, Collectors.counting()))
                .forEach((key, value) -> byCategory.put(key, value));

        // Count by verification status
        Map<String, Long> byVerification = new HashMap<>();
        alerts.stream()
                .collect(Collectors.groupingBy(Alert::getVerificationStatus, Collectors.counting()))
                .forEach((key, value) -> byVerification.put(key, value));

        // Calculate false report percentage
        long falseReports = alerts.stream().filter(a -> "REJECTED".equals(a.getVerificationStatus())).count();
        double falsePercentage = totalAlerts > 0 ? (falseReports * 100.0) / totalAlerts : 0;

        // Get user stats
        List<User> activeUsers = userRepository.findByStatus("ACTIVE");
        long totalUsers = userRepository.count();

        return AlertStatsDto.builder()
                .totalAlerts(totalAlerts)
                .activeAlerts(activeAlerts)
                .resolvedAlerts(resolvedAlerts)
                .cancelledAlerts(cancelledAlerts)
                .alertsByCategory(byCategory)
                .alertsByVerificationStatus(byVerification)
                .falseReportsPercentage(falsePercentage)
                .totalUsers(totalUsers)
                .activeUsers((long) activeUsers.size())
                .timeRange(timeRange)
                .build();
    }

    private AlertDto mapToDto(Alert alert, Double distance) {
        String createdByUserName = null;
        if (!Boolean.TRUE.equals(alert.getIsAnonymous())) {
            createdByUserName = alert.getCreatedByUser().getFirstName() + " " +
                    alert.getCreatedByUser().getLastName();
        }

        return AlertDto.builder()
                .id(alert.getId())
                .createdByUserId(alert.getCreatedByUser().getId())
                .createdByUserName(createdByUserName)
                .category(alert.getCategory())
                .status(alert.getStatus())
                .verificationStatus(alert.getVerificationStatus())
                .title(alert.getTitle())
                .description(alert.getDescription())
                .isAnonymous(alert.getIsAnonymous())
                .address(alert.getAddress())
                .cityId(alert.getCity() != null ? alert.getCity().getId() : null)
                .cityName(alert.getCity() != null ? alert.getCity().getName() : null)
                .latitude(alert.getGeometry().getY())
                .longitude(alert.getGeometry().getX())
                .radiusM(alert.getRadiusM())
                .createdAt(alert.getCreatedAt())
                .updatedAt(alert.getUpdatedAt())
                .resolvedAt(alert.getResolvedAt())
                .distanceFromUserM(distance)
                .build();
    }
}
