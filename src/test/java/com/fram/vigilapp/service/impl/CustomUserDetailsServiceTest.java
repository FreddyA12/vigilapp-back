package com.fram.vigilapp.service.impl;

import com.fram.vigilapp.entity.User;
import com.fram.vigilapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .passwordHash("$2a$10$hashedPassword")
                .firstName("John")
                .lastName("Doe")
                .role("USER")
                .status("ACTIVE")
                .build();
    }

    @Test
    void loadUserByUsername_withValidEmail_shouldReturnUserDetails() {
        // Given
        String email = "test@example.com";
        when(userRepository.findByEmail(email)).thenReturn(testUser);

        // When
        UserDetails result = customUserDetailsService.loadUserByUsername(email);

        // Then
        assertNotNull(result);
        assertEquals(email, result.getUsername());
        assertEquals(testUser.getPasswordHash(), result.getPassword());
        assertTrue(result.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_USER")));
        verify(userRepository).findByEmail(email);
    }

    @Test
    void loadUserByUsername_withAdminRole_shouldReturnAdminAuthority() {
        // Given
        testUser.setRole("ADMIN");
        String email = "admin@example.com";
        testUser.setEmail(email);
        when(userRepository.findByEmail(email)).thenReturn(testUser);

        // When
        UserDetails result = customUserDetailsService.loadUserByUsername(email);

        // Then
        assertNotNull(result);
        assertTrue(result.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void loadUserByUsername_withModRole_shouldReturnModAuthority() {
        // Given
        testUser.setRole("MOD");
        String email = "mod@example.com";
        testUser.setEmail(email);
        when(userRepository.findByEmail(email)).thenReturn(testUser);

        // When
        UserDetails result = customUserDetailsService.loadUserByUsername(email);

        // Then
        assertNotNull(result);
        assertTrue(result.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_MOD")));
    }

    @Test
    void loadUserByUsername_withNonExistentEmail_shouldThrowException() {
        // Given
        String nonExistentEmail = "nonexistent@example.com";
        when(userRepository.findByEmail(nonExistentEmail)).thenReturn(null);

        // When & Then
        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername(nonExistentEmail)
        );

        assertTrue(exception.getMessage().contains("Credenciales inválidas"));
        verify(userRepository).findByEmail(nonExistentEmail);
    }

    @Test
    void loadUserByUsername_shouldCreateSingleAuthority() {
        // Given
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(testUser);

        // When
        UserDetails result = customUserDetailsService.loadUserByUsername(testUser.getEmail());

        // Then
        assertEquals(1, result.getAuthorities().size());
    }
}
