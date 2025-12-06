package com.fram.vigilapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fram.vigilapp.config.auth.AuthenticationRequest;
import com.fram.vigilapp.dto.UserDto;
import com.fram.vigilapp.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    private UserDto userDto;
    private AuthenticationRequest authRequest;

    @BeforeEach
    void setUp() {
        userDto = new UserDto();
        userDto.setId(UUID.randomUUID());
        userDto.setEmail("test@example.com");
        userDto.setFirstName("John");
        userDto.setLastName("Doe");

        authRequest = new AuthenticationRequest();
        authRequest.setEmail("test@example.com");
        authRequest.setPassword("password123");
    }

    @Test
    void registerUser_shouldRegisterSuccessfully() throws Exception {
        // Given
        when(authService.register(any())).thenReturn(userDto);

        MockMultipartFile fotoCedula = new MockMultipartFile(
                "fotoCedula",
                "cedula.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "cedula content".getBytes()
        );

        MockMultipartFile selfie = new MockMultipartFile(
                "selfie",
                "selfie.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "selfie content".getBytes()
        );

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
                .andExpect(jsonPath("$.firstName").value("John"));

        verify(authService).register(any());
    }

    @Test
    void login_shouldReturnJwtToken() throws Exception {
        // Given
        String expectedToken = "jwt-token-123";
        when(authService.login(any(AuthenticationRequest.class))).thenReturn(expectedToken);

        // When & Then
        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string(expectedToken));

        verify(authService).login(any(AuthenticationRequest.class));
    }

    @Test
    void login_withInvalidCredentials_shouldReturnError() throws Exception {
        // Given
        when(authService.login(any(AuthenticationRequest.class)))
                .thenThrow(new RuntimeException("Invalid credentials"));

        // When & Then
        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().is5xxServerError());

        verify(authService).login(any(AuthenticationRequest.class));
    }
}
