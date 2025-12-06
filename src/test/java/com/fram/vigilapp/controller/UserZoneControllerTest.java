package com.fram.vigilapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fram.vigilapp.dto.SaveUserZoneDto;
import com.fram.vigilapp.dto.UserZoneDto;
import com.fram.vigilapp.entity.User;
import com.fram.vigilapp.repository.UserRepository;
import com.fram.vigilapp.service.UserZoneService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserZoneControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserZoneService userZoneService;

    @MockitoBean
    private UserRepository userRepository;

    private User testUser;
    private UserZoneDto userZoneDto;
    private SaveUserZoneDto saveUserZoneDto;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();

        userZoneDto = UserZoneDto.builder()
                .id(UUID.randomUUID())
                .userId(testUser.getId())
                .centerLatitude(40.7128)
                .centerLongitude(-74.0060)
                .radiusM(5000)
                .build();

        saveUserZoneDto = SaveUserZoneDto.builder()
                .centerLatitude(40.7128)
                .centerLongitude(-74.0060)
                .radiusM(5000)
                .build();
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void createOrUpdateUserZone_shouldCreateZone() throws Exception {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(testUser);
        when(userZoneService.createOrUpdateUserZone(any(User.class), any(SaveUserZoneDto.class)))
                .thenReturn(userZoneDto);

        // When & Then
        mockMvc.perform(post("/api/user-zones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveUserZoneDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.centerLatitude").value(40.7128))
                .andExpect(jsonPath("$.radiusM").value(5000));

        verify(userRepository).findByEmail("test@example.com");
        verify(userZoneService).createOrUpdateUserZone(any(User.class), any(SaveUserZoneDto.class));
    }

    @Test
    @WithMockUser(username = "nonexistent@example.com", roles = "USER")
    void createOrUpdateUserZone_withNonExistentUser_shouldReturnUnauthorized() throws Exception {
        // Given
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(null);

        // When & Then
        mockMvc.perform(post("/api/user-zones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveUserZoneDto)))
                .andExpect(status().isUnauthorized());

        verify(userRepository).findByEmail("nonexistent@example.com");
        verify(userZoneService, never()).createOrUpdateUserZone(any(), any());
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void getMyUserZone_shouldReturnZone() throws Exception {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(testUser);
        when(userZoneService.getUserZone(testUser.getId())).thenReturn(userZoneDto);

        // When & Then
        mockMvc.perform(get("/api/user-zones/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.centerLatitude").value(40.7128))
                .andExpect(jsonPath("$.radiusM").value(5000));

        verify(userZoneService).getUserZone(testUser.getId());
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void getMyUserZone_withNoZone_shouldReturnNotFound() throws Exception {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(testUser);
        when(userZoneService.getUserZone(testUser.getId())).thenReturn(null);

        // When & Then
        mockMvc.perform(get("/api/user-zones/me"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void deleteMyUserZone_shouldReturnNoContent() throws Exception {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(testUser);
        doNothing().when(userZoneService).deleteUserZone(testUser.getId());

        // When & Then
        mockMvc.perform(delete("/api/user-zones/me"))
                .andExpect(status().isNoContent());

        verify(userZoneService).deleteUserZone(testUser.getId());
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void createOrUpdateUserZone_withServiceException_shouldReturnServerError() throws Exception {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(testUser);
        when(userZoneService.createOrUpdateUserZone(any(User.class), any(SaveUserZoneDto.class)))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then
        mockMvc.perform(post("/api/user-zones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveUserZoneDto)))
                .andExpect(status().is5xxServerError());

        verify(userZoneService).createOrUpdateUserZone(any(User.class), any(SaveUserZoneDto.class));
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void getMyUserZone_withServiceException_shouldReturnServerError() throws Exception {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(testUser);
        when(userZoneService.getUserZone(testUser.getId()))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then
        mockMvc.perform(get("/api/user-zones/me"))
                .andExpect(status().is5xxServerError());

        verify(userZoneService).getUserZone(testUser.getId());
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void deleteMyUserZone_withServiceException_shouldReturnServerError() throws Exception {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(testUser);
        doThrow(new RuntimeException("Delete error")).when(userZoneService).deleteUserZone(testUser.getId());

        // When & Then
        mockMvc.perform(delete("/api/user-zones/me"))
                .andExpect(status().is5xxServerError());

        verify(userZoneService).deleteUserZone(testUser.getId());
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void createOrUpdateUserZone_withLargeRadius_shouldProcess() throws Exception {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(testUser);

        SaveUserZoneDto largeRadiusDto = SaveUserZoneDto.builder()
                .centerLatitude(40.7128)
                .centerLongitude(-74.0060)
                .radiusM(50000) // 50km
                .build();

        UserZoneDto largeZoneDto = UserZoneDto.builder()
                .id(UUID.randomUUID())
                .userId(testUser.getId())
                .centerLatitude(40.7128)
                .centerLongitude(-74.0060)
                .radiusM(50000)
                .build();

        when(userZoneService.createOrUpdateUserZone(any(User.class), any(SaveUserZoneDto.class)))
                .thenReturn(largeZoneDto);

        // When & Then
        mockMvc.perform(post("/api/user-zones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(largeRadiusDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.radiusM").value(50000));

        verify(userZoneService).createOrUpdateUserZone(any(User.class), any(SaveUserZoneDto.class));
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void createOrUpdateUserZone_withDifferentCoordinates_shouldProcess() throws Exception {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(testUser);

        SaveUserZoneDto tokyoDto = SaveUserZoneDto.builder()
                .centerLatitude(35.6762)
                .centerLongitude(139.6503)
                .radiusM(10000)
                .build();

        UserZoneDto tokyoZoneDto = UserZoneDto.builder()
                .id(UUID.randomUUID())
                .userId(testUser.getId())
                .centerLatitude(35.6762)
                .centerLongitude(139.6503)
                .radiusM(10000)
                .build();

        when(userZoneService.createOrUpdateUserZone(any(User.class), any(SaveUserZoneDto.class)))
                .thenReturn(tokyoZoneDto);

        // When & Then
        mockMvc.perform(post("/api/user-zones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tokyoDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.centerLatitude").value(35.6762))
                .andExpect(jsonPath("$.centerLongitude").value(139.6503));

        verify(userZoneService).createOrUpdateUserZone(any(User.class), any(SaveUserZoneDto.class));
    }
}
