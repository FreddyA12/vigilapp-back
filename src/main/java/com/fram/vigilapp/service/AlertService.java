package com.fram.vigilapp.service;

import com.fram.vigilapp.dto.AlertDto;
import com.fram.vigilapp.dto.SaveAlertDto;
import com.fram.vigilapp.entity.User;

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
}
