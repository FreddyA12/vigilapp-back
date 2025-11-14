package com.fram.vigilapp.service.impl;

import com.fram.vigilapp.dto.NotificationDto;
import com.fram.vigilapp.entity.Alert;
import com.fram.vigilapp.entity.Notification;
import com.fram.vigilapp.entity.User;
import com.fram.vigilapp.repository.AlertRepository;
import com.fram.vigilapp.repository.NotificationRepository;
import com.fram.vigilapp.repository.UserRepository;
import com.fram.vigilapp.repository.UserZoneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserZoneRepository userZoneRepository;

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private UUID userId;
    private UUID alertId;
    private UUID notificationId;
    private User user;
    private Alert alert;
    private Notification notification;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        alertId = UUID.randomUUID();
        notificationId = UUID.randomUUID();

        user = User.builder()
                .id(userId)
                .email("test@example.com")
                .build();

        alert = Alert.builder()
                .id(alertId)
                .title("Test Alert")
                .description("Test Description")
                .category("EMERGENCY")
                .createdByUser(user)
                .build();

        notification = Notification.builder()
                .id(notificationId)
                .alert(alert)
                .user(user)
                .channel("PUSH")
                .status("QUEUED")
                .createdAt(OffsetDateTime.now())
                .build();
    }

    @Test
    void getUserNotifications_ShouldReturnPageOfNotifications() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Notification> notifications = Arrays.asList(notification);
        Page<Notification> notificationPage = new PageImpl<>(notifications, pageable, 1);

        when(notificationRepository.findByUserIdAndNotDeleted(userId, pageable)).thenReturn(notificationPage);

        // When
        Page<NotificationDto> result = notificationService.getUserNotifications(userId, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(notificationRepository).findByUserIdAndNotDeleted(userId, pageable);
    }

    @Test
    void getUndeliveredNotifications_ShouldReturnQueuedNotifications() {
        // Given
        when(notificationRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, "QUEUED"))
                .thenReturn(Arrays.asList(notification));

        // When
        List<NotificationDto> result = notificationService.getUndeliveredNotifications(userId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(notificationRepository).findByUserIdAndStatusOrderByCreatedAtDesc(userId, "QUEUED");
    }

    @Test
    void markAsDelivered_ShouldUpdateNotificationStatus() {
        // Given
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        // When
        NotificationDto result = notificationService.markAsDelivered(notificationId);

        // Then
        assertNotNull(result);
        assertEquals("DELIVERED", notification.getStatus());
        assertNotNull(notification.getDeliveredAt());
        verify(notificationRepository).findById(notificationId);
        verify(notificationRepository).save(notification);
    }

    @Test
    void markAsDelivered_WithNonExistentNotification_ShouldThrowException() {
        // Given
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class,
                () -> notificationService.markAsDelivered(notificationId));

        verify(notificationRepository).findById(notificationId);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markAsRead_ShouldUpdateReadTimestamp() {
        // Given
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        // When
        NotificationDto result = notificationService.markAsRead(notificationId);

        // Then
        assertNotNull(result);
        assertNotNull(notification.getReadAt());
        verify(notificationRepository).save(notification);
    }

    @Test
    void deleteNotification_ShouldSetDeletedAt() {
        // Given
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        // When
        notificationService.deleteNotification(notificationId);

        // Then
        assertNotNull(notification.getDeletedAt());
        verify(notificationRepository).save(notification);
    }

    @Test
    void createNotification_ShouldSaveNewNotification() {
        // Given
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        // When
        NotificationDto result = notificationService.createNotification(user, alert, "PUSH");

        // Then
        assertNotNull(result);
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void markAsSent_ShouldUpdateStatusAndTimestamp() {
        // Given
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        // When
        NotificationDto result = notificationService.markAsSent(notificationId);

        // Then
        assertNotNull(result);
        assertEquals("SENT", notification.getStatus());
        assertNotNull(notification.getSentAt());
        verify(notificationRepository).save(notification);
    }

    @Test
    void markAsFailed_ShouldUpdateStatusToFailed() {
        // Given
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        // When
        NotificationDto result = notificationService.markAsFailed(notificationId);

        // Then
        assertNotNull(result);
        assertEquals("FAILED", notification.getStatus());
        verify(notificationRepository).save(notification);
    }

    @Test
    void countUndeliveredNotifications_ShouldReturnCount() {
        // Given
        when(notificationRepository.countUndeliveredNotifications(userId)).thenReturn(5L);

        // When
        long count = notificationService.countUndeliveredNotifications(userId);

        // Then
        assertEquals(5L, count);
        verify(notificationRepository).countUndeliveredNotifications(userId);
    }

    @Test
    void countUnreadNotifications_ShouldReturnCount() {
        // Given
        when(notificationRepository.countUnreadNotifications(userId)).thenReturn(3L);

        // When
        long count = notificationService.countUnreadNotifications(userId);

        // Then
        assertEquals(3L, count);
        verify(notificationRepository).countUnreadNotifications(userId);
    }

    @Test
    void markAllAsRead_ShouldReturnNumberOfUpdatedNotifications() {
        // Given
        when(notificationRepository.markAllAsReadForUser(userId)).thenReturn(10);

        // When
        int count = notificationService.markAllAsRead(userId);

        // Then
        assertEquals(10, count);
        verify(notificationRepository).markAllAsReadForUser(userId);
    }
}
