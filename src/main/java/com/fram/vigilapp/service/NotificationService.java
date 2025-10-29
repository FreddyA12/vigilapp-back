package com.fram.vigilapp.service;

import com.fram.vigilapp.dto.NotificationDto;
import com.fram.vigilapp.dto.SaveNotificationDto;
import com.fram.vigilapp.entity.Alert;
import com.fram.vigilapp.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface NotificationService {

    /**
     * Get paginated notifications for a user
     */
    Page<NotificationDto> getUserNotifications(UUID userId, Pageable pageable);

    /**
     * Get undelivered notifications for a user
     */
    List<NotificationDto> getUndeliveredNotifications(UUID userId);

    /**
     * Mark a notification as delivered
     */
    NotificationDto markAsDelivered(UUID notificationId);

    /**
     * Get single notification by ID
     */
    NotificationDto getNotificationById(UUID notificationId);

    /**
     * Delete a notification
     */
    void deleteNotification(UUID notificationId);

    /**
     * Notify all users whose zone contains the alert
     * Called automatically after alert creation
     */
    void notifyUsersInZone(Alert alert, String channel);

    /**
     * Notify users within a specific radius from alert location
     */
    void notifyUsersInRadius(Alert alert, Integer radiusM, String channel);

    /**
     * Get notifications for an alert
     */
    List<NotificationDto> getNotificationsByAlert(UUID alertId);

    /**
     * Create a notification manually
     */
    NotificationDto createNotification(User user, Alert alert, String channel);

    /**
     * Get queued notifications (for processing/sending)
     */
    List<NotificationDto> getQueuedNotifications();

    /**
     * Get queued notifications for a specific channel
     */
    List<NotificationDto> getQueuedNotificationsByChannel(String channel);

    /**
     * Mark notification as sent
     */
    NotificationDto markAsSent(UUID notificationId);

    /**
     * Mark notification as failed
     */
    NotificationDto markAsFailed(UUID notificationId);

    /**
     * Count unread notifications for a user
     */
    long countUndeliveredNotifications(UUID userId);

    /**
     * Delete old notifications (older than specified days)
     */
    void deleteOldNotifications(int daysOld);
}
