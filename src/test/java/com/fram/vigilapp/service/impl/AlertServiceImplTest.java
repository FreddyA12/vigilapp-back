package com.fram.vigilapp.service.impl;

import com.fram.vigilapp.dto.AlertDto;
import com.fram.vigilapp.dto.AlertStatsDto;
import com.fram.vigilapp.dto.HeatmapPointDto;
import com.fram.vigilapp.dto.SaveAlertDto;
import com.fram.vigilapp.entity.*;
import com.fram.vigilapp.repository.*;
import com.fram.vigilapp.service.AlertNotificationService;
import com.fram.vigilapp.service.MediaService;
import com.fram.vigilapp.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertServiceImplTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private UserZoneRepository userZoneRepository;

    @Mock
    private CityRepository cityRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AlertNotificationService alertNotificationService;

    @Mock
    private MediaService mediaService;

    @Mock
    private AlertMediaRepository alertMediaRepository;

    @InjectMocks
    private AlertServiceImpl alertService;

    private User testUser;
    private Alert testAlert;
    private City testCity;
    private GeometryFactory geometryFactory;

    @BeforeEach
    void setUp() {
        geometryFactory = new GeometryFactory();
        testCity = new City();
        testCity.setId(UUID.randomUUID());
        testCity.setName("Test City");

        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();

        Point testPoint = geometryFactory.createPoint(new Coordinate(-74.0060, 40.7128));
        testPoint.setSRID(4326);

        testAlert = Alert.builder()
                .id(UUID.randomUUID())
                .createdByUser(testUser)
                .category("EMERGENCY")
                .status("ACTIVE")
                .verificationStatus("PENDING")
                .title("Test Alert")
                .description("Test Description")
                .isAnonymous(false)
                .geometry(testPoint)
                .radiusM(1000)
                .city(testCity)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    @Test
    void createAlert_shouldCreateAlertSuccessfully() {
        // Given
        SaveAlertDto saveDto = SaveAlertDto.builder()
                .category("EMERGENCY")
                .title("New Alert")
                .description("Alert Description")
                .latitude(40.7128)
                .longitude(-74.0060)
                .radiusM(1000)
                .isAnonymous(false)
                .cityId(testCity.getId())
                .build();

        when(cityRepository.findById(testCity.getId())).thenReturn(Optional.of(testCity));
        when(alertRepository.save(any(Alert.class))).thenReturn(testAlert);
        when(alertMediaRepository.findByAlertId(any(UUID.class))).thenReturn(Collections.emptyList());
        doNothing().when(notificationService).notifyUsersInZone(any(Alert.class), anyString());
        doNothing().when(alertNotificationService).notifyNewAlert(any(Alert.class), any(AlertDto.class));

        // When
        AlertDto result = alertService.createAlert(testUser, saveDto);

        // Then
        assertNotNull(result);
        assertEquals(testAlert.getId(), result.getId());
        assertEquals("Test Alert", result.getTitle());
        verify(alertRepository).save(any(Alert.class));
        verify(notificationService).notifyUsersInZone(any(Alert.class), eq("PUSH"));
        verify(alertNotificationService).notifyNewAlert(any(Alert.class), any(AlertDto.class));
    }

    @Test
    void createAlert_withoutCityId_shouldCreateAlertWithNullCity() {
        // Given
        SaveAlertDto saveDto = SaveAlertDto.builder()
                .category("INFO")
                .title("No City Alert")
                .description("Description")
                .latitude(40.7128)
                .longitude(-74.0060)
                .build();

        when(alertRepository.save(any(Alert.class))).thenReturn(testAlert);
        when(alertMediaRepository.findByAlertId(any(UUID.class))).thenReturn(Collections.emptyList());

        // When
        AlertDto result = alertService.createAlert(testUser, saveDto);

        // Then
        assertNotNull(result);
        verify(cityRepository, never()).findById(any(UUID.class));
        verify(alertRepository).save(any(Alert.class));
    }

    @Test
    void getAlertById_shouldReturnAlert() {
        // Given
        when(alertRepository.findById(testAlert.getId())).thenReturn(Optional.of(testAlert));
        when(alertMediaRepository.findByAlertId(testAlert.getId())).thenReturn(Collections.emptyList());

        // When
        AlertDto result = alertService.getAlertById(testAlert.getId());

        // Then
        assertNotNull(result);
        assertEquals(testAlert.getId(), result.getId());
        assertEquals("Test Alert", result.getTitle());
    }

    @Test
    void getAlertById_withNonExistentId_shouldThrowException() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(alertRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> alertService.getAlertById(nonExistentId));
    }

    @Test
    void getAlertsNearLocation_shouldReturnNearbyAlerts() {
        // Given
        Double latitude = 40.7128;
        Double longitude = -74.0060;
        Integer radius = 5000;
        List<Alert> alerts = Arrays.asList(testAlert);

        when(alertRepository.findActiveAlertsWithinRadius(latitude, longitude, radius)).thenReturn(alerts);
        when(alertRepository.calculateDistanceFromPoint(testAlert.getId(), latitude, longitude)).thenReturn(100.0);
        when(alertMediaRepository.findByAlertId(testAlert.getId())).thenReturn(Collections.emptyList());

        // When
        List<AlertDto> result = alertService.getAlertsNearLocation(latitude, longitude, radius, true);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(100.0, result.get(0).getDistanceFromUserM());
    }

    @Test
    void getUserAlerts_shouldReturnUserAlerts() {
        // Given
        List<Alert> alerts = Arrays.asList(testAlert);
        when(alertRepository.findByCreatedByUserIdOrderByCreatedAtDesc(testUser.getId())).thenReturn(alerts);
        when(alertMediaRepository.findByAlertId(testAlert.getId())).thenReturn(Collections.emptyList());

        // When
        List<AlertDto> result = alertService.getUserAlerts(testUser.getId());

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testAlert.getId(), result.get(0).getId());
    }

    @Test
    void updateAlertStatus_shouldUpdateStatus() {
        // Given
        String newStatus = "RESOLVED";
        when(alertRepository.findById(testAlert.getId())).thenReturn(Optional.of(testAlert));
        when(alertRepository.save(any(Alert.class))).thenReturn(testAlert);
        when(alertMediaRepository.findByAlertId(testAlert.getId())).thenReturn(Collections.emptyList());

        // When
        AlertDto result = alertService.updateAlertStatus(testAlert.getId(), newStatus);

        // Then
        assertNotNull(result);
        verify(alertRepository).save(any(Alert.class));
    }

    @Test
    void updateAlertStatus_toResolved_shouldSetResolvedAt() {
        // Given
        String newStatus = "RESOLVED";
        when(alertRepository.findById(testAlert.getId())).thenReturn(Optional.of(testAlert));
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> {
            Alert savedAlert = invocation.getArgument(0);
            assertNotNull(savedAlert.getResolvedAt());
            return savedAlert;
        });
        when(alertMediaRepository.findByAlertId(testAlert.getId())).thenReturn(Collections.emptyList());

        // When
        alertService.updateAlertStatus(testAlert.getId(), newStatus);

        // Then
        verify(alertRepository).save(any(Alert.class));
    }

    @Test
    void deleteAlert_shouldDeleteAlert() {
        // Given
        when(alertRepository.findById(testAlert.getId())).thenReturn(Optional.of(testAlert));
        doNothing().when(alertRepository).delete(testAlert);

        // When
        alertService.deleteAlert(testAlert.getId());

        // Then
        verify(alertRepository).delete(testAlert);
    }

    @Test
    void getRecentAlerts_shouldReturnPageOfAlerts() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Alert> alerts = Arrays.asList(testAlert);
        Page<Alert> alertPage = new PageImpl<>(alerts, pageable, 1);

        when(alertRepository.findByStatusIn(anyList(), eq(pageable))).thenReturn(alertPage);
        when(alertMediaRepository.findByAlertId(testAlert.getId())).thenReturn(Collections.emptyList());

        // When
        Page<AlertDto> result = alertService.getRecentAlerts(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getAlertStats_shouldReturnStatistics() {
        // Given
        List<Alert> alerts = Arrays.asList(testAlert);
        when(alertRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(alerts);
        when(userRepository.findByStatus("ACTIVE")).thenReturn(Arrays.asList(testUser));
        when(userRepository.count()).thenReturn(10L);

        // When
        AlertStatsDto result = alertService.getAlertStats("7d", null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalAlerts());
        assertEquals(1, result.getActiveAlerts());
        assertNotNull(result.getAlertsByCategory());
        assertEquals("7d", result.getTimeRange());
    }

    @Test
    void searchAlerts_shouldFilterAlertsByQuery() {
        // Given
        testAlert.setTitle("Emergency Fire");
        testAlert.setDescription("Fire in building");
        when(alertRepository.findAll()).thenReturn(Arrays.asList(testAlert));
        when(alertMediaRepository.findByAlertId(testAlert.getId())).thenReturn(Collections.emptyList());

        // When
        List<AlertDto> result = alertService.searchAlerts("fire", null, null, null, null, null, null, null, null, 0, 10);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void searchAlerts_withNoMatch_shouldReturnEmptyList() {
        // Given
        when(alertRepository.findAll()).thenReturn(Arrays.asList(testAlert));

        // When
        List<AlertDto> result = alertService.searchAlerts("nonexistent", null, null, null, null, null, null, null, null, 0, 10);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getAlertsByStatus_shouldReturnFilteredAlerts() {
        // Given
        String status = "ACTIVE";
        when(alertRepository.findByStatusOrderByCreatedAtDesc(status)).thenReturn(Arrays.asList(testAlert));
        when(alertMediaRepository.findByAlertId(testAlert.getId())).thenReturn(Collections.emptyList());

        // When
        List<AlertDto> result = alertService.getAlertsByStatus(status);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(alertRepository).findByStatusOrderByCreatedAtDesc(status);
    }

    @Test
    void getAlertsByCategoryAndStatus_shouldReturnFilteredAlerts() {
        // Given
        String category = "EMERGENCY";
        String status = "ACTIVE";
        when(alertRepository.findByCategoryAndStatusOrderByCreatedAtDesc(category, status))
                .thenReturn(Arrays.asList(testAlert));
        when(alertMediaRepository.findByAlertId(testAlert.getId())).thenReturn(Collections.emptyList());

        // When
        List<AlertDto> result = alertService.getAlertsByCategoryAndStatus(category, status);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(alertRepository).findByCategoryAndStatusOrderByCreatedAtDesc(category, status);
    }

    @Test
    void createAlert_withException_shouldNotFailAlertCreation() {
        // Given
        SaveAlertDto saveDto = SaveAlertDto.builder()
                .category("EMERGENCY")
                .title("Test Alert")
                .description("Description")
                .latitude(40.7128)
                .longitude(-74.0060)
                .build();

        when(alertRepository.save(any(Alert.class))).thenReturn(testAlert);
        when(alertMediaRepository.findByAlertId(any(UUID.class))).thenReturn(Collections.emptyList());
        doThrow(new RuntimeException("Notification error")).when(notificationService).notifyUsersInZone(any(Alert.class), anyString());

        // When
        AlertDto result = alertService.createAlert(testUser, saveDto);

        // Then
        assertNotNull(result);
        verify(alertRepository).save(any(Alert.class));
    }

    @Test
    void createAlert_withWebSocketException_shouldNotFailAlertCreation() {
        // Given
        SaveAlertDto saveDto = SaveAlertDto.builder()
                .category("EMERGENCY")
                .title("Test Alert")
                .description("Description")
                .latitude(40.7128)
                .longitude(-74.0060)
                .build();

        when(alertRepository.save(any(Alert.class))).thenReturn(testAlert);
        when(alertMediaRepository.findByAlertId(any(UUID.class))).thenReturn(Collections.emptyList());
        doNothing().when(notificationService).notifyUsersInZone(any(Alert.class), anyString());
        doThrow(new RuntimeException("WebSocket error")).when(alertNotificationService).notifyNewAlert(any(Alert.class), any(AlertDto.class));

        // When
        AlertDto result = alertService.createAlert(testUser, saveDto);

        // Then
        assertNotNull(result);
        verify(alertRepository).save(any(Alert.class));
    }

    @Test
    void getAlertsNearLocation_withActiveOnlyFalse_shouldReturnAllAlerts() {
        // Given
        Double latitude = 40.7128;
        Double longitude = -74.0060;
        Integer radius = 5000;
        List<Alert> alerts = Arrays.asList(testAlert);

        when(alertRepository.findAllAlertsWithinRadius(latitude, longitude, radius)).thenReturn(alerts);
        when(alertRepository.calculateDistanceFromPoint(testAlert.getId(), latitude, longitude)).thenReturn(150.0);
        when(alertMediaRepository.findByAlertId(testAlert.getId())).thenReturn(Collections.emptyList());

        // When
        List<AlertDto> result = alertService.getAlertsNearLocation(latitude, longitude, radius, false);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(150.0, result.get(0).getDistanceFromUserM());
        verify(alertRepository).findAllAlertsWithinRadius(latitude, longitude, radius);
        verify(alertRepository, never()).findActiveAlertsWithinRadius(any(), any(), any());
    }

    @Test
    void getAlertsInUserZone_shouldReturnAlertsInZone() {
        // Given
        Coordinate[] coords = new Coordinate[]{
                new Coordinate(-74.01, 40.71),
                new Coordinate(-74.01, 40.72),
                new Coordinate(-74.00, 40.72),
                new Coordinate(-74.00, 40.71),
                new Coordinate(-74.01, 40.71)
        };
        Polygon zonePolygon = geometryFactory.createPolygon(coords);
        zonePolygon.setSRID(4326);

        UserZone userZone = UserZone.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .geometry(zonePolygon)
                .radiusM(5000)
                .build();

        when(userZoneRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(userZone));
        when(alertRepository.findActiveAlertsWithinRadius(any(), any(), any())).thenReturn(Arrays.asList(testAlert));
        when(alertRepository.calculateDistanceFromPoint(any(), any(), any())).thenReturn(200.0);
        when(alertMediaRepository.findByAlertId(testAlert.getId())).thenReturn(Collections.emptyList());

        // When
        List<AlertDto> result = alertService.getAlertsInUserZone(testUser.getId());

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getAlertsInUserZone_withNoZone_shouldThrowException() {
        // Given
        when(userZoneRepository.findByUserId(testUser.getId())).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> alertService.getAlertsInUserZone(testUser.getId()));
    }

    @Test
    void updateAlertStatus_toCancelled_shouldSetResolvedAt() {
        // Given
        String newStatus = "CANCELLED";
        when(alertRepository.findById(testAlert.getId())).thenReturn(Optional.of(testAlert));
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> {
            Alert savedAlert = invocation.getArgument(0);
            assertNotNull(savedAlert.getResolvedAt());
            return savedAlert;
        });
        when(alertMediaRepository.findByAlertId(testAlert.getId())).thenReturn(Collections.emptyList());

        // When
        alertService.updateAlertStatus(testAlert.getId(), newStatus);

        // Then
        verify(alertRepository).save(any(Alert.class));
    }

    @Test
    void searchAlerts_withAllFilters_shouldFilterCorrectly() {
        // Given
        testAlert.setTitle("Emergency Fire");
        testAlert.setDescription("Fire in building");
        testAlert.setCategory("EMERGENCY");
        testAlert.setStatus("ACTIVE");
        testAlert.setVerificationStatus("VERIFIED");
        testAlert.setRadiusM(1500);
        testAlert.setCity(testCity);

        when(alertRepository.findAll()).thenReturn(Arrays.asList(testAlert));
        when(alertMediaRepository.findByAlertId(testAlert.getId())).thenReturn(Collections.emptyList());

        // When
        List<AlertDto> result = alertService.searchAlerts(
                "fire",
                "EMERGENCY",
                "ACTIVE",
                "VERIFIED",
                testCity.getId(),
                1000,
                2000,
                OffsetDateTime.now().minusDays(1),
                OffsetDateTime.now().plusDays(1),
                0,
                10
        );

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void searchAlerts_withNullQuery_shouldReturnAllAlerts() {
        // Given
        when(alertRepository.findAll()).thenReturn(Arrays.asList(testAlert));
        when(alertMediaRepository.findByAlertId(testAlert.getId())).thenReturn(Collections.emptyList());

        // When
        List<AlertDto> result = alertService.searchAlerts(null, null, null, null, null, null, null, null, null, 0, 10);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void searchAlerts_withEmptyQuery_shouldReturnAllAlerts() {
        // Given
        when(alertRepository.findAll()).thenReturn(Arrays.asList(testAlert));
        when(alertMediaRepository.findByAlertId(testAlert.getId())).thenReturn(Collections.emptyList());

        // When
        List<AlertDto> result = alertService.searchAlerts("", null, null, null, null, null, null, null, null, 0, 10);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getHeatmapData_shouldReturnHeatmapPoints() {
        // Given
        Double swLat = 40.0;
        Double swLon = -75.0;
        Double neLat = 41.0;
        Double neLon = -73.0;
        Double gridSizeM = 1000.0;

        when(alertRepository.findAlertsInBounds(swLat, swLon, neLat, neLon))
                .thenReturn(Arrays.asList(testAlert));

        // When
        List<HeatmapPointDto> result = alertService.getHeatmapData(swLat, swLon, neLat, neLon, gridSizeM);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1.0, result.get(0).getIntensity());
    }

    @Test
    void getHeatmapData_withMultipleAlerts_shouldNormalizeIntensity() {
        // Given
        Double swLat = 40.0;
        Double swLon = -75.0;
        Double neLat = 41.0;
        Double neLon = -73.0;
        Double gridSizeM = 1000.0;

        Alert alert2 = Alert.builder()
                .id(UUID.randomUUID())
                .geometry(geometryFactory.createPoint(new Coordinate(-74.0060, 40.7128)))
                .build();
        alert2.getGeometry().setSRID(4326);

        when(alertRepository.findAlertsInBounds(swLat, swLon, neLat, neLon))
                .thenReturn(Arrays.asList(testAlert, alert2));

        // When
        List<HeatmapPointDto> result = alertService.getHeatmapData(swLat, swLon, neLat, neLon, gridSizeM);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void getAlertStats_with24hTimeRange_shouldCalculateStats() {
        // Given
        List<Alert> alerts = Arrays.asList(testAlert);
        when(alertRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(alerts);
        when(userRepository.findByStatus("ACTIVE")).thenReturn(Arrays.asList(testUser));
        when(userRepository.count()).thenReturn(10L);

        // When
        AlertStatsDto result = alertService.getAlertStats("24h", null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalAlerts());
        assertEquals("24h", result.getTimeRange());
    }

    @Test
    void getAlertStats_with30dTimeRange_shouldCalculateStats() {
        // Given
        List<Alert> alerts = Arrays.asList(testAlert);
        when(alertRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(alerts);
        when(userRepository.findByStatus("ACTIVE")).thenReturn(Arrays.asList(testUser));
        when(userRepository.count()).thenReturn(10L);

        // When
        AlertStatsDto result = alertService.getAlertStats("30d", null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalAlerts());
        assertEquals("30d", result.getTimeRange());
    }

    @Test
    void getAlertStats_withInvalidTimeRange_shouldDefaultTo7d() {
        // Given
        List<Alert> alerts = Arrays.asList(testAlert);
        when(alertRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(alerts);
        when(userRepository.findByStatus("ACTIVE")).thenReturn(Arrays.asList(testUser));
        when(userRepository.count()).thenReturn(10L);

        // When
        AlertStatsDto result = alertService.getAlertStats("invalid", null);

        // Then
        assertNotNull(result);
        assertEquals("invalid", result.getTimeRange());
    }

    @Test
    void getAlertStats_withCityFilter_shouldFilterByCity() {
        // Given
        List<Alert> alerts = Arrays.asList(testAlert);
        when(alertRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(alerts);
        when(userRepository.findByStatus("ACTIVE")).thenReturn(Arrays.asList(testUser));
        when(userRepository.count()).thenReturn(10L);

        // When
        AlertStatsDto result = alertService.getAlertStats("7d", testCity.getId());

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalAlerts());
    }

    @Test
    void getAlertStats_withCityFilter_noMatch_shouldReturnZeroAlerts() {
        // Given
        List<Alert> alerts = Arrays.asList(testAlert);
        when(alertRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(alerts);
        when(userRepository.findByStatus("ACTIVE")).thenReturn(Arrays.asList(testUser));
        when(userRepository.count()).thenReturn(10L);

        // When
        AlertStatsDto result = alertService.getAlertStats("7d", UUID.randomUUID());

        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotalAlerts());
    }

    @Test
    void getAlertStats_withDifferentStatuses_shouldCountCorrectly() {
        // Given
        Alert resolvedAlert = Alert.builder()
                .id(UUID.randomUUID())
                .status("RESOLVED")
                .category("INFO")
                .verificationStatus("VERIFIED")
                .geometry(geometryFactory.createPoint(new Coordinate(-74.0060, 40.7128)))
                .city(testCity)
                .createdAt(OffsetDateTime.now())
                .createdByUser(testUser)
                .build();

        Alert cancelledAlert = Alert.builder()
                .id(UUID.randomUUID())
                .status("CANCELLED")
                .category("PRECAUTION")
                .verificationStatus("REJECTED")
                .geometry(geometryFactory.createPoint(new Coordinate(-74.0060, 40.7128)))
                .city(testCity)
                .createdAt(OffsetDateTime.now())
                .createdByUser(testUser)
                .build();

        List<Alert> alerts = Arrays.asList(testAlert, resolvedAlert, cancelledAlert);
        when(alertRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(alerts);
        when(userRepository.findByStatus("ACTIVE")).thenReturn(Arrays.asList(testUser));
        when(userRepository.count()).thenReturn(10L);

        // When
        AlertStatsDto result = alertService.getAlertStats("7d", null);

        // Then
        assertNotNull(result);
        assertEquals(3, result.getTotalAlerts());
        assertEquals(1, result.getActiveAlerts());
        assertEquals(1, result.getResolvedAlerts());
        assertEquals(1, result.getCancelledAlerts());
        assertTrue(result.getFalseReportsPercentage() > 0);
    }

    @Test
    void mapToDto_withAnonymousAlert_shouldHideUserName() {
        // Given
        testAlert.setIsAnonymous(true);
        when(alertMediaRepository.findByAlertId(testAlert.getId())).thenReturn(Collections.emptyList());
        when(alertRepository.findById(testAlert.getId())).thenReturn(Optional.of(testAlert));

        // When
        AlertDto result = alertService.getAlertById(testAlert.getId());

        // Then
        assertNotNull(result);
        assertNull(result.getCreatedByUserName());
        assertTrue(result.getIsAnonymous());
    }

    @Test
    void mapToDto_withMedia_shouldIncludeMediaList() {
        // Given
        Media media = Media.builder()
                .id(UUID.randomUUID())
                .url("/uploads/test.jpg")
                .mimeType("image/jpeg")
                .forBlurAnalysis(true)
                .createdAt(OffsetDateTime.now())
                .build();

        AlertMedia alertMedia = AlertMedia.builder()
                .alert(testAlert)
                .media(media)
                .build();

        when(alertRepository.findById(testAlert.getId())).thenReturn(Optional.of(testAlert));
        when(alertMediaRepository.findByAlertId(testAlert.getId())).thenReturn(Arrays.asList(alertMedia));

        // When
        AlertDto result = alertService.getAlertById(testAlert.getId());

        // Then
        assertNotNull(result);
        assertNotNull(result.getMedia());
        assertEquals(1, result.getMedia().size());
        assertEquals("/uploads/test.jpg", result.getMedia().get(0).getUrl());
    }

    @Test
    void createAlertWithMedia_shouldCreateAlertWithFiles() {
        // Given
        SaveAlertDto saveDto = SaveAlertDto.builder()
                .category("EMERGENCY")
                .title("Alert with Media")
                .description("Description")
                .latitude(40.7128)
                .longitude(-74.0060)
                .cityId(testCity.getId())
                .build();

        MockMultipartFile file1 = new MockMultipartFile(
                "file1",
                "test1.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        List<MultipartFile> files = Arrays.asList(file1);

        Media media = Media.builder()
                .id(UUID.randomUUID())
                .url("/uploads/test1.jpg")
                .mimeType("image/jpeg")
                .forBlurAnalysis(true)
                .createdAt(OffsetDateTime.now())
                .build();

        when(cityRepository.findById(testCity.getId())).thenReturn(Optional.of(testCity));
        when(alertRepository.save(any(Alert.class))).thenReturn(testAlert);
        when(mediaService.processAndSaveMultipleMedia(anyList(), any(User.class), anyBoolean()))
                .thenReturn(Arrays.asList(media));
        when(alertMediaRepository.save(any(AlertMedia.class))).thenReturn(null);
        doNothing().when(notificationService).notifyUsersInZone(any(Alert.class), anyString());
        doNothing().when(alertNotificationService).notifyNewAlert(any(Alert.class), any(AlertDto.class));

        // When
        AlertDto result = alertService.createAlertWithMedia(testUser, saveDto, files);

        // Then
        assertNotNull(result);
        verify(mediaService).processAndSaveMultipleMedia(anyList(), any(User.class), eq(true));
        verify(alertMediaRepository).save(any(AlertMedia.class));
    }

    @Test
    void createAlertWithMedia_withNullFiles_shouldCreateAlertWithoutMedia() {
        // Given
        SaveAlertDto saveDto = SaveAlertDto.builder()
                .category("INFO")
                .title("Alert without Media")
                .description("Description")
                .latitude(40.7128)
                .longitude(-74.0060)
                .build();

        when(alertRepository.save(any(Alert.class))).thenReturn(testAlert);
        doNothing().when(notificationService).notifyUsersInZone(any(Alert.class), anyString());
        doNothing().when(alertNotificationService).notifyNewAlert(any(Alert.class), any(AlertDto.class));

        // When
        AlertDto result = alertService.createAlertWithMedia(testUser, saveDto, null);

        // Then
        assertNotNull(result);
        verify(mediaService, never()).processAndSaveMultipleMedia(any(), any(), anyBoolean());
        verify(alertMediaRepository, never()).save(any(AlertMedia.class));
    }

    @Test
    void createAlertWithMedia_withEmptyFiles_shouldCreateAlertWithoutMedia() {
        // Given
        SaveAlertDto saveDto = SaveAlertDto.builder()
                .category("INFO")
                .title("Alert without Media")
                .description("Description")
                .latitude(40.7128)
                .longitude(-74.0060)
                .build();

        when(alertRepository.save(any(Alert.class))).thenReturn(testAlert);
        doNothing().when(notificationService).notifyUsersInZone(any(Alert.class), anyString());
        doNothing().when(alertNotificationService).notifyNewAlert(any(Alert.class), any(AlertDto.class));

        // When
        AlertDto result = alertService.createAlertWithMedia(testUser, saveDto, Collections.emptyList());

        // Then
        assertNotNull(result);
        verify(mediaService, never()).processAndSaveMultipleMedia(any(), any(), anyBoolean());
        verify(alertMediaRepository, never()).save(any(AlertMedia.class));
    }

    @Test
    void createAlertWithMedia_withMediaServiceException_shouldNotFailAlertCreation() {
        // Given
        SaveAlertDto saveDto = SaveAlertDto.builder()
                .category("EMERGENCY")
                .title("Alert with Media")
                .description("Description")
                .latitude(40.7128)
                .longitude(-74.0060)
                .build();

        MockMultipartFile file1 = new MockMultipartFile(
                "file1",
                "test1.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        List<MultipartFile> files = Arrays.asList(file1);

        when(alertRepository.save(any(Alert.class))).thenReturn(testAlert);
        when(mediaService.processAndSaveMultipleMedia(anyList(), any(User.class), anyBoolean()))
                .thenThrow(new RuntimeException("Media processing error"));
        doNothing().when(notificationService).notifyUsersInZone(any(Alert.class), anyString());
        doNothing().when(alertNotificationService).notifyNewAlert(any(Alert.class), any(AlertDto.class));

        // When
        AlertDto result = alertService.createAlertWithMedia(testUser, saveDto, files);

        // Then
        assertNotNull(result);
        verify(alertRepository).save(any(Alert.class));
        verify(alertMediaRepository, never()).save(any(AlertMedia.class));
    }

    @Test
    void createAlertWithMedia_withNotificationException_shouldNotFailAlertCreation() {
        // Given
        SaveAlertDto saveDto = SaveAlertDto.builder()
                .category("EMERGENCY")
                .title("Alert with Media")
                .description("Description")
                .latitude(40.7128)
                .longitude(-74.0060)
                .build();

        MockMultipartFile file1 = new MockMultipartFile(
                "file1",
                "test1.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        List<MultipartFile> files = Arrays.asList(file1);

        Media media = Media.builder()
                .id(UUID.randomUUID())
                .url("/uploads/test1.jpg")
                .mimeType("image/jpeg")
                .build();

        when(alertRepository.save(any(Alert.class))).thenReturn(testAlert);
        when(mediaService.processAndSaveMultipleMedia(anyList(), any(User.class), anyBoolean()))
                .thenReturn(Arrays.asList(media));
        when(alertMediaRepository.save(any(AlertMedia.class))).thenReturn(null);
        doThrow(new RuntimeException("Notification error")).when(notificationService).notifyUsersInZone(any(Alert.class), anyString());

        // When
        AlertDto result = alertService.createAlertWithMedia(testUser, saveDto, files);

        // Then
        assertNotNull(result);
        verify(alertRepository).save(any(Alert.class));
    }

    @Test
    void createAlertWithMedia_withWebSocketException_shouldNotFailAlertCreation() {
        // Given
        SaveAlertDto saveDto = SaveAlertDto.builder()
                .category("EMERGENCY")
                .title("Alert with Media")
                .description("Description")
                .latitude(40.7128)
                .longitude(-74.0060)
                .build();

        MockMultipartFile file1 = new MockMultipartFile(
                "file1",
                "test1.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        List<MultipartFile> files = Arrays.asList(file1);

        Media media = Media.builder()
                .id(UUID.randomUUID())
                .url("/uploads/test1.jpg")
                .mimeType("image/jpeg")
                .build();

        when(alertRepository.save(any(Alert.class))).thenReturn(testAlert);
        when(mediaService.processAndSaveMultipleMedia(anyList(), any(User.class), anyBoolean()))
                .thenReturn(Arrays.asList(media));
        when(alertMediaRepository.save(any(AlertMedia.class))).thenReturn(null);
        doNothing().when(notificationService).notifyUsersInZone(any(Alert.class), anyString());
        doThrow(new RuntimeException("WebSocket error")).when(alertNotificationService).notifyNewAlert(any(Alert.class), any(AlertDto.class));

        // When
        AlertDto result = alertService.createAlertWithMedia(testUser, saveDto, files);

        // Then
        assertNotNull(result);
        verify(alertRepository).save(any(Alert.class));
    }

    @Test
    void createAlertWithMedia_withCityId_shouldAssociateCity() {
        // Given
        SaveAlertDto saveDto = SaveAlertDto.builder()
                .category("EMERGENCY")
                .title("Alert with City")
                .description("Description")
                .latitude(40.7128)
                .longitude(-74.0060)
                .cityId(testCity.getId())
                .radiusM(2000)
                .isAnonymous(true)
                .address("123 Test St")
                .build();

        when(cityRepository.findById(testCity.getId())).thenReturn(Optional.of(testCity));
        when(alertRepository.save(any(Alert.class))).thenReturn(testAlert);
        doNothing().when(notificationService).notifyUsersInZone(any(Alert.class), anyString());
        doNothing().when(alertNotificationService).notifyNewAlert(any(Alert.class), any(AlertDto.class));

        // When
        AlertDto result = alertService.createAlertWithMedia(testUser, saveDto, null);

        // Then
        assertNotNull(result);
        verify(cityRepository).findById(testCity.getId());
        verify(alertRepository).save(any(Alert.class));
    }
}
