package com.fram.vigilapp.service.impl;

import com.fram.vigilapp.dto.AlertDto;
import com.fram.vigilapp.entity.Alert;
import com.fram.vigilapp.entity.User;
import com.fram.vigilapp.entity.UserZone;
import com.fram.vigilapp.repository.UserRepository;
import com.fram.vigilapp.repository.UserZoneRepository;
import com.fram.vigilapp.service.AlertNotificationService;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Point;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AlertNotificationServiceImpl implements AlertNotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final UserZoneRepository userZoneRepository;
    private final UserRepository userRepository;

    // Map de usuarioId -> Set de sessionIds (para manejar múltiples conexiones)
    private final Map<UUID, Set<String>> connectedUsers = new ConcurrentHashMap<>();

    @Override
    public void notifyNewAlert(Alert alert, AlertDto alertDto) {
        Point alertPoint = alert.getGeometry();

        // Obtener todos los usuarios conectados
        for (UUID userId : connectedUsers.keySet()) {
            // No notificar al creador de la alerta
            if (userId.equals(alert.getCreatedByUser().getId())) {
                continue;
            }

            // Verificar si el usuario tiene zona configurada
            UserZone userZone = userZoneRepository.findByUserId(userId).orElse(null);

            // Si tiene zona, verificar si la alerta está dentro
            if (userZone != null) {
                // Usar PostGIS: ST_Intersects para verificar si alerta está en zona
                boolean isInZone = userZone.getGeometry().intersects(alertPoint);

                if (isInZone) {
                    // Enviar notificación solo a este usuario
                    sendAlertNotificationToUser(userId, alertDto);
                }
            }
        }
    }

    @Override
    public void registerUser(UUID userId, String sessionId) {
        connectedUsers.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
        System.out.println("Usuario conectado: " + userId + " (sesión: " + sessionId + ")");
    }

    @Override
    public void unregisterUser(UUID userId, String sessionId) {
        Set<String> sessions = connectedUsers.get(userId);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                connectedUsers.remove(userId);
                System.out.println("Usuario desconectado: " + userId);
            }
        }
    }

    @Override
    public long getConnectedUsersCount() {
        return connectedUsers.size();
    }

    /**
     * Enviar notificación a un usuario específico
     */
    private void sendAlertNotificationToUser(UUID userId, AlertDto alertDto) {
        try {
            // Construir mensaje con detalles de la alerta
            AlertNotificationMessage message = AlertNotificationMessage.builder()
                    .event("NEW_ALERT")
                    .alertId(alertDto.getId())
                    .alertTitle(alertDto.getTitle())
                    .alertCategory(alertDto.getCategory())
                    .alertDescription(alertDto.getDescription())
                    .latitude(alertDto.getLatitude())
                    .longitude(alertDto.getLongitude())
                    .createdByUserName(alertDto.getCreatedByUserName())
                    .timestamp(System.currentTimeMillis())
                    .build();

            // Enviar a través de WebSocket: /topic/alerts/{userId}
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/topic/alerts",
                    message
            );
        } catch (Exception e) {
            System.err.println("Error enviando notificación a usuario " + userId + ": " + e.getMessage());
        }
    }

    /**
     * Enviar alerta a todos los usuarios conectados (sin filtro de zona)
     */
    public void broadcastAlert(AlertDto alertDto) {
        try {
            AlertNotificationMessage message = AlertNotificationMessage.builder()
                    .event("NEW_ALERT")
                    .alertId(alertDto.getId())
                    .alertTitle(alertDto.getTitle())
                    .alertCategory(alertDto.getCategory())
                    .alertDescription(alertDto.getDescription())
                    .latitude(alertDto.getLatitude())
                    .longitude(alertDto.getLongitude())
                    .createdByUserName(alertDto.getCreatedByUserName())
                    .timestamp(System.currentTimeMillis())
                    .build();

            messagingTemplate.convertAndSend("/topic/alerts/broadcast", message);
        } catch (Exception e) {
            System.err.println("Error broadcasting alert: " + e.getMessage());
        }
    }
}
