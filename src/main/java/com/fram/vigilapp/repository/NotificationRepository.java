package com.fram.vigilapp.repository;

import com.fram.vigilapp.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /**
     * Find all notifications for a user, ordered by creation date (newest first)
     */
    Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Find undelivered notifications for a user
     */
    List<Notification> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, String status);

    /**
     * Find notifications for an alert
     */
    List<Notification> findByAlertIdOrderByCreatedAtDesc(UUID alertId);

    /**
     * Find queued notifications (ready to be sent)
     */
    @Query("SELECT n FROM Notification n WHERE n.status = 'QUEUED' ORDER BY n.createdAt ASC")
    List<Notification> findQueuedNotifications();

    /**
     * Find queued notifications for a specific channel
     */
    @Query("SELECT n FROM Notification n WHERE n.status = 'QUEUED' AND n.channel = :channel ORDER BY n.createdAt ASC")
    List<Notification> findQueuedNotificationsByChannel(@Param("channel") String channel);

    /**
     * Find failed notifications (for retry logic)
     */
    @Query("SELECT n FROM Notification n WHERE n.status = 'FAILED' ORDER BY n.createdAt ASC")
    List<Notification> findFailedNotifications();

    /**
     * Count undelivered notifications for a user
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.id = :userId AND n.status IN ('QUEUED', 'SENT')")
    long countUndeliveredNotifications(@Param("userId") UUID userId);

    /**
     * Find notifications by multiple statuses
     */
    List<Notification> findByStatusInOrderByCreatedAtDesc(List<String> statuses);
}
