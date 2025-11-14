package com.fram.vigilapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fram.vigilapp.config.auth.AuthenticationRequest;
import com.fram.vigilapp.dto.UserDto;
import com.fram.vigilapp.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    private UserDto userDto;
    private MockMultipartFile fotoCedula;
    private MockMultipartFile selfie;

    @BeforeEach
    void setUp() {
        userDto = new UserDto();
        userDto.setEmail("test@example.com");
        userDto.setFirstName("John");
        userDto.setLastName("Doe");

        fotoCedula = new MockMultipartFile("fotoCedula", "cedula.jpg", "image/jpeg", "cedula content".getBytes());
        selfie = new MockMultipartFile("selfie", "selfie.jpg", "image/jpeg", "selfie content".getBytes());
    }

    @Test
    void registerUser_WithValidData_ShouldReturnCreatedUser() throws Exception {
        // Given
        when(authService.register(any())).thenReturn(userDto);

        // When & Then
        mockMvc.perform(multipart("/api/register")
                        .file(fotoCedula)
                        .file(selfie)
                        .param("firstName", "John")
                        .param("lastName", "Doe")
                        .param("email", "test@example.com")
                        .param("password", "password123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"));

        verify(authService).register(any());
    }

    @Test
    void registerUser_WithMissingFotoCedula_ShouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(multipart("/api/register")
                        .file(selfie)
                        .param("firstName", "John")
                        .param("lastName", "Doe")
                        .param("email", "test@example.com")
                        .param("password", "password123"))
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any());
    }

    @Test
    void registerUser_WithMissingSelfie_ShouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(multipart("/api/register")
                        .file(fotoCedula)
                        .param("firstName", "John")
                        .param("lastName", "Doe")
                        .param("email", "test@example.com")
                        .param("password", "password123"))
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any());
    }

    @Test
    void login_WithValidCredentials_ShouldReturnJwtToken() throws Exception {
        // Given
        AuthenticationRequest authRequest = new AuthenticationRequest();
        authRequest.setEmail("test@example.com");
        authRequest.setPassword("password123");

        when(authService.login(any(AuthenticationRequest.class))).thenReturn("jwt-token-123");

        // When & Then
        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string("jwt-token-123"));

        verify(authService).login(any(AuthenticationRequest.class));
    }

    @Test
    void login_WithInvalidEmail_ShouldReturnBadRequest() throws Exception {
        // Given
        AuthenticationRequest authRequest = new AuthenticationRequest();
        authRequest.setEmail("invalid-email");
        authRequest.setPassword("password123");

        // When & Then
        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_WithMissingPassword_ShouldReturnBadRequest() throws Exception {
        // Given
        AuthenticationRequest authRequest = new AuthenticationRequest();
        authRequest.setEmail("test@example.com");

        // When & Then
        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isBadRequest());
    }
}
