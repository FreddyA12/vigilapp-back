package com.fram.vigilapp.controller;

import com.fram.vigilapp.dto.AlertDto;
import com.fram.vigilapp.dto.SaveAlertDto;
import com.fram.vigilapp.entity.User;
import com.fram.vigilapp.repository.UserRepository;
import com.fram.vigilapp.service.AlertService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;
    private final UserRepository userRepository;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('USER', 'MOD', 'ADMIN')")
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

    @GetMapping("/{alertId}")
    @PreAuthorize("hasAnyAuthority('USER', 'MOD', 'ADMIN')")
    public ResponseEntity<AlertDto> getAlert(@PathVariable UUID alertId) {
        try {
            AlertDto alertDto = alertService.getAlertById(alertId);
            return ResponseEntity.ok(alertDto);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/nearby")
    @PreAuthorize("hasAnyAuthority('USER', 'MOD', 'ADMIN')")
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
    @PreAuthorize("hasAnyAuthority('USER', 'MOD', 'ADMIN')")
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
    @PreAuthorize("hasAnyAuthority('USER', 'MOD', 'ADMIN')")
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
    @PreAuthorize("hasAnyAuthority('MOD', 'ADMIN')")
    public ResponseEntity<List<AlertDto>> getAlertsByStatus(@RequestParam String status) {
        List<AlertDto> alerts = alertService.getAlertsByStatus(status);
        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/by-category-status")
    @PreAuthorize("hasAnyAuthority('USER', 'MOD', 'ADMIN')")
    public ResponseEntity<List<AlertDto>> getAlertsByCategoryAndStatus(
            @RequestParam String category,
            @RequestParam String status
    ) {
        List<AlertDto> alerts = alertService.getAlertsByCategoryAndStatus(category, status);
        return ResponseEntity.ok(alerts);
    }

    @PutMapping("/{alertId}/status")
    @PreAuthorize("hasAnyAuthority('MOD', 'ADMIN')")
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
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<Void> deleteAlert(@PathVariable UUID alertId) {
        try {
            alertService.deleteAlert(alertId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
