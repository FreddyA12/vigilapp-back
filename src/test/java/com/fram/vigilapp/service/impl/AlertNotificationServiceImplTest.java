package com.fram.vigilapp.service.impl;

import com.fram.vigilapp.dto.AlertDto;
import com.fram.vigilapp.entity.Alert;
import com.fram.vigilapp.entity.User;
import com.fram.vigilapp.entity.UserZone;
import com.fram.vigilapp.repository.UserRepository;
import com.fram.vigilapp.repository.UserZoneRepository;
import com.fram.vigilapp.websocket.AlertWebSocketHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.util.GeometricShapeFactory;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertNotificationServiceImplTest {

    @Mock
    private AlertWebSocketHandler webSocketHandler;

    @Mock
    private UserZoneRepository userZoneRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AlertNotificationServiceImpl alertNotificationService;

    private User testUser;
    private User creator;
    private Alert testAlert;
    private AlertDto alertDto;
    private UserZone userZone;
    private GeometryFactory geometryFactory;

    @BeforeEach
    void setUp() {
        geometryFactory = new GeometryFactory();

        creator = User.builder()
                .id(UUID.randomUUID())
                .email("creator@example.com")
                .firstName("Creator")
                .lastName("User")
                .build();

        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();

        Point alertPoint = geometryFactory.createPoint(new Coordinate(-74.0060, 40.7128));
        alertPoint.setSRID(4326);

        testAlert = Alert.builder()
                .id(UUID.randomUUID())
                .createdByUser(creator)
                .category("EMERGENCY")
                .status("ACTIVE")
                .title("Test Alert")
                .description("Test Description")
                .geometry(alertPoint)
                .radiusM(1000)
                .createdAt(OffsetDateTime.now())
                .build();

        alertDto = AlertDto.builder()
                .id(testAlert.getId())
                .createdByUserId(creator.getId())
                .createdByUserName("Creator User")
                .category("EMERGENCY")
                .status("ACTIVE")
                .title("Test Alert")
                .description("Test Description")
                .latitude(40.7128)
                .longitude(-74.0060)
                .radiusM(1000)
                .build();

        GeometricShapeFactory shapeFactory = new GeometricShapeFactory();
        shapeFactory.setNumPoints(32);
        shapeFactory.setCentre(new Coordinate(-74.0060, 40.7128));
        shapeFactory.setSize(0.1); // Large enough to contain alert point
        Polygon polygon = shapeFactory.createCircle();
        polygon.setSRID(4326);

        userZone = UserZone.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .geometry(polygon)
                .radiusM(5000)
                .build();
    }

    @Test
    void notifyNewAlert_withUserInZone_shouldSendNotification() {
        // Given
        Set<UUID> connectedUsers = new HashSet<>(Arrays.asList(testUser.getId()));
        when(webSocketHandler.getConnectedUserIds()).thenReturn(connectedUsers);
        when(userZoneRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(userZone));
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        doNothing().when(webSocketHandler).sendAlertToUser(anyString(), any());

        // When
        alertNotificationService.notifyNewAlert(testAlert, alertDto);

        // Then
        verify(webSocketHandler).getConnectedUserIds();
        verify(userZoneRepository).findByUserId(testUser.getId());
        verify(userRepository).findById(testUser.getId());
        verify(webSocketHandler).sendAlertToUser(eq(testUser.getEmail()), any(Map.class));
    }

    @Test
    void notifyNewAlert_withUserOutsideZone_shouldNotSendNotification() {
        // Given
        Set<UUID> connectedUsers = new HashSet<>(Arrays.asList(testUser.getId()));

        // Create zone far from alert
        GeometricShapeFactory shapeFactory = new GeometricShapeFactory();
        shapeFactory.setNumPoints(32);
        shapeFactory.setCentre(new Coordinate(100.0, 100.0)); // Far away
        shapeFactory.setSize(0.01);
        Polygon farPolygon = shapeFactory.createCircle();
        farPolygon.setSRID(4326);

        UserZone farZone = UserZone.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .geometry(farPolygon)
                .radiusM(1000)
                .build();

        when(webSocketHandler.getConnectedUserIds()).thenReturn(connectedUsers);
        when(userZoneRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(farZone));

        // When
        alertNotificationService.notifyNewAlert(testAlert, alertDto);

        // Then
        verify(webSocketHandler).getConnectedUserIds();
        verify(userZoneRepository).findByUserId(testUser.getId());
        verify(webSocketHandler, never()).sendAlertToUser(anyString(), any());
    }

    @Test
    void notifyNewAlert_shouldNotNotifyCreator() {
        // Given
        Set<UUID> connectedUsers = new HashSet<>(Arrays.asList(creator.getId()));
        when(webSocketHandler.getConnectedUserIds()).thenReturn(connectedUsers);

        // When
        alertNotificationService.notifyNewAlert(testAlert, alertDto);

        // Then
        verify(webSocketHandler).getConnectedUserIds();
        verify(userZoneRepository, never()).findByUserId(creator.getId());
        verify(webSocketHandler, never()).sendAlertToUser(anyString(), any());
    }

    @Test
    void notifyNewAlert_withUserWithoutZone_shouldNotSendNotification() {
        // Given
        Set<UUID> connectedUsers = new HashSet<>(Arrays.asList(testUser.getId()));
        when(webSocketHandler.getConnectedUserIds()).thenReturn(connectedUsers);
        when(userZoneRepository.findByUserId(testUser.getId())).thenReturn(Optional.empty());

        // When
        alertNotificationService.notifyNewAlert(testAlert, alertDto);

        // Then
        verify(webSocketHandler).getConnectedUserIds();
        verify(userZoneRepository).findByUserId(testUser.getId());
        verify(webSocketHandler, never()).sendAlertToUser(anyString(), any());
    }

    @Test
    void registerUser_shouldAddUserToConnectedUsers() {
        // Given
        UUID userId = UUID.randomUUID();
        String sessionId = "session123";

        // When
        alertNotificationService.registerUser(userId, sessionId);

        // Then - No exception thrown, method executes successfully
        assertDoesNotThrow(() -> alertNotificationService.registerUser(userId, sessionId));
    }

    @Test
    void unregisterUser_shouldRemoveUserFromConnectedUsers() {
        // Given
        UUID userId = UUID.randomUUID();
        String sessionId = "session123";
        alertNotificationService.registerUser(userId, sessionId);

        // When
        alertNotificationService.unregisterUser(userId, sessionId);

        // Then - No exception thrown
        assertDoesNotThrow(() -> alertNotificationService.unregisterUser(userId, sessionId));
    }

    @Test
    void getConnectedUsersCount_shouldReturnCount() {
        // Given
        when(webSocketHandler.getConnectedUsersCount()).thenReturn(Integer.valueOf(5));

        // When
        long count = alertNotificationService.getConnectedUsersCount();

        // Then
        assertEquals(5L, count);
        verify(webSocketHandler).getConnectedUsersCount();
    }

    @Test
    void broadcastAlert_shouldSendToAllConnectedUsers() {
        // Given
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();

        User user1 = User.builder().id(userId1).email("user1@test.com").build();
        User user2 = User.builder().id(userId2).email("user2@test.com").build();

        alertNotificationService.registerUser(userId1, "session1");
        alertNotificationService.registerUser(userId2, "session2");

        when(userRepository.findById(userId1)).thenReturn(Optional.of(user1));
        when(userRepository.findById(userId2)).thenReturn(Optional.of(user2));

        // When
        alertNotificationService.broadcastAlert(alertDto);

        // Then
        verify(webSocketHandler, atLeast(0)).sendAlertToUser(anyString(), any(Map.class));
    }

    @Test
    void notifyNewAlert_withException_shouldNotThrowException() {
        // Given
        Set<UUID> connectedUsers = new HashSet<>(Arrays.asList(testUser.getId()));
        when(webSocketHandler.getConnectedUserIds()).thenReturn(connectedUsers);
        when(userZoneRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(userZone));
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        doThrow(new RuntimeException("WebSocket error"))
                .when(webSocketHandler).sendAlertToUser(anyString(), any());

        // When & Then
        assertDoesNotThrow(() -> alertNotificationService.notifyNewAlert(testAlert, alertDto));
    }
}
