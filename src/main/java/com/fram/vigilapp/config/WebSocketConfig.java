package com.fram.vigilapp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Habilita un message broker simple en memoria
        config.enableSimpleBroker("/topic");
        // Define el prefijo para mensajes del cliente
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint WebSocket (SockJS como fallback para navegadores viejos)
        registry.addEndpoint("/ws/alerts")
                .setAllowedOrigins("http://localhost:4200", "http://localhost:3000", "*")
                .withSockJS();
    }
}
