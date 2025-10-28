package com.fram.vigilapp.service.impl;

import com.fram.vigilapp.dto.AlertDto;
import com.fram.vigilapp.dto.SaveAlertDto;
import com.fram.vigilapp.entity.Alert;
import com.fram.vigilapp.entity.City;
import com.fram.vigilapp.entity.User;
import com.fram.vigilapp.entity.UserZone;
import com.fram.vigilapp.repository.AlertRepository;
import com.fram.vigilapp.repository.CityRepository;
import com.fram.vigilapp.repository.UserZoneRepository;
import com.fram.vigilapp.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AlertServiceImpl implements AlertService {

    private final AlertRepository alertRepository;
    private final UserZoneRepository userZoneRepository;
    private final CityRepository cityRepository;
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

        return mapToDto(alert, null);
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
