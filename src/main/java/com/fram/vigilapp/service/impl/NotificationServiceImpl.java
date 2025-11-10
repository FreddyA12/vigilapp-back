package com.fram.vigilapp.service.impl;

import com.fram.vigilapp.dto.NotificationDto;
import com.fram.vigilapp.dto.SaveNotificationDto;
import com.fram.vigilapp.entity.Alert;
import com.fram.vigilapp.entity.Notification;
import com.fram.vigilapp.entity.User;
import com.fram.vigilapp.entity.UserZone;
import com.fram.vigilapp.repository.AlertRepository;
import com.fram.vigilapp.repository.NotificationRepository;
import com.fram.vigilapp.repository.UserRepository;
import com.fram.vigilapp.repository.UserZoneRepository;
import com.fram.vigilapp.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserZoneRepository userZoneRepository;
    private final AlertRepository alertRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationDto> getUserNotifications(UUID userId, Pageable pageable) {
        // Traer solo notificaciones no borradas
        return notificationRepository.findByUserIdAndNotDeleted(userId, pageable)
                .map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDto> getUndeliveredNotifications(UUID userId) {
        List<String> undeliveredStatuses = List.of("QUEUED", "SENT");
        return notificationRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, "QUEUED").stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public NotificationDto markAsDelivered(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notificación no encontrada"));

        notification.setStatus("DELIVERED");
        notification.setDeliveredAt(OffsetDateTime.now());
        notification = notificationRepository.save(notification);

        return mapToDto(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationDto getNotificationById(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notificación no encontrada"));

        return mapToDto(notification);
    }

    @Override
    @Transactional
    public void deleteNotification(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notificación no encontrada"));

        // Borrado lógico
        notification.setDeletedAt(OffsetDateTime.now());
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void notifyUsersInZone(Alert alert, String channel) {
        // Find all users whose zone intersects with the alert
        List<User> usersToNotify = userRepository.findUsersInZone(alert.getGeometry());

        // Filter out the alert creator
        usersToNotify = usersToNotify.stream()
                .filter(user -> !user.getId().equals(alert.getCreatedByUser().getId()))
                .collect(Collectors.toList());

        // Create notification for each user
        for (User user : usersToNotify) {
            createNotification(user, alert, channel);
        }
    }

    @Override
    @Transactional
    public void notifyUsersInRadius(Alert alert, Integer radiusM, String channel) {
        // Find all users within the specified radius
        List<User> allUsers = userRepository.findAll();

        for (User user : allUsers) {
            UserZone userZone = userZoneRepository.findByUserId(user.getId()).orElse(null);
            if (userZone != null) {
                // Check if alert is within user's zone radius
                Double distance = alertRepository.calculateDistanceFromPoint(
                        alert.getId(),
                        userZone.getGeometry().getCentroid().getY(),
                        userZone.getGeometry().getCentroid().getX()
                );

                if (distance != null && distance <= radiusM) {
                    createNotification(user, alert, channel);
                }
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDto> getNotificationsByAlert(UUID alertId) {
        return notificationRepository.findByAlertIdOrderByCreatedAtDesc(alertId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public NotificationDto createNotification(User user, Alert alert, String channel) {
        Notification notification = Notification.builder()
                .alert(alert)
                .user(user)
                .channel(channel)
                .status("QUEUED")
                .createdAt(OffsetDateTime.now())
                .build();

        notification = notificationRepository.save(notification);
        return mapToDto(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDto> getQueuedNotifications() {
        return notificationRepository.findQueuedNotifications().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDto> getQueuedNotificationsByChannel(String channel) {
        return notificationRepository.findQueuedNotificationsByChannel(channel).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public NotificationDto markAsSent(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notificación no encontrada"));

        notification.setStatus("SENT");
        notification.setSentAt(OffsetDateTime.now());
        notification = notificationRepository.save(notification);

        return mapToDto(notification);
    }

    @Override
    @Transactional
    public NotificationDto markAsFailed(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notificación no encontrada"));

        notification.setStatus("FAILED");
        notification = notificationRepository.save(notification);

        return mapToDto(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUndeliveredNotifications(UUID userId) {
        return notificationRepository.countUndeliveredNotifications(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnreadNotifications(UUID userId) {
        return notificationRepository.countUnreadNotifications(userId);
    }

    @Override
    @Transactional
    public NotificationDto markAsRead(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notificación no encontrada"));

        notification.setReadAt(OffsetDateTime.now());
        notification = notificationRepository.save(notification);

        return mapToDto(notification);
    }

    @Override
    @Transactional
    public int markAllAsRead(UUID userId) {
        return notificationRepository.markAllAsReadForUser(userId);
    }

    @Override
    @Transactional
    public void deleteOldNotifications(int daysOld) {
        OffsetDateTime cutoffDate = OffsetDateTime.now().minus(daysOld, ChronoUnit.DAYS);
        List<Notification> notifications = notificationRepository.findAll().stream()
                .filter(n -> n.getCreatedAt().isBefore(cutoffDate))
                .collect(Collectors.toList());

        notificationRepository.deleteAll(notifications);
    }

    private NotificationDto mapToDto(Notification notification) {
        return NotificationDto.builder()
                .id(notification.getId())
                .alertId(notification.getAlert().getId())
                .alertTitle(notification.getAlert().getTitle())
                .alertCategory(notification.getAlert().getCategory())
                .userId(notification.getUser().getId())
                .channel(notification.getChannel())
                .status(notification.getStatus())
                .sentAt(notification.getSentAt())
                .deliveredAt(notification.getDeliveredAt())
                .readAt(notification.getReadAt())
                .isRead(notification.getReadAt() != null)
                .deletedAt(notification.getDeletedAt())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
