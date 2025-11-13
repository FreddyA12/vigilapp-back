package com.fram.vigilapp.service;

import com.fram.vigilapp.dto.AlertDto;
import com.fram.vigilapp.entity.Alert;

import java.util.UUID;

/**
 * Servicio para enviar notificaciones de alertas en tiempo real via WebSocket
 */
public interface AlertNotificationService {

    /**
     * Notificar a todos los usuarios conectados sobre una nueva alerta
     * Si un usuario tiene zona configurada, solo recibe si alerta está en su zona
     */
    void notifyNewAlert(Alert alert, AlertDto alertDto);

    /**
     * Registrar un usuario conectado al WebSocket
     */
    void registerUser(UUID userId, String sessionId);

    /**
     * Desregistrar un usuario cuando se desconecta
     */
    void unregisterUser(UUID userId, String sessionId);

    /**
     * Obtener cantidad de usuarios conectados
     */
    long getConnectedUsersCount();
}
