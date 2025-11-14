package com.fram.vigilapp.util;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        // Set the secret key using reflection
        ReflectionTestUtils.setField(jwtUtil, "SECRET_KEY",
            "vigilapp-secret-key-for-jwt-token-generation-minimum-256-bits-required-for-hs256");

        userDetails = new User("test@example.com", "password", new ArrayList<>());
    }

    @Test
    void generateToken_ShouldReturnValidToken() {
        // When
        String token = jwtUtil.generateToken(userDetails);

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.split("\\.").length == 3); // JWT has 3 parts
    }

    @Test
    void extractUsername_ShouldReturnCorrectUsername() {
        // Given
        String token = jwtUtil.generateToken(userDetails);

        // When
        String username = jwtUtil.extractUsername(token);

        // Then
        assertEquals("test@example.com", username);
    }

    @Test
    void extractExpiration_ShouldReturnFutureDate() {
        // Given
        String token = jwtUtil.generateToken(userDetails);

        // When
        Date expiration = jwtUtil.extractExpiration(token);

        // Then
        assertNotNull(expiration);
        assertTrue(expiration.after(new Date()));
    }

    @Test
    void extractClaim_ShouldExtractSubject() {
        // Given
        String token = jwtUtil.generateToken(userDetails);

        // When
        String subject = jwtUtil.extractClaim(token, Claims::getSubject);

        // Then
        assertEquals("test@example.com", subject);
    }

    @Test
    void validateToken_WithValidToken_ShouldReturnTrue() {
        // Given
        String token = jwtUtil.generateToken(userDetails);

        // When
        Boolean isValid = jwtUtil.validateToken(token, userDetails);

        // Then
        assertTrue(isValid);
    }

    @Test
    void validateToken_WithDifferentUser_ShouldReturnFalse() {
        // Given
        String token = jwtUtil.generateToken(userDetails);
        UserDetails differentUser = new User("different@example.com", "password", new ArrayList<>());

        // When
        Boolean isValid = jwtUtil.validateToken(token, differentUser);

        // Then
        assertFalse(isValid);
    }

    @Test
    void validateToken_WithInvalidToken_ShouldReturnFalse() {
        // Given
        String invalidToken = "invalid.token.here";

        // When
        Boolean isValid = jwtUtil.validateToken(invalidToken, userDetails);

        // Then
        assertFalse(isValid);
    }

    @Test
    void validateToken_WithMalformedToken_ShouldReturnFalse() {
        // Given
        String malformedToken = "malformed";

        // When
        Boolean isValid = jwtUtil.validateToken(malformedToken, userDetails);

        // Then
        assertFalse(isValid);
    }

    @Test
    void extractExpiration_ShouldBeApproximately10HoursInFuture() {
        // Given
        long beforeGeneration = System.currentTimeMillis();
        String token = jwtUtil.generateToken(userDetails);
        long afterGeneration = System.currentTimeMillis();

        // When
        Date expiration = jwtUtil.extractExpiration(token);

        // Then
        long tenHoursInMs = 1000L * 60 * 60 * 10;
        long expectedMinExpiration = beforeGeneration + tenHoursInMs;
        long expectedMaxExpiration = afterGeneration + tenHoursInMs;

        assertTrue(expiration.getTime() >= expectedMinExpiration - 1000);
        assertTrue(expiration.getTime() <= expectedMaxExpiration + 1000);
    }

    @Test
    void generateToken_MultipleTimes_ShouldGenerateDifferentTokens() {
        // When
        String token1 = jwtUtil.generateToken(userDetails);
        String token2 = jwtUtil.generateToken(userDetails);

        // Then
        assertNotEquals(token1, token2); // Different issued at times
    }
}
