package com.fram.vigilapp.controller;

import com.fram.vigilapp.dto.AlertDto;
import com.fram.vigilapp.dto.AlertStatsDto;
import com.fram.vigilapp.dto.HeatmapPointDto;
import com.fram.vigilapp.dto.SaveAlertDto;
import com.fram.vigilapp.entity.User;
import com.fram.vigilapp.repository.UserRepository;
import com.fram.vigilapp.service.AlertService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;
    private final UserRepository userRepository;

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'MOD', 'ADMIN')")
    public ResponseEntity<AlertDto> createAlert(
            @Valid @RequestBody SaveAlertDto saveAlertDto,
            Authentication authentication
    ) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        AlertDto alertDto = alertService.createAlert(user, saveAlertDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(alertDto);
    }

    /**
     * Crea una alerta con archivos adjuntos (evidencia).
     * Los archivos que son imágenes serán procesados automáticamente para difuminar caras.
     *
     * POST /api/alerts/with-media
     */
    @PostMapping(value = "/with-media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('USER', 'MOD', 'ADMIN')")
    public ResponseEntity<AlertDto> createAlertWithMedia(
            @RequestPart("alert") String alertJson,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            Authentication authentication
    ) {
        try {
            String email = authentication.getName();
            User user = userRepository.findByEmail(email);

            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // Parsear JSON a SaveAlertDto
            ObjectMapper objectMapper = new ObjectMapper();
            SaveAlertDto saveAlertDto = objectMapper.readValue(alertJson, SaveAlertDto.class);

            // Crear alerta con archivos adjuntos
            AlertDto alertDto = alertService.createAlertWithMedia(user, saveAlertDto, files);
            return ResponseEntity.status(HttpStatus.CREATED).body(alertDto);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/{alertId}")
    @PreAuthorize("hasAnyRole('USER', 'MOD', 'ADMIN')")
    public ResponseEntity<AlertDto> getAlert(@PathVariable UUID alertId) {
        try {
            AlertDto alertDto = alertService.getAlertById(alertId);
            return ResponseEntity.ok(alertDto);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/nearby")
    @PreAuthorize("hasAnyRole('USER', 'MOD', 'ADMIN')")
    public ResponseEntity<List<AlertDto>> getAlertsNearLocation(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "5000") Integer radiusM,
            @RequestParam(defaultValue = "true") boolean activeOnly
    ) {
        List<AlertDto> alerts = alertService.getAlertsNearLocation(latitude, longitude, radiusM, activeOnly);
        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/my-zone")
    @PreAuthorize("hasAnyRole('USER', 'MOD', 'ADMIN')")
    public ResponseEntity<List<AlertDto>> getAlertsInMyZone(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            List<AlertDto> alerts = alertService.getAlertsInUserZone(user.getId());
            return ResponseEntity.ok(alerts);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @GetMapping("/my-alerts")
    @PreAuthorize("hasAnyRole('USER', 'MOD', 'ADMIN')")
    public ResponseEntity<List<AlertDto>> getMyAlerts(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<AlertDto> alerts = alertService.getUserAlerts(user.getId());
        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/by-status")
    @PreAuthorize("hasAnyRole('MOD', 'ADMIN')")
    public ResponseEntity<List<AlertDto>> getAlertsByStatus(@RequestParam String status) {
        List<AlertDto> alerts = alertService.getAlertsByStatus(status);
        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/by-category-status")
    @PreAuthorize("hasAnyRole('USER', 'MOD', 'ADMIN')")
    public ResponseEntity<List<AlertDto>> getAlertsByCategoryAndStatus(
            @RequestParam String category,
            @RequestParam String status
    ) {
        List<AlertDto> alerts = alertService.getAlertsByCategoryAndStatus(category, status);
        return ResponseEntity.ok(alerts);
    }

    @PutMapping("/{alertId}/status")
    @PreAuthorize("hasAnyRole('MOD', 'ADMIN')")
    public ResponseEntity<AlertDto> updateAlertStatus(
            @PathVariable UUID alertId,
            @RequestParam String status
    ) {
        try {
            AlertDto alertDto = alertService.updateAlertStatus(alertId, status);
            return ResponseEntity.ok(alertDto);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{alertId}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<Void> deleteAlert(@PathVariable UUID alertId) {
        try {
            alertService.deleteAlert(alertId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get recent alerts with pagination
     * GET /api/alerts/recent?page=0&size=20
     */
    @GetMapping("/recent")
    @PreAuthorize("hasAnyRole('USER', 'MOD', 'ADMIN')")
    public ResponseEntity<Page<AlertDto>> getRecentAlerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AlertDto> alerts = alertService.getRecentAlerts(pageable);
        return ResponseEntity.ok(alerts);
    }

    /**
     * Advanced alert search with multiple filters
     * GET /api/alerts/search?query=incendio&category=EMERGENCY&status=ACTIVE&skip=0&limit=20
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('USER', 'MOD', 'ADMIN')")
    public ResponseEntity<List<AlertDto>> searchAlerts(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String verificationStatus,
            @RequestParam(required = false) UUID cityId,
            @RequestParam(required = false) Integer minRadiusM,
            @RequestParam(required = false) Integer maxRadiusM,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateTo,
            @RequestParam(defaultValue = "0") int skip,
            @RequestParam(defaultValue = "50") int limit) {

        List<AlertDto> alerts = alertService.searchAlerts(
                query, category, status, verificationStatus, cityId,
                minRadiusM, maxRadiusM, dateFrom, dateTo, skip, limit);
        return ResponseEntity.ok(alerts);
    }

    /**
     * Get heatmap data for density visualization
     * GET /api/alerts/heatmap?swLat=10.0&swLon=-75.0&neLat=11.0&neLon=-74.0&gridSizeM=1000
     */
    @GetMapping("/heatmap")
    @PreAuthorize("hasAnyRole('USER', 'MOD', 'ADMIN')")
    public ResponseEntity<List<HeatmapPointDto>> getHeatmapData(
            @RequestParam Double swLat,
            @RequestParam Double swLon,
            @RequestParam Double neLat,
            @RequestParam Double neLon,
            @RequestParam(defaultValue = "1000") Double gridSizeM) {

        List<HeatmapPointDto> heatmapData = alertService.getHeatmapData(swLat, swLon, neLat, neLon, gridSizeM);
        return ResponseEntity.ok(heatmapData);
    }

    /**
     * Get alert statistics
     * GET /api/alerts/stats?timeRange=7d&cityId=<uuid>
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('USER', 'MOD', 'ADMIN')")
    public ResponseEntity<AlertStatsDto> getAlertStats(
            @RequestParam(defaultValue = "7d") String timeRange,
            @RequestParam(required = false) UUID cityId) {

        AlertStatsDto stats = alertService.getAlertStats(timeRange, cityId);
        return ResponseEntity.ok(stats);
    }
}
