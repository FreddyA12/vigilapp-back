package com.fram.vigilapp.config;

import com.fram.vigilapp.websocket.AlertWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Configuración de WebSocket para conexiones raw (sin STOMP)
 * Compatible con React Native WebSocket API
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class RawWebSocketConfig implements WebSocketConfigurer {

    private final AlertWebSocketHandler alertWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(alertWebSocketHandler, "/ws/alerts")
                .setAllowedOrigins("*"); // Permitir todos los orígenes (ajustar en producción)
    }
}
