package com.fram.vigilapp.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fram.vigilapp.repository.UserRepository;
import com.fram.vigilapp.service.AlertNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handler para WebSocket raw (sin STOMP)
 * Maneja conexiones desde React Native
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AlertWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UserRepository userRepository;

    // Mapa de sessionId -> userId (ahora acepta email o UUID)
    private final Map<String, String> sessionUserMap = new ConcurrentHashMap<>();

    // Mapa de userId -> sessionId (para enviar mensajes a usuarios específicos)
    private final Map<String, String> userSessionMap = new ConcurrentHashMap<>();

    // Mapa de sessionId -> WebSocketSession (para enviar mensajes)
    private final Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();

    // Mapa de email -> UUID para tracking
    private final Map<String, UUID> emailToUuidMap = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket connection established: {}", session.getId());

        // Guardar sesión activa
        activeSessions.put(session.getId(), session);

        // Enviar mensaje de bienvenida
        Map<String, String> welcome = Map.of(
            "type", "CONNECTION_ESTABLISHED",
            "message", "Conectado al servidor de alertas"
        );
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(welcome)));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.info("Received message from {}: {}", session.getId(), payload);

        try {
            Map<String, Object> data = objectMapper.readValue(payload, Map.class);
            String messageType = (String) data.get("type");

            switch (messageType) {
                case "REGISTER":
                    handleRegister(session, data);
                    break;
                case "UNREGISTER":
                    handleUnregister(session, data);
                    break;
                case "PING":
                    handlePing(session);
                    break;
                default:
                    log.warn("Unknown message type: {}", messageType);
            }
        } catch (Exception e) {
            log.error("Error processing message: {}", e.getMessage(), e);
            sendError(session, "Error procesando mensaje: " + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("WebSocket connection closed: {} with status {}", session.getId(), status);

        // Limpiar registros del usuario
        String userId = sessionUserMap.remove(session.getId());
        if (userId != null) {
            userSessionMap.remove(userId);
            emailToUuidMap.remove(userId);
            log.info("User {} unregistered due to connection close", userId);
        }

        // Limpiar sesión activa
        activeSessions.remove(session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket error for session {}: {}", session.getId(), exception.getMessage(), exception);
    }

    /**
     * Registrar un usuario con su sesión WebSocket
     * Acepta email o UUID como identificador
     */
    private void handleRegister(WebSocketSession session, Map<String, Object> data) throws IOException {
        String userId = (String) data.get("userId");
        if (userId == null || userId.isEmpty()) {
            sendError(session, "userId es requerido");
            return;
        }

        // Guardar mapeos (ahora acepta cualquier string: email o UUID)
        sessionUserMap.put(session.getId(), userId);
        userSessionMap.put(userId, session.getId());

        // Guardar mapeo email -> UUID
        try {
            var user = userRepository.findByEmail(userId);
            if (user != null) {
                emailToUuidMap.put(userId, user.getId());
                log.info("User {} (UUID: {}) registered with session {}", userId, user.getId(), session.getId());
            } else {
                log.warn("User with email {} not found in database", userId);
            }
        } catch (Exception e) {
            log.error("Error looking up user: {}", e.getMessage());
        }

        // Confirmar registro
        Map<String, Object> response = Map.of(
            "type", "REGISTERED",
            "message", "Usuario registrado correctamente",
            "userId", userId
        );
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
    }

    /**
     * Desregistrar un usuario
     */
    private void handleUnregister(WebSocketSession session, Map<String, Object> data) throws IOException {
        String userId = sessionUserMap.remove(session.getId());
        if (userId != null) {
            userSessionMap.remove(userId);
            emailToUuidMap.remove(userId);

            log.info("User {} unregistered", userId);

            Map<String, String> response = Map.of(
                "type", "UNREGISTERED",
                "message", "Usuario desregistrado correctamente"
            );
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
        }
    }

    /**
     * Responder a ping
     */
    private void handlePing(WebSocketSession session) throws IOException {
        Map<String, String> pong = Map.of(
            "type", "PONG",
            "timestamp", String.valueOf(System.currentTimeMillis())
        );
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(pong)));
    }

    /**
     * Enviar mensaje de error
     */
    private void sendError(WebSocketSession session, String errorMessage) {
        try {
            Map<String, String> error = Map.of(
                "type", "ERROR",
                "message", errorMessage
            );
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(error)));
        } catch (IOException e) {
            log.error("Error sending error message: {}", e.getMessage(), e);
        }
    }

    /**
     * Enviar notificación de alerta a un usuario específico
     * Llamado por AlertNotificationService cuando se crea una alerta
     * @param userId Puede ser email o UUID
     */
    public void sendAlertToUser(String userId, Map<String, Object> alertData) {
        String sessionId = userSessionMap.get(userId);
        if (sessionId == null) {
            log.debug("User {} is not connected", userId);
            return;
        }

        WebSocketSession session = activeSessions.get(sessionId);
        if (session == null || !session.isOpen()) {
            log.warn("Session {} for user {} is not available or closed", sessionId, userId);
            userSessionMap.remove(userId);
            return;
        }

        try {
            String message = objectMapper.writeValueAsString(alertData);
            session.sendMessage(new TextMessage(message));
            log.info("Alert sent to user {} via session {}", userId, sessionId);
        } catch (IOException e) {
            log.error("Error sending alert to user {}: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * Obtener cantidad de usuarios conectados
     */
    public int getConnectedUsersCount() {
        return userSessionMap.size();
    }

    /**
     * Obtener todos los UUIDs de usuarios conectados
     */
    public Set<UUID> getConnectedUserIds() {
        return new HashSet<>(emailToUuidMap.values());
    }

    /**
     * Verificar si un usuario está conectado por UUID
     */
    public boolean isUserConnected(UUID userId) {
        return emailToUuidMap.containsValue(userId);
    }
}
