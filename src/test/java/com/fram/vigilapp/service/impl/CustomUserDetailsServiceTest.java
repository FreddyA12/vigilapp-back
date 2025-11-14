package com.fram.vigilapp.service.impl;

import com.fram.vigilapp.entity.User;
import com.fram.vigilapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
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
                .passwordHash("hashedPassword123")
                .role("USER")
                .build();
    }

    @Test
    void loadUserByUsername_WithValidEmail_ShouldReturnUserDetails() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(testUser);

        // When
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("test@example.com");

        // Then
        assertNotNull(userDetails);
        assertEquals("test@example.com", userDetails.getUsername());
        assertEquals("hashedPassword123", userDetails.getPassword());
        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    void loadUserByUsername_ShouldMapRoleCorrectly() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(testUser);

        // When
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("test@example.com");

        // Then
        assertEquals(1, userDetails.getAuthorities().size());
        GrantedAuthority authority = userDetails.getAuthorities().iterator().next();
        assertEquals("ROLE_USER", authority.getAuthority());
    }

    @Test
    void loadUserByUsername_WithAdminRole_ShouldMapToRoleAdmin() {
        // Given
        testUser.setRole("ADMIN");
        when(userRepository.findByEmail("admin@example.com")).thenReturn(testUser);

        // When
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("admin@example.com");

        // Then
        GrantedAuthority authority = userDetails.getAuthorities().iterator().next();
        assertEquals("ROLE_ADMIN", authority.getAuthority());
    }

    @Test
    void loadUserByUsername_WithModRole_ShouldMapToRoleMod() {
        // Given
        testUser.setRole("MOD");
        when(userRepository.findByEmail("mod@example.com")).thenReturn(testUser);

        // When
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("mod@example.com");

        // Then
        GrantedAuthority authority = userDetails.getAuthorities().iterator().next();
        assertEquals("ROLE_MOD", authority.getAuthority());
    }

    @Test
    void loadUserByUsername_WithNonExistentUser_ShouldThrowException() {
        // Given
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(null);

        // When & Then
        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername("nonexistent@example.com")
        );

        assertEquals("Credenciales inválidas", exception.getMessage());
        verify(userRepository).findByEmail("nonexistent@example.com");
    }

    @Test
    void loadUserByUsername_WithNullEmail_ShouldQueryRepository() {
        // Given
        when(userRepository.findByEmail(null)).thenReturn(null);

        // When & Then
        assertThrows(
                UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername(null)
        );

        verify(userRepository).findByEmail(null);
    }
}
