package com.fram.vigilapp.entity;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AlertTest {

    private final GeometryFactory geometryFactory = new GeometryFactory();

    @Test
    void testAlertBuilder() {
        // Given
        UUID id = UUID.randomUUID();
        User user = User.builder().id(UUID.randomUUID()).build();
        Point point = geometryFactory.createPoint(new Coordinate(-74.0060, 40.7128));
        OffsetDateTime now = OffsetDateTime.now();

        // When
        Alert alert = Alert.builder()
                .id(id)
                .createdByUser(user)
                .category("EMERGENCY")
                .status("ACTIVE")
                .verificationStatus("PENDING")
                .title("Emergency Alert")
                .description("Test description")
                .isAnonymous(false)
                .geometry(point)
                .radiusM(1000)
                .createdAt(now)
                .build();

        // Then
        assertNotNull(alert);
        assertEquals(id, alert.getId());
        assertEquals(user, alert.getCreatedByUser());
        assertEquals("EMERGENCY", alert.getCategory());
        assertEquals("ACTIVE", alert.getStatus());
        assertEquals("PENDING", alert.getVerificationStatus());
        assertEquals("Emergency Alert", alert.getTitle());
        assertEquals("Test description", alert.getDescription());
        assertFalse(alert.getIsAnonymous());
        assertEquals(point, alert.getGeometry());
        assertEquals(1000, alert.getRadiusM());
        assertEquals(now, alert.getCreatedAt());
    }

    @Test
    void testAlertSettersAndGetters() {
        // Given
        Alert alert = new Alert();
        UUID id = UUID.randomUUID();

        // When
        alert.setId(id);
        alert.setCategory("INFO");
        alert.setStatus("RESOLVED");
        alert.setTitle("Information");
        alert.setDescription("Details");
        alert.setIsAnonymous(true);

        // Then
        assertEquals(id, alert.getId());
        assertEquals("INFO", alert.getCategory());
        assertEquals("RESOLVED", alert.getStatus());
        assertEquals("Information", alert.getTitle());
        assertEquals("Details", alert.getDescription());
        assertTrue(alert.getIsAnonymous());
    }

    @Test
    void testAlertGeometry() {
        // Given
        Point point = geometryFactory.createPoint(new Coordinate(10.5, 20.3));
        Alert alert = new Alert();

        // When
        alert.setGeometry(point);

        // Then
        assertNotNull(alert.getGeometry());
        assertEquals(10.5, alert.getGeometry().getX());
        assertEquals(20.3, alert.getGeometry().getY());
    }

    @Test
    void testAlertWithAllStatuses() {
        // Test all possible status combinations
        Alert alert = Alert.builder()
                .category("COMMUNITY")
                .status("CANCELLED")
                .verificationStatus("REJECTED")
                .build();

        assertEquals("COMMUNITY", alert.getCategory());
        assertEquals("CANCELLED", alert.getStatus());
        assertEquals("REJECTED", alert.getVerificationStatus());
    }

    @Test
    void testAlertResolvedAt() {
        // Given
        OffsetDateTime resolvedTime = OffsetDateTime.now();
        Alert alert = new Alert();

        // When
        alert.setResolvedAt(resolvedTime);

        // Then
        assertEquals(resolvedTime, alert.getResolvedAt());
    }

    @Test
    void testAlertWithCity() {
        // Given
        City city = new City();
        Alert alert = Alert.builder()
                .city(city)
                .address("123 Emergency St")
                .build();

        // Then
        assertEquals(city, alert.getCity());
        assertEquals("123 Emergency St", alert.getAddress());
    }
}
