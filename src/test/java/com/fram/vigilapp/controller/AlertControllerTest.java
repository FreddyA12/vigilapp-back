package com.fram.vigilapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fram.vigilapp.dto.AlertDto;
import com.fram.vigilapp.dto.AlertStatsDto;
import com.fram.vigilapp.dto.HeatmapPointDto;
import com.fram.vigilapp.dto.SaveAlertDto;
import com.fram.vigilapp.entity.User;
import com.fram.vigilapp.repository.UserRepository;
import com.fram.vigilapp.service.AlertService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AlertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AlertService alertService;

    @MockitoBean
    private UserRepository userRepository;

    private User testUser;
    private AlertDto alertDto;
    private SaveAlertDto saveAlertDto;
    private UUID alertId;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();

        alertId = UUID.randomUUID();
        alertDto = AlertDto.builder()
                .id(alertId)
                .title("Test Alert")
                .description("Test Description")
                .category("EMERGENCY")
                .status("ACTIVE")
                .latitude(40.7128)
                .longitude(-74.0060)
                .radiusM(1000)
                .createdAt(OffsetDateTime.now())
                .build();

        saveAlertDto = SaveAlertDto.builder()
                .title("Test Alert")
                .description("Test Description")
                .category("EMERGENCY")
                .latitude(40.7128)
                .longitude(-74.0060)
                .radiusM(1000)
                .cityId(UUID.randomUUID())
                .build();
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void createAlert_shouldCreateAlert() throws Exception {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(testUser);
        when(alertService.createAlert(any(User.class), any(SaveAlertDto.class))).thenReturn(alertDto);

        // When & Then
        mockMvc.perform(post("/api/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveAlertDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Test Alert"));

        verify(userRepository).findByEmail("test@example.com");
        verify(alertService).createAlert(any(User.class), any(SaveAlertDto.class));
    }

    @Test
    @WithMockUser(username = "nonexistent@example.com", roles = "USER")
    void createAlert_withNonExistentUser_shouldReturnUnauthorized() throws Exception {
        // Given
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(null);

        // When & Then
        mockMvc.perform(post("/api/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveAlertDto)))
                .andExpect(status().isUnauthorized());

        verify(alertService, never()).createAlert(any(), any());
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void createAlertWithMedia_shouldCreateAlert() throws Exception {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(testUser);
        when(alertService.createAlertWithMedia(any(User.class), any(SaveAlertDto.class), anyList()))
                .thenReturn(alertDto);

        MockMultipartFile alertPart = new MockMultipartFile(
                "alert",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsString(saveAlertDto).getBytes()
        );

        MockMultipartFile filePart = new MockMultipartFile(
                "files",
                "test.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "test image content".getBytes()
        );

        // When & Then
        mockMvc.perform(multipart("/api/alerts/with-media")
                        .file(alertPart)
                        .file(filePart))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Test Alert"));

        verify(alertService).createAlertWithMedia(any(User.class), any(SaveAlertDto.class), anyList());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getAlert_shouldReturnAlert() throws Exception {
        // Given
        when(alertService.getAlertById(alertId)).thenReturn(alertDto);

        // When & Then
        mockMvc.perform(get("/api/alerts/{alertId}", alertId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(alertId.toString()))
                .andExpect(jsonPath("$.title").value("Test Alert"));

        verify(alertService).getAlertById(alertId);
    }

    @Test
    @WithMockUser(roles = "USER")
    void getAlert_withNonExistentId_shouldReturnNotFound() throws Exception {
        // Given
        when(alertService.getAlertById(alertId)).thenThrow(new RuntimeException("Not found"));

        // When & Then
        mockMvc.perform(get("/api/alerts/{alertId}", alertId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getAlertsNearLocation_shouldReturnAlerts() throws Exception {
        // Given
        when(alertService.getAlertsNearLocation(anyDouble(), anyDouble(), anyInt(), anyBoolean()))
                .thenReturn(Arrays.asList(alertDto));

        // When & Then
        mockMvc.perform(get("/api/alerts/nearby")
                        .param("latitude", "40.7128")
                        .param("longitude", "-74.0060")
                        .param("radiusM", "5000")
                        .param("activeOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Test Alert"));

        verify(alertService).getAlertsNearLocation(40.7128, -74.0060, 5000, true);
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void getAlertsInMyZone_shouldReturnAlerts() throws Exception {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(testUser);
        when(alertService.getAlertsInUserZone(testUser.getId())).thenReturn(Arrays.asList(alertDto));

        // When & Then
        mockMvc.perform(get("/api/alerts/my-zone"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Test Alert"));

        verify(alertService).getAlertsInUserZone(testUser.getId());
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void getAlertsInMyZone_withException_shouldReturnBadRequest() throws Exception {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(testUser);
        when(alertService.getAlertsInUserZone(testUser.getId())).thenThrow(new RuntimeException("Zone not found"));

        // When & Then
        mockMvc.perform(get("/api/alerts/my-zone"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void getMyAlerts_shouldReturnUserAlerts() throws Exception {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(testUser);
        when(alertService.getUserAlerts(testUser.getId())).thenReturn(Arrays.asList(alertDto));

        // When & Then
        mockMvc.perform(get("/api/alerts/my-alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Test Alert"));

        verify(alertService).getUserAlerts(testUser.getId());
    }

    @Test
    @WithMockUser(roles = "MOD")
    void getAlertsByStatus_shouldReturnAlerts() throws Exception {
        // Given
        when(alertService.getAlertsByStatus("ACTIVE")).thenReturn(Arrays.asList(alertDto));

        // When & Then
        mockMvc.perform(get("/api/alerts/by-status")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));

        verify(alertService).getAlertsByStatus("ACTIVE");
    }

    @Test
    @WithMockUser(roles = "USER")
    void getAlertsByCategoryAndStatus_shouldReturnAlerts() throws Exception {
        // Given
        when(alertService.getAlertsByCategoryAndStatus("EMERGENCY", "ACTIVE"))
                .thenReturn(Arrays.asList(alertDto));

        // When & Then
        mockMvc.perform(get("/api/alerts/by-category-status")
                        .param("category", "EMERGENCY")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("EMERGENCY"));

        verify(alertService).getAlertsByCategoryAndStatus("EMERGENCY", "ACTIVE");
    }

    @Test
    @WithMockUser(roles = "MOD")
    void updateAlertStatus_shouldUpdateStatus() throws Exception {
        // Given
        when(alertService.updateAlertStatus(alertId, "RESOLVED")).thenReturn(alertDto);

        // When & Then
        mockMvc.perform(put("/api/alerts/{alertId}/status", alertId)
                        .param("status", "RESOLVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(alertId.toString()));

        verify(alertService).updateAlertStatus(alertId, "RESOLVED");
    }

    @Test
    @WithMockUser(roles = "MOD")
    void updateAlertStatus_withNonExistentId_shouldReturnNotFound() throws Exception {
        // Given
        when(alertService.updateAlertStatus(alertId, "RESOLVED"))
                .thenThrow(new RuntimeException("Not found"));

        // When & Then
        mockMvc.perform(put("/api/alerts/{alertId}/status", alertId)
                        .param("status", "RESOLVED"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteAlert_shouldDeleteAlert() throws Exception {
        // Given
        doNothing().when(alertService).deleteAlert(alertId);

        // When & Then
        mockMvc.perform(delete("/api/alerts/{alertId}", alertId))
                .andExpect(status().isNoContent());

        verify(alertService).deleteAlert(alertId);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteAlert_withNonExistentId_shouldReturnNotFound() throws Exception {
        // Given
        doThrow(new RuntimeException("Not found")).when(alertService).deleteAlert(alertId);

        // When & Then
        mockMvc.perform(delete("/api/alerts/{alertId}", alertId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getRecentAlerts_shouldReturnPagedAlerts() throws Exception {
        // Given
        Page<AlertDto> page = new PageImpl<>(Arrays.asList(alertDto), PageRequest.of(0, 20), 1);
        when(alertService.getRecentAlerts(any())).thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/alerts/recent")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Test Alert"));

        verify(alertService).getRecentAlerts(any());
    }

    @Test
    @WithMockUser(roles = "USER")
    void searchAlerts_shouldReturnFilteredAlerts() throws Exception {
        // Given
        when(alertService.searchAlerts(
                anyString(), anyString(), anyString(), anyString(), any(),
                any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(Arrays.asList(alertDto));

        // When & Then
        mockMvc.perform(get("/api/alerts/search")
                        .param("query", "test")
                        .param("category", "EMERGENCY")
                        .param("status", "ACTIVE")
                        .param("skip", "0")
                        .param("limit", "50"))
                .andExpect(status().isOk());

        verify(alertService).searchAlerts(
                any(), any(), any(), any(), any(),
                any(), any(), any(), any(), anyInt(), anyInt());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getHeatmapData_shouldReturnHeatmapPoints() throws Exception {
        // Given
        HeatmapPointDto heatmapPoint = HeatmapPointDto.builder()
                .latitude(40.7128)
                .longitude(-74.0060)
                .intensity(5.0)
                .build();
        when(alertService.getHeatmapData(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(Arrays.asList(heatmapPoint));

        // When & Then
        mockMvc.perform(get("/api/alerts/heatmap")
                        .param("swLat", "40.0")
                        .param("swLon", "-75.0")
                        .param("neLat", "41.0")
                        .param("neLon", "-73.0")
                        .param("gridSizeM", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].intensity").value(5));

        verify(alertService).getHeatmapData(40.0, -75.0, 41.0, -73.0, 1000.0);
    }

    @Test
    @WithMockUser(roles = "USER")
    void getAlertStats_shouldReturnStatistics() throws Exception {
        // Given
        AlertStatsDto stats = AlertStatsDto.builder()
                .totalAlerts(100L)
                .activeAlerts(50L)
                .resolvedAlerts(30L)
                .build();
        when(alertService.getAlertStats(anyString(), any())).thenReturn(stats);

        // When & Then
        mockMvc.perform(get("/api/alerts/stats")
                        .param("timeRange", "7d"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAlerts").value(100))
                .andExpect(jsonPath("$.activeAlerts").value(50));

        verify(alertService).getAlertStats("7d", null);
    }
}
