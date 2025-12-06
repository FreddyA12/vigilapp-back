package com.fram.vigilapp.entity;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void testUserBuilder() {
        // Given
        UUID id = UUID.randomUUID();
        String email = "test@example.com";
        String passwordHash = "hashedPassword";
        String firstName = "John";
        String lastName = "Doe";
        String phone = "+1234567890";
        String role = "USER";
        String status = "ACTIVE";
        OffsetDateTime now = OffsetDateTime.now();

        // When
        User user = User.builder()
                .id(id)
                .email(email)
                .passwordHash(passwordHash)
                .firstName(firstName)
                .lastName(lastName)
                .phone(phone)
                .role(role)
                .status(status)
                .createdAt(now)
                .updatedAt(now)
                .build();

        // Then
        assertNotNull(user);
        assertEquals(id, user.getId());
        assertEquals(email, user.getEmail());
        assertEquals(passwordHash, user.getPasswordHash());
        assertEquals(firstName, user.getFirstName());
        assertEquals(lastName, user.getLastName());
        assertEquals(phone, user.getPhone());
        assertEquals(role, user.getRole());
        assertEquals(status, user.getStatus());
        assertEquals(now, user.getCreatedAt());
        assertEquals(now, user.getUpdatedAt());
    }

    @Test
    void testUserSettersAndGetters() {
        // Given
        User user = new User();
        UUID id = UUID.randomUUID();
        String email = "new@example.com";

        // When
        user.setId(id);
        user.setEmail(email);
        user.setFirstName("Jane");
        user.setLastName("Smith");
        user.setRole("ADMIN");
        user.setStatus("PENDING");

        // Then
        assertEquals(id, user.getId());
        assertEquals(email, user.getEmail());
        assertEquals("Jane", user.getFirstName());
        assertEquals("Smith", user.getLastName());
        assertEquals("ADMIN", user.getRole());
        assertEquals("PENDING", user.getStatus());
    }

    @Test
    void testUserAllArgsConstructor() {
        // Given
        UUID id = UUID.randomUUID();
        String email = "test@example.com";
        String passwordHash = "hash";
        OffsetDateTime now = OffsetDateTime.now();

        // When
        User user = new User(id, email, passwordHash, "John", "Doe", "123",
                "NATIONAL_ID", "123456", "USER", "ACTIVE", "Address",
                null, now, now);

        // Then
        assertNotNull(user);
        assertEquals(id, user.getId());
        assertEquals(email, user.getEmail());
        assertEquals("John", user.getFirstName());
    }

    @Test
    void testUserNoArgsConstructor() {
        // When
        User user = new User();

        // Then
        assertNotNull(user);
        assertNull(user.getId());
        assertNull(user.getEmail());
    }

    @Test
    void testUserDocumentFields() {
        // Given
        User user = User.builder()
                .documentType("PASSPORT")
                .documentNumber("AB123456")
                .build();

        // Then
        assertEquals("PASSPORT", user.getDocumentType());
        assertEquals("AB123456", user.getDocumentNumber());
    }

    @Test
    void testUserAddressAndCity() {
        // Given
        City city = new City();
        User user = User.builder()
                .address("123 Main St")
                .city(city)
                .build();

        // Then
        assertEquals("123 Main St", user.getAddress());
        assertEquals(city, user.getCity());
    }
}
