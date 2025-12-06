package com.fram.vigilapp.service.impl;

import com.fram.vigilapp.dto.SaveUserZoneDto;
import com.fram.vigilapp.dto.UserZoneDto;
import com.fram.vigilapp.entity.User;
import com.fram.vigilapp.entity.UserZone;
import com.fram.vigilapp.repository.UserZoneRepository;
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

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserZoneServiceImplTest {

    @Mock
    private UserZoneRepository userZoneRepository;

    @InjectMocks
    private UserZoneServiceImpl userZoneService;

    private User testUser;
    private UserZone testUserZone;
    private SaveUserZoneDto saveUserZoneDto;
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

        GeometricShapeFactory shapeFactory = new GeometricShapeFactory();
        shapeFactory.setNumPoints(32);
        shapeFactory.setCentre(new Coordinate(-74.0060, 40.7128));
        shapeFactory.setSize(0.01);
        Polygon polygon = shapeFactory.createCircle();
        polygon.setSRID(4326);

        testUserZone = UserZone.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .geometry(polygon)
                .radiusM(1000)
                .build();

        saveUserZoneDto = SaveUserZoneDto.builder()
                .centerLatitude(40.7128)
                .centerLongitude(-74.0060)
                .radiusM(1000)
                .build();
    }

    @Test
    void createOrUpdateUserZone_withNewZone_shouldCreateZone() {
        // Given
        when(userZoneRepository.findByUser(testUser)).thenReturn(Optional.empty());
        when(userZoneRepository.save(any(UserZone.class))).thenReturn(testUserZone);

        // When
        UserZoneDto result = userZoneService.createOrUpdateUserZone(testUser, saveUserZoneDto);

        // Then
        assertNotNull(result);
        assertEquals(testUserZone.getId(), result.getId());
        assertEquals(testUser.getId(), result.getUserId());
        assertEquals(40.7128, result.getCenterLatitude());
        assertEquals(-74.0060, result.getCenterLongitude());
        assertEquals(1000, result.getRadiusM());
        verify(userZoneRepository).save(any(UserZone.class));
    }

    @Test
    void createOrUpdateUserZone_withExistingZone_shouldUpdateZone() {
        // Given
        when(userZoneRepository.findByUser(testUser)).thenReturn(Optional.of(testUserZone));
        when(userZoneRepository.save(any(UserZone.class))).thenReturn(testUserZone);

        // When
        UserZoneDto result = userZoneService.createOrUpdateUserZone(testUser, saveUserZoneDto);

        // Then
        assertNotNull(result);
        verify(userZoneRepository).findByUser(testUser);
        verify(userZoneRepository).save(any(UserZone.class));
    }

    @Test
    void createOrUpdateUserZone_withException_shouldPropagateException() {
        // Given
        when(userZoneRepository.findByUser(testUser)).thenReturn(Optional.empty());
        when(userZoneRepository.save(any(UserZone.class)))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then
        assertThrows(RuntimeException.class, () ->
                userZoneService.createOrUpdateUserZone(testUser, saveUserZoneDto)
        );
    }

    @Test
    void getUserZone_withExistingZone_shouldReturnZone() {
        // Given
        when(userZoneRepository.findByUserId(testUser.getId()))
                .thenReturn(Optional.of(testUserZone));

        // When
        UserZoneDto result = userZoneService.getUserZone(testUser.getId());

        // Then
        assertNotNull(result);
        assertEquals(testUserZone.getId(), result.getId());
        assertEquals(testUser.getId(), result.getUserId());
        assertNotNull(result.getCenterLatitude());
        assertNotNull(result.getCenterLongitude());
        assertEquals(1000, result.getRadiusM());
    }

    @Test
    void getUserZone_withNonExistentZone_shouldReturnNull() {
        // Given
        when(userZoneRepository.findByUserId(testUser.getId()))
                .thenReturn(Optional.empty());

        // When
        UserZoneDto result = userZoneService.getUserZone(testUser.getId());

        // Then
        assertNull(result);
        verify(userZoneRepository).findByUserId(testUser.getId());
    }

    @Test
    void deleteUserZone_withExistingZone_shouldDeleteZone() {
        // Given
        when(userZoneRepository.findByUserId(testUser.getId()))
                .thenReturn(Optional.of(testUserZone));
        doNothing().when(userZoneRepository).delete(testUserZone);

        // When
        userZoneService.deleteUserZone(testUser.getId());

        // Then
        verify(userZoneRepository).findByUserId(testUser.getId());
        verify(userZoneRepository).delete(testUserZone);
    }

    @Test
    void deleteUserZone_withNonExistentZone_shouldNotThrowException() {
        // Given
        when(userZoneRepository.findByUserId(testUser.getId()))
                .thenReturn(Optional.empty());

        // When & Then
        assertDoesNotThrow(() -> userZoneService.deleteUserZone(testUser.getId()));
        verify(userZoneRepository).findByUserId(testUser.getId());
        verify(userZoneRepository, never()).delete(any());
    }

    @Test
    void createOrUpdateUserZone_shouldCreateCorrectPolygon() {
        // Given
        SaveUserZoneDto dto = SaveUserZoneDto.builder()
                .centerLatitude(10.0)
                .centerLongitude(20.0)
                .radiusM(5000)
                .build();

        when(userZoneRepository.findByUser(testUser)).thenReturn(Optional.empty());
        when(userZoneRepository.save(any(UserZone.class))).thenAnswer(invocation -> {
            UserZone savedZone = invocation.getArgument(0);
            assertNotNull(savedZone.getGeometry());
            assertEquals(4326, savedZone.getGeometry().getSRID());
            assertEquals(5000, savedZone.getRadiusM());
            return savedZone;
        });

        // When
        userZoneService.createOrUpdateUserZone(testUser, dto);

        // Then
        verify(userZoneRepository).save(any(UserZone.class));
    }
}
