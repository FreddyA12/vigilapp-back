package com.fram.vigilapp.config.auth;

import com.fram.vigilapp.service.impl.CustomUserDetailsService;
import com.fram.vigilapp.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtRequestFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtRequestFilter jwtRequestFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldNotFilter_withPublicUrl_shouldReturnTrue() {
        // Given
        when(request.getRequestURI()).thenReturn("/api/register");

        // When
        boolean result = jwtRequestFilter.shouldNotFilter(request);

        // Then
        assertTrue(result);
    }

    @Test
    void shouldNotFilter_withLoginUrl_shouldReturnTrue() {
        // Given
        when(request.getRequestURI()).thenReturn("/api/login");

        // When
        boolean result = jwtRequestFilter.shouldNotFilter(request);

        // Then
        assertTrue(result);
    }

    @Test
    void shouldNotFilter_withProtectedUrl_shouldReturnFalse() {
        // Given
        when(request.getRequestURI()).thenReturn("/api/alerts");

        // When
        boolean result = jwtRequestFilter.shouldNotFilter(request);

        // Then
        assertFalse(result);
    }

    @Test
    void doFilterInternal_withValidToken_shouldAuthenticateUser() throws ServletException, IOException {
        // Given
        String token = "valid.jwt.token";
        String username = "test@example.com";
        UserDetails userDetails = new User(username, "password", Collections.emptyList());

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.extractUsername(token)).thenReturn(username);
        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
        when(jwtUtil.validateToken(token, userDetails)).thenReturn(true);

        // When
        jwtRequestFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(username, SecurityContextHolder.getContext().getAuthentication().getName());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_withNoAuthorizationHeader_shouldContinueChain() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn(null);

        // When
        jwtRequestFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verify(jwtUtil, never()).extractUsername(anyString());
    }

    @Test
    void doFilterInternal_withInvalidToken_shouldNotAuthenticate() throws ServletException, IOException {
        // Given
        String token = "invalid.jwt.token";
        String username = "test@example.com";
        UserDetails userDetails = new User(username, "password", Collections.emptyList());

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.extractUsername(token)).thenReturn(username);
        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
        when(jwtUtil.validateToken(token, userDetails)).thenReturn(false);

        // When
        jwtRequestFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_withMalformedHeader_shouldContinueChain() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn("InvalidFormat");

        // When
        jwtRequestFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_withException_shouldContinueChain() throws ServletException, IOException {
        // Given
        String token = "valid.jwt.token";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.extractUsername(token)).thenThrow(new RuntimeException("JWT parsing error"));

        // When
        jwtRequestFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }
}
