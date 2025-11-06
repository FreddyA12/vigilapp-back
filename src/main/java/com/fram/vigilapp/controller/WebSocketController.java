package com.fram.vigilapp.controller;

import com.fram.vigilapp.service.AlertNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.UUID;

/**
 * WebSocket Controller para manejar suscripciones y notificaciones en tiempo real
 * Endpoint: ws://localhost:8080/ws/alerts
 */
@Controller
@RequestMapping("/api")
@RequiredArgsConstructor
public class WebSocketController {

    private final AlertNotificationService alertNotificationService;

    /**
     * Manejar cuando un usuario se conecta al WebSocket
     * Cliente envía: {"userId": "uuid-del-usuario"}
     */
    @MessageMapping("/alerts/register")
    public void registerUser(@Payload UserRegistrationMessage registration,
                             SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        UUID userId = UUID.fromString(registration.getUserId());
        alertNotificationService.registerUser(userId, sessionId);
    }

    /**
     * Manejar cuando un usuario se desconecta
     * Cliente envía: {"userId": "uuid-del-usuario"}
     */
    @MessageMapping("/alerts/unregister")
    public void unregisterUser(@Payload UserRegistrationMessage registration,
                               SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        UUID userId = UUID.fromString(registration.getUserId());
        alertNotificationService.unregisterUser(userId, sessionId);
    }

    /**
     * Endpoint REST para obtener cantidad de usuarios conectados
     * GET /api/alerts/connected-users
     */
    @GetMapping("/alerts/connected-users")
    @ResponseBody
    public ConnectedUsersResponse getConnectedUsers() {
        return ConnectedUsersResponse.builder()
                .connectedUsers(alertNotificationService.getConnectedUsersCount())
                .build();
    }

    /**
     * DTO para registro de usuario en WebSocket
     */
    public static class UserRegistrationMessage {
        public String userId;

        public UserRegistrationMessage() {}
        public UserRegistrationMessage(String userId) {
            this.userId = userId;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }
    }

    /**
     * DTO para respuesta de usuarios conectados
     */
    @lombok.Getter
    @lombok.Setter
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    @lombok.Builder
    public static class ConnectedUsersResponse {
        private long connectedUsers;
    }
}
