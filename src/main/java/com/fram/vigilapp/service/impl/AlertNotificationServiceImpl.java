package com.fram.vigilapp.service.impl;

import com.fram.vigilapp.dto.AlertDto;
import com.fram.vigilapp.entity.Alert;
import com.fram.vigilapp.entity.User;
import com.fram.vigilapp.entity.UserZone;
import com.fram.vigilapp.repository.UserRepository;
import com.fram.vigilapp.repository.UserZoneRepository;
import com.fram.vigilapp.service.AlertNotificationService;
import com.fram.vigilapp.websocket.AlertWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertNotificationServiceImpl implements AlertNotificationService {

    private final AlertWebSocketHandler webSocketHandler;
    private final UserZoneRepository userZoneRepository;
    private final UserRepository userRepository;

    // Map de usuarioId -> Set de sessionIds (para manejar múltiples conexiones)
    // Este mapa es legacy, ahora el handler mantiene las conexiones
    private final Map<UUID, Set<String>> connectedUsers = new ConcurrentHashMap<>();

    @Override
    public void notifyNewAlert(Alert alert, AlertDto alertDto) {
        Point alertPoint = alert.getGeometry();

        // Obtener todos los usuarios conectados desde el handler
        Set<UUID> connectedUserIds = webSocketHandler.getConnectedUserIds();

        log.info("Notificando alerta {} a {} usuarios conectados", alert.getId(), connectedUserIds.size());

        // Notificar a cada usuario conectado
        for (UUID userId : connectedUserIds) {
            // No notificar al creador de la alerta
            if (userId.equals(alert.getCreatedByUser().getId())) {
                log.debug("Omitiendo notificación al creador de la alerta: {}", userId);
                continue;
            }

            // Verificar si el usuario tiene zona configurada
            UserZone userZone = userZoneRepository.findByUserId(userId).orElse(null);

            // Si tiene zona, verificar si la alerta está dentro
            if (userZone != null) {
                // Usar PostGIS: ST_Intersects para verificar si alerta está en zona
                boolean isInZone = userZone.getGeometry().intersects(alertPoint);

                if (isInZone) {
                    log.info("Alerta {} está en la zona del usuario {}, enviando notificación", alert.getId(), userId);
                    // Enviar notificación solo a este usuario
                    sendAlertNotificationToUser(userId, alertDto);
                } else {
                    log.debug("Alerta {} NO está en la zona del usuario {}", alert.getId(), userId);
                }
            } else {
                log.debug("Usuario {} no tiene zona configurada", userId);
            }
        }
    }

    @Override
    public void registerUser(UUID userId, String sessionId) {
        connectedUsers.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
        log.info("Usuario {} registrado para notificaciones (sesión: {})", userId, sessionId);
    }

    @Override
    public void unregisterUser(UUID userId, String sessionId) {
        Set<String> sessions = connectedUsers.get(userId);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                connectedUsers.remove(userId);
                log.info("Usuario {} desregistrado de notificaciones", userId);
            }
        }
    }

    @Override
    public long getConnectedUsersCount() {
        return webSocketHandler.getConnectedUsersCount();
    }

    /**
     * Enviar notificación a un usuario específico
     */
    private void sendAlertNotificationToUser(UUID userId, AlertDto alertDto) {
        try {
            // Construir mensaje con detalles de la alerta
            Map<String, Object> message = new HashMap<>();
            message.put("event", "NEW_ALERT");
            message.put("alertId", alertDto.getId());
            message.put("alertTitle", alertDto.getTitle());
            message.put("alertCategory", alertDto.getCategory());
            message.put("alertDescription", alertDto.getDescription());
            message.put("latitude", alertDto.getLatitude());
            message.put("longitude", alertDto.getLongitude());
            message.put("createdByUserName", alertDto.getCreatedByUserName());
            message.put("timestamp", System.currentTimeMillis());

            // Enviar a través de WebSocket raw usando email como ID
            var userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                webSocketHandler.sendAlertToUser(user.getEmail(), message);
                log.info("✅ Notificación enviada a: {}", user.getEmail());
            }
        } catch (Exception e) {
            System.err.println("Error enviando notificación a usuario " + userId + ": " + e.getMessage());
        }
    }

    /**
     * Enviar alerta a todos los usuarios conectados (sin filtro de zona)
     */
    public void broadcastAlert(AlertDto alertDto) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("event", "NEW_ALERT");
            message.put("alertId", alertDto.getId());
            message.put("alertTitle", alertDto.getTitle());
            message.put("alertCategory", alertDto.getCategory());
            message.put("alertDescription", alertDto.getDescription());
            message.put("latitude", alertDto.getLatitude());
            message.put("longitude", alertDto.getLongitude());
            message.put("createdByUserName", alertDto.getCreatedByUserName());
            message.put("timestamp", System.currentTimeMillis());

            // Enviar a todos los usuarios conectados
            for (UUID userId : connectedUsers.keySet()) {
                var userOpt = userRepository.findById(userId);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    webSocketHandler.sendAlertToUser(user.getEmail(), message);
                }
            }
        } catch (Exception e) {
            System.err.println("Error broadcasting alert: " + e.getMessage());
        }
    }
}
