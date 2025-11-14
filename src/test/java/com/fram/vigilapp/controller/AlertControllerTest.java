package com.fram.vigilapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fram.vigilapp.dto.AlertDto;
import com.fram.vigilapp.dto.AlertStatsDto;
import com.fram.vigilapp.dto.SaveAlertDto;
import com.fram.vigilapp.entity.User;
import com.fram.vigilapp.repository.UserRepository;
import com.fram.vigilapp.service.AlertService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AlertController.class)
@AutoConfigureMockMvc(addFilters = false)
class AlertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AlertService alertService;

    @MockBean
    private UserRepository userRepository;

    private User testUser;
    private AlertDto alertDto;
    private SaveAlertDto saveAlertDto;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();

        alertDto = AlertDto.builder()
                .id(UUID.randomUUID())
                .title("Test Alert")
                .description("Test Description")
                .category("EMERGENCY")
                .status("ACTIVE")
                .latitude(10.0)
                .longitude(-75.0)
                .radiusM(1000)
                .build();

        saveAlertDto = new SaveAlertDto();
        saveAlertDto.setTitle("Test Alert");
        saveAlertDto.setDescription("Test Description");
        saveAlertDto.setCategory("EMERGENCY");
        saveAlertDto.setLatitude(10.0);
        saveAlertDto.setLongitude(-75.0);
        saveAlertDto.setRadiusM(1000);
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = {"USER"})
    void createAlert_WithValidData_ShouldReturnCreated() throws Exception {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(testUser);
        when(alertService.createAlert(any(User.class), any(SaveAlertDto.class))).thenReturn(alertDto);

        // When & Then
        mockMvc.perform(post("/api/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveAlertDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Test Alert"))
                .andExpect(jsonPath("$.category").value("EMERGENCY"));

        verify(userRepository).findByEmail("test@example.com");
        verify(alertService).createAlert(any(User.class), any(SaveAlertDto.class));
    }

    @Test
    @WithMockUser(username = "nonexistent@example.com", roles = {"USER"})
    void createAlert_WithNonExistentUser_ShouldReturnUnauthorized() throws Exception {
        // Given
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(null);

        // When & Then
        mockMvc.perform(post("/api/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveAlertDto)))
                .andExpect(status().isUnauthorized());

        verify(userRepository).findByEmail("nonexistent@example.com");
        verify(alertService, never()).createAlert(any(), any());
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = {"USER"})
    void getAlert_WithExistingId_ShouldReturnAlert() throws Exception {
        // Given
        UUID alertId = UUID.randomUUID();
        when(alertService.getAlertById(alertId)).thenReturn(alertDto);

        // When & Then
        mockMvc.perform(get("/api/alerts/{alertId}", alertId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Alert"));

        verify(alertService).getAlertById(alertId);
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = {"USER"})
    void getAlert_WithNonExistentId_ShouldReturnNotFound() throws Exception {
        // Given
        UUID alertId = UUID.randomUUID();
        when(alertService.getAlertById(alertId)).thenThrow(new RuntimeException("Alerta no encontrada"));

        // When & Then
        mockMvc.perform(get("/api/alerts/{alertId}", alertId))
                .andExpect(status().isNotFound());

        verify(alertService).getAlertById(alertId);
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = {"USER"})
    void getAlertsNearLocation_ShouldReturnAlertsList() throws Exception {
        // Given
        List<AlertDto> alerts = Arrays.asList(alertDto);
        when(alertService.getAlertsNearLocation(10.0, -75.0, 5000, true)).thenReturn(alerts);

        // When & Then
        mockMvc.perform(get("/api/alerts/nearby")
                        .param("latitude", "10.0")
                        .param("longitude", "-75.0")
                        .param("radiusM", "5000")
                        .param("activeOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Test Alert"));

        verify(alertService).getAlertsNearLocation(10.0, -75.0, 5000, true);
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = {"USER"})
    void getMyAlerts_ShouldReturnUserAlerts() throws Exception {
        // Given
        List<AlertDto> alerts = Arrays.asList(alertDto);
        when(userRepository.findByEmail("test@example.com")).thenReturn(testUser);
        when(alertService.getUserAlerts(testUser.getId())).thenReturn(alerts);

        // When & Then
        mockMvc.perform(get("/api/alerts/my-alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Test Alert"));

        verify(alertService).getUserAlerts(testUser.getId());
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = {"MOD"})
    void updateAlertStatus_WithModRole_ShouldUpdateStatus() throws Exception {
        // Given
        UUID alertId = UUID.randomUUID();
        AlertDto updatedAlert = AlertDto.builder()
                .id(alertId)
                .title("Test Alert")
                .status("RESOLVED")
                .build();

        when(alertService.updateAlertStatus(alertId, "RESOLVED")).thenReturn(updatedAlert);

        // When & Then
        mockMvc.perform(put("/api/alerts/{alertId}/status", alertId)
                        .param("status", "RESOLVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        verify(alertService).updateAlertStatus(alertId, "RESOLVED");
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = {"ADMIN"})
    void deleteAlert_WithAdminRole_ShouldDeleteAlert() throws Exception {
        // Given
        UUID alertId = UUID.randomUUID();
        doNothing().when(alertService).deleteAlert(alertId);

        // When & Then
        mockMvc.perform(delete("/api/alerts/{alertId}", alertId))
                .andExpect(status().isNoContent());

        verify(alertService).deleteAlert(alertId);
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = {"USER"})
    void getAlertStats_ShouldReturnStatistics() throws Exception {
        // Given
        AlertStatsDto stats = AlertStatsDto.builder()
                .totalAlerts(100L)
                .activeAlerts(50L)
                .resolvedAlerts(40L)
                .cancelledAlerts(10L)
                .alertsByCategory(new HashMap<>())
                .alertsByVerificationStatus(new HashMap<>())
                .timeRange("7d")
                .build();

        when(alertService.getAlertStats("7d", null)).thenReturn(stats);

        // When & Then
        mockMvc.perform(get("/api/alerts/stats")
                        .param("timeRange", "7d"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAlerts").value(100))
                .andExpect(jsonPath("$.activeAlerts").value(50));

        verify(alertService).getAlertStats("7d", null);
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = {"USER"})
    void searchAlerts_WithFilters_ShouldReturnFilteredAlerts() throws Exception {
        // Given
        List<AlertDto> alerts = Arrays.asList(alertDto);
        when(alertService.searchAlerts(
                eq("test"),
                eq("EMERGENCY"),
                eq("ACTIVE"),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                eq(0),
                eq(50)
        )).thenReturn(alerts);

        // When & Then
        mockMvc.perform(get("/api/alerts/search")
                        .param("query", "test")
                        .param("category", "EMERGENCY")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Test Alert"));
    }
}
