package com.fram.vigilapp.util;

import com.fram.vigilapp.entity.User;
import com.fram.vigilapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserUtilTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private UserUtil userUtil;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @Mock
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void getUserId_withUserDetails_shouldReturnUserId() {
        // Given
        String email = "test@example.com";
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email(email)
                .build();

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn(email);
        when(userRepository.findByEmail(email)).thenReturn(user);

        // When
        UUID result = userUtil.getUserId();

        // Then
        assertEquals(userId, result);
        verify(userRepository).findByEmail(email);
    }

    @Test
    void getUserId_withStringPrincipal_shouldReturnUserId() {
        // Given
        String email = "test@example.com";
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email(email)
                .build();

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(email);
        when(userRepository.findByEmail(email)).thenReturn(user);

        // When
        UUID result = userUtil.getUserId();

        // Then
        assertEquals(userId, result);
        verify(userRepository).findByEmail(email);
    }

    @Test
    void getUserId_withNoAuthentication_shouldThrowUnauthorizedException() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(null);

        // When & Then
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userUtil.getUserId()
        );

        assertTrue(exception.getMessage().contains("Necesita autenticación"));
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void getUserId_withNonExistentUser_shouldThrowNotFoundException() {
        // Given
        String email = "nonexistent@example.com";

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn(email);
        when(userRepository.findByEmail(email)).thenReturn(null);

        // When & Then
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userUtil.getUserId()
        );

        assertTrue(exception.getMessage().contains("No existe el usuario"));
        verify(userRepository).findByEmail(email);
    }
}
