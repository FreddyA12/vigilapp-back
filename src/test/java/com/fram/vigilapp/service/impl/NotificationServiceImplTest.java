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
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
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

    private User testUser;
    private Alert testAlert;
    private Notification testNotification;
    private GeometryFactory geometryFactory;

    @BeforeEach
    void setUp() {
        geometryFactory = new GeometryFactory();

        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();

        Point point = geometryFactory.createPoint(new Coordinate(-74.0060, 40.7128));
        point.setSRID(4326);

        testAlert = Alert.builder()
                .id(UUID.randomUUID())
                .createdByUser(testUser)
                .category("EMERGENCY")
                .status("ACTIVE")
                .title("Test Alert")
                .description("Test Description")
                .geometry(point)
                .build();

        testNotification = Notification.builder()
                .id(UUID.randomUUID())
                .alert(testAlert)
                .user(testUser)
                .channel("PUSH")
                .status("QUEUED")
                .createdAt(OffsetDateTime.now())
                .build();
    }

    @Test
    void getUserNotifications_shouldReturnPageOfNotifications() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Notification> notifications = Arrays.asList(testNotification);
        Page<Notification> notificationPage = new PageImpl<>(notifications, pageable, 1);

        when(notificationRepository.findByUserIdAndNotDeleted(testUser.getId(), pageable))
                .thenReturn(notificationPage);

        // When
        Page<NotificationDto> result = notificationService.getUserNotifications(testUser.getId(), pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(testNotification.getId(), result.getContent().get(0).getId());
    }

    @Test
    void getUndeliveredNotifications_shouldReturnQueuedNotifications() {
        // Given
        when(notificationRepository.findByUserIdAndStatusOrderByCreatedAtDesc(testUser.getId(), "QUEUED"))
                .thenReturn(Arrays.asList(testNotification));

        // When
        List<NotificationDto> result = notificationService.getUndeliveredNotifications(testUser.getId());

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("QUEUED", result.get(0).getStatus());
    }

    @Test
    void markAsDelivered_shouldUpdateNotificationStatus() {
        // Given
        when(notificationRepository.findById(testNotification.getId()))
                .thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        // When
        NotificationDto result = notificationService.markAsDelivered(testNotification.getId());

        // Then
        assertNotNull(result);
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void markAsDelivered_withNonExistentId_shouldThrowException() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(notificationRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () ->
                notificationService.markAsDelivered(nonExistentId)
        );
    }

    @Test
    void getNotificationById_shouldReturnNotification() {
        // Given
        when(notificationRepository.findById(testNotification.getId()))
                .thenReturn(Optional.of(testNotification));

        // When
        NotificationDto result = notificationService.getNotificationById(testNotification.getId());

        // Then
        assertNotNull(result);
        assertEquals(testNotification.getId(), result.getId());
        assertEquals(testAlert.getId(), result.getAlertId());
    }

    @Test
    void deleteNotification_shouldSetDeletedAt() {
        // Given
        when(notificationRepository.findById(testNotification.getId()))
                .thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        // When
        notificationService.deleteNotification(testNotification.getId());

        // Then
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void createNotification_shouldCreateNewNotification() {
        // Given
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        // When
        NotificationDto result = notificationService.createNotification(testUser, testAlert, "PUSH");

        // Then
        assertNotNull(result);
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void notifyUsersInZone_shouldCreateNotificationsForUsersInZone() {
        // Given
        User otherUser = User.builder()
                .id(UUID.randomUUID())
                .email("other@example.com")
                .build();

        when(userRepository.findUsersInZone(any(Point.class)))
                .thenReturn(Arrays.asList(otherUser));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        // When
        notificationService.notifyUsersInZone(testAlert, "PUSH");

        // Then
        verify(notificationRepository, atLeastOnce()).save(any(Notification.class));
    }

    @Test
    void notifyUsersInZone_shouldNotNotifyAlertCreator() {
        // Given
        when(userRepository.findUsersInZone(any(Point.class)))
                .thenReturn(Arrays.asList(testUser)); // The creator

        // When
        notificationService.notifyUsersInZone(testAlert, "PUSH");

        // Then
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void markAsSent_shouldUpdateStatus() {
        // Given
        when(notificationRepository.findById(testNotification.getId()))
                .thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        // When
        NotificationDto result = notificationService.markAsSent(testNotification.getId());

        // Then
        assertNotNull(result);
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void markAsFailed_shouldUpdateStatus() {
        // Given
        when(notificationRepository.findById(testNotification.getId()))
                .thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        // When
        NotificationDto result = notificationService.markAsFailed(testNotification.getId());

        // Then
        assertNotNull(result);
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void markAsRead_shouldSetReadAt() {
        // Given
        when(notificationRepository.findById(testNotification.getId()))
                .thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        // When
        NotificationDto result = notificationService.markAsRead(testNotification.getId());

        // Then
        assertNotNull(result);
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void markAllAsRead_shouldUpdateAllUserNotifications() {
        // Given
        when(notificationRepository.markAllAsReadForUser(testUser.getId())).thenReturn(5);

        // When
        int result = notificationService.markAllAsRead(testUser.getId());

        // Then
        assertEquals(5, result);
        verify(notificationRepository).markAllAsReadForUser(testUser.getId());
    }

    @Test
    void countUndeliveredNotifications_shouldReturnCount() {
        // Given
        when(notificationRepository.countUndeliveredNotifications(testUser.getId())).thenReturn(3L);

        // When
        long result = notificationService.countUndeliveredNotifications(testUser.getId());

        // Then
        assertEquals(3L, result);
    }

    @Test
    void countUnreadNotifications_shouldReturnCount() {
        // Given
        when(notificationRepository.countUnreadNotifications(testUser.getId())).thenReturn(7L);

        // When
        long result = notificationService.countUnreadNotifications(testUser.getId());

        // Then
        assertEquals(7L, result);
    }

    @Test
    void getQueuedNotifications_shouldReturnQueuedNotifications() {
        // Given
        when(notificationRepository.findQueuedNotifications())
                .thenReturn(Arrays.asList(testNotification));

        // When
        List<NotificationDto> result = notificationService.getQueuedNotifications();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getQueuedNotificationsByChannel_shouldReturnFilteredNotifications() {
        // Given
        String channel = "PUSH";
        when(notificationRepository.findQueuedNotificationsByChannel(channel))
                .thenReturn(Arrays.asList(testNotification));

        // When
        List<NotificationDto> result = notificationService.getQueuedNotificationsByChannel(channel);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(notificationRepository).findQueuedNotificationsByChannel(channel);
    }

    @Test
    void getNotificationsByAlert_shouldReturnNotificationsForAlert() {
        // Given
        when(notificationRepository.findByAlertIdOrderByCreatedAtDesc(testAlert.getId()))
                .thenReturn(Arrays.asList(testNotification));

        // When
        List<NotificationDto> result = notificationService.getNotificationsByAlert(testAlert.getId());

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testAlert.getId(), result.get(0).getAlertId());
    }

    @Test
    void deleteOldNotifications_shouldDeleteNotificationsOlderThanDays() {
        // Given
        OffsetDateTime oldDate = OffsetDateTime.now().minusDays(100);
        Notification oldNotification = Notification.builder()
                .id(UUID.randomUUID())
                .alert(testAlert)
                .user(testUser)
                .createdAt(oldDate)
                .build();

        when(notificationRepository.findAll()).thenReturn(Arrays.asList(oldNotification));
        doNothing().when(notificationRepository).deleteAll(anyList());

        // When
        notificationService.deleteOldNotifications(30);

        // Then
        verify(notificationRepository).deleteAll(anyList());
    }
}
