package com.fram.vigilapp.controller;

import com.fram.vigilapp.controller.WebSocketController.UserRegistrationMessage;
import com.fram.vigilapp.service.AlertNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class WebSocketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebSocketController webSocketController;

    @MockitoBean
    private AlertNotificationService alertNotificationService;

    private UUID userId;
    private String sessionId;
    private UserRegistrationMessage registrationMessage;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        sessionId = "session-123";
        registrationMessage = new UserRegistrationMessage(userId.toString());
    }

    @Test
    void registerUser_shouldRegisterUserInNotificationService() {
        // Given
        SimpMessageHeaderAccessor headerAccessor = createHeaderAccessor();
        doNothing().when(alertNotificationService).registerUser(any(UUID.class), anyString());

        // When
        webSocketController.registerUser(registrationMessage, headerAccessor);

        // Then
        verify(alertNotificationService).registerUser(userId, sessionId);
    }

    @Test
    void unregisterUser_shouldUnregisterUserFromNotificationService() {
        // Given
        SimpMessageHeaderAccessor headerAccessor = createHeaderAccessor();
        doNothing().when(alertNotificationService).unregisterUser(any(UUID.class), anyString());

        // When
        webSocketController.unregisterUser(registrationMessage, headerAccessor);

        // Then
        verify(alertNotificationService).unregisterUser(userId, sessionId);
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(roles = "USER")
    void getConnectedUsers_shouldReturnConnectedUsersCount() throws Exception {
        // Given
        when(alertNotificationService.getConnectedUsersCount()).thenReturn(10L);

        // When & Then
        mockMvc.perform(get("/api/alerts/connected-users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connectedUsers").value(10));

        verify(alertNotificationService).getConnectedUsersCount();
    }

    @Test
    void userRegistrationMessage_shouldSetAndGetUserId() {
        // Given
        String testUserId = UUID.randomUUID().toString();
        UserRegistrationMessage message = new UserRegistrationMessage();

        // When
        message.setUserId(testUserId);

        // Then
        assert message.getUserId().equals(testUserId);
    }

    @Test
    void userRegistrationMessage_shouldConstructWithUserId() {
        // Given
        String testUserId = UUID.randomUUID().toString();

        // When
        UserRegistrationMessage message = new UserRegistrationMessage(testUserId);

        // Then
        assert message.getUserId().equals(testUserId);
    }

    private SimpMessageHeaderAccessor createHeaderAccessor() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("simpSessionId", sessionId);
        GenericMessage<byte[]> message = new GenericMessage<>(new byte[0], headers);
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(message);
        return accessor;
    }
}
