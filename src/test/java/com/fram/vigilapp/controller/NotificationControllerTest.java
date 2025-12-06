package com.fram.vigilapp.controller;

import com.fram.vigilapp.dto.NotificationDto;
import com.fram.vigilapp.service.NotificationService;
import com.fram.vigilapp.util.JwtUtil;
import com.fram.vigilapp.util.UserUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private UserUtil userUtil;

    private UUID userId;
    private UUID notificationId;
    private NotificationDto notificationDto;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        notificationId = UUID.randomUUID();

        notificationDto = NotificationDto.builder()
                .id(notificationId)
                .alertId(UUID.randomUUID())
                .alertTitle("Test Alert")
                .alertCategory("EMERGENCY")
                .userId(userId)
                .channel("PUSH")
                .status("QUEUED")
                .createdAt(OffsetDateTime.now())
                .build();

        when(userUtil.getUserId()).thenReturn(userId);
    }

    @Test
    @WithMockUser(roles = "USER")
    void getAllUserNotifications_shouldReturnPage() throws Exception {
        // Given
        List<NotificationDto> notifications = Arrays.asList(notificationDto);
        Page<NotificationDto> page = new PageImpl<>(notifications, PageRequest.of(0, 20), 1);

        when(notificationService.getUserNotifications(eq(userId), any())).thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/notifications")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").exists())
                .andExpect(jsonPath("$.content[0].alertTitle").value("Test Alert"));

        verify(notificationService).getUserNotifications(eq(userId), any());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getNotification_shouldReturnNotification() throws Exception {
        // Given
        when(notificationService.getNotificationById(notificationId)).thenReturn(notificationDto);

        // When & Then
        mockMvc.perform(get("/api/notifications/{id}", notificationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(notificationId.toString()))
                .andExpect(jsonPath("$.alertTitle").value("Test Alert"));

        verify(notificationService).getNotificationById(notificationId);
    }

    @Test
    @WithMockUser(roles = "USER")
    void markAsDelivered_shouldReturnUpdatedNotification() throws Exception {
        // Given
        when(notificationService.markAsDelivered(notificationId)).thenReturn(notificationDto);

        // When & Then
        mockMvc.perform(put("/api/notifications/{id}/delivered", notificationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(notificationId.toString()));

        verify(notificationService).markAsDelivered(notificationId);
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteNotification_shouldReturnNoContent() throws Exception {
        // Given
        doNothing().when(notificationService).deleteNotification(notificationId);

        // When & Then
        mockMvc.perform(delete("/api/notifications/{id}", notificationId))
                .andExpect(status().isNoContent());

        verify(notificationService).deleteNotification(notificationId);
    }

    @Test
    @WithMockUser(roles = "USER")
    void countUndeliveredNotifications_shouldReturnCount() throws Exception {
        // Given
        when(notificationService.countUndeliveredNotifications(userId)).thenReturn(5L);

        // When & Then
        mockMvc.perform(get("/api/notifications/undelivered/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(5));

        verify(notificationService).countUndeliveredNotifications(userId);
    }

    @Test
    @WithMockUser(roles = "USER")
    void countUnreadNotifications_shouldReturnCount() throws Exception {
        // Given
        when(notificationService.countUnreadNotifications(userId)).thenReturn(3L);

        // When & Then
        mockMvc.perform(get("/api/notifications/unread/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(3));

        verify(notificationService).countUnreadNotifications(userId);
    }

    @Test
    @WithMockUser(roles = "USER")
    void markAsRead_shouldReturnUpdatedNotification() throws Exception {
        // Given
        when(notificationService.markAsRead(notificationId)).thenReturn(notificationDto);

        // When & Then
        mockMvc.perform(put("/api/notifications/{id}/read", notificationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(notificationId.toString()));

        verify(notificationService).markAsRead(notificationId);
    }

    @Test
    @WithMockUser(roles = "USER")
    void markAllAsRead_shouldReturnNoContent() throws Exception {
        // Given
        when(notificationService.markAllAsRead(userId)).thenReturn(5);

        // When & Then
        mockMvc.perform(put("/api/notifications/read-all"))
                .andExpect(status().isNoContent());

        verify(notificationService).markAllAsRead(userId);
    }

    @Test
    @WithMockUser(roles = "MOD")
    void getQueuedNotifications_shouldReturnList() throws Exception {
        // Given
        when(notificationService.getQueuedNotifications())
                .thenReturn(Arrays.asList(notificationDto));

        // When & Then
        mockMvc.perform(get("/api/notifications/queued"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].alertTitle").value("Test Alert"));

        verify(notificationService).getQueuedNotifications();
    }

    @Test
    @WithMockUser(roles = "MOD")
    void getQueuedNotificationsByChannel_shouldReturnList() throws Exception {
        // Given
        when(notificationService.getQueuedNotificationsByChannel("PUSH"))
                .thenReturn(Arrays.asList(notificationDto));

        // When & Then
        mockMvc.perform(get("/api/notifications/queued/by-channel")
                        .param("channel", "PUSH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].channel").value("PUSH"));

        verify(notificationService).getQueuedNotificationsByChannel("PUSH");
    }
}
