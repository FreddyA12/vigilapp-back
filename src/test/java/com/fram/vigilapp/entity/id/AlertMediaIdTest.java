package com.fram.vigilapp.entity.id;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AlertMediaIdTest {

    @Test
    void testNoArgsConstructor() {
        // When
        AlertMediaId id = new AlertMediaId();

        // Then
        assertNotNull(id);
        assertNull(id.getAlert());
        assertNull(id.getMedia());
    }

    @Test
    void testAllArgsConstructor() {
        // Given
        UUID alertId = UUID.randomUUID();
        UUID mediaId = UUID.randomUUID();

        // When
        AlertMediaId id = new AlertMediaId(alertId, mediaId);

        // Then
        assertEquals(alertId, id.getAlert());
        assertEquals(mediaId, id.getMedia());
    }

    @Test
    void testSettersAndGetters() {
        // Given
        AlertMediaId id = new AlertMediaId();
        UUID alertId = UUID.randomUUID();
        UUID mediaId = UUID.randomUUID();

        // When
        id.setAlert(alertId);
        id.setMedia(mediaId);

        // Then
        assertEquals(alertId, id.getAlert());
        assertEquals(mediaId, id.getMedia());
    }

    @Test
    void testEqualsAndHashCode() {
        // Given
        UUID alertId = UUID.randomUUID();
        UUID mediaId = UUID.randomUUID();
        AlertMediaId id1 = new AlertMediaId(alertId, mediaId);
        AlertMediaId id2 = new AlertMediaId(alertId, mediaId);
        AlertMediaId id3 = new AlertMediaId(UUID.randomUUID(), UUID.randomUUID());

        // Then
        assertEquals(id1, id2);
        assertNotEquals(id1, id3);
        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    void testToString() {
        // Given
        UUID alertId = UUID.randomUUID();
        UUID mediaId = UUID.randomUUID();
        AlertMediaId id = new AlertMediaId(alertId, mediaId);

        // When
        String toString = id.toString();

        // Then
        assertNotNull(toString);
        assertTrue(toString.contains(alertId.toString()));
        assertTrue(toString.contains(mediaId.toString()));
    }
}
