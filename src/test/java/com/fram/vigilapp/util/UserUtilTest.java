package com.fram.vigilapp.util;

import com.fram.vigilapp.entity.User;
import com.fram.vigilapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserUtilTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserUtil userUtil;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void getUserId_WithUserDetails_ShouldReturnUserId() {
        // Given
        UUID expectedUserId = UUID.randomUUID();
        String email = "test@example.com";
        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                email, "password", new ArrayList<>());

        User user = User.builder()
                .id(expectedUserId)
                .email(email)
                .build();

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findByEmail(email)).thenReturn(user);

        // When
        UUID actualUserId = userUtil.getUserId();

        // Then
        assertEquals(expectedUserId, actualUserId);
        verify(userRepository).findByEmail(email);
    }

    @Test
    void getUserId_WithStringPrincipal_ShouldReturnUserId() {
        // Given
        UUID expectedUserId = UUID.randomUUID();
        String email = "test@example.com";

        User user = User.builder()
                .id(expectedUserId)
                .email(email)
                .build();

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(email);
        when(userRepository.findByEmail(email)).thenReturn(user);

        // When
        UUID actualUserId = userUtil.getUserId();

        // Then
        assertEquals(expectedUserId, actualUserId);
        verify(userRepository).findByEmail(email);
    }

    @Test
    void getUserId_WithNoAuthentication_ShouldThrowUnauthorizedException() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(null);

        // When & Then
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userUtil.getUserId()
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("Necesita autenticación", exception.getReason());
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void getUserId_WithNonExistentUser_ShouldThrowNotFoundException() {
        // Given
        String email = "nonexistent@example.com";
        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                email, "password", new ArrayList<>());

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findByEmail(email)).thenReturn(null);

        // When & Then
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userUtil.getUserId()
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("No existe el usuario", exception.getReason());
        verify(userRepository).findByEmail(email);
    }
}
