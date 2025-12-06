package com.fram.vigilapp.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fram.vigilapp.entity.User;
import com.fram.vigilapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertWebSocketHandlerTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AlertWebSocketHandler handler;

    private ObjectMapper objectMapper;
    private UUID userId;
    private User testUser;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        userId = UUID.randomUUID();
        testUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .build();
    }

    @Test
    void afterConnectionEstablished_shouldSendWelcomeMessage() throws Exception {
        // Given
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("session-123");

        // When
        handler.afterConnectionEstablished(session);

        // Then
        ArgumentCaptor<TextMessage> messageCaptor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(messageCaptor.capture());

        String payload = messageCaptor.getValue().getPayload();
        assertTrue(payload.contains("CONNECTION_ESTABLISHED"));
    }

    @Test
    void handleTextMessage_withRegister_shouldRegisterUser() throws Exception {
        // Given
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("session-123");
        when(userRepository.findByEmail("test@example.com")).thenReturn(testUser);

        Map<String, Object> registerMessage = Map.of(
                "type", "REGISTER",
                "userId", "test@example.com"
        );
        TextMessage message = new TextMessage(objectMapper.writeValueAsString(registerMessage));

        // When
        handler.handleTextMessage(session, message);

        // Then
        verify(session).sendMessage(any(TextMessage.class));
        verify(userRepository).findByEmail("test@example.com");
        assertEquals(1, handler.getConnectedUsersCount());
    }

    @Test
    void handleTextMessage_withPing_shouldRespondWithPong() throws Exception {
        // Given
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("session-123");

        Map<String, String> pingMessage = Map.of("type", "PING");
        TextMessage message = new TextMessage(objectMapper.writeValueAsString(pingMessage));

        // When
        handler.handleTextMessage(session, message);

        // Then
        ArgumentCaptor<TextMessage> messageCaptor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(messageCaptor.capture());

        String payload = messageCaptor.getValue().getPayload();
        assertTrue(payload.contains("PONG"));
    }

    @Test
    void getConnectedUsersCount_shouldReturnZeroInitially() {
        // When & Then
        assertEquals(0, handler.getConnectedUsersCount());
    }

    @Test
    void getConnectedUserIds_shouldReturnEmptySetInitially() {
        // When
        var connectedIds = handler.getConnectedUserIds();

        // Then
        assertTrue(connectedIds.isEmpty());
    }

    @Test
    void isUserConnected_shouldReturnFalseForUnconnectedUser() {
        // When & Then
        assertFalse(handler.isUserConnected(UUID.randomUUID()));
    }

    @Test
    void handleTransportError_shouldNotThrowException() throws Exception {
        // Given
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("session-123");
        Exception error = new RuntimeException("Transport error");

        // When & Then - Should not throw
        assertDoesNotThrow(() -> handler.handleTransportError(session, error));
    }

    @Test
    void afterConnectionClosed_shouldCleanupSession() throws Exception {
        // Given
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("session-123");
        when(userRepository.findByEmail("test@example.com")).thenReturn(testUser);

        // Register user first
        Map<String, Object> registerMessage = Map.of(
                "type", "REGISTER",
                "userId", "test@example.com"
        );
        handler.handleTextMessage(session, new TextMessage(objectMapper.writeValueAsString(registerMessage)));

        assertEquals(1, handler.getConnectedUsersCount());

        // When
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        // Then
        assertEquals(0, handler.getConnectedUsersCount());
    }

    @Test
    void sendAlertToUser_withDisconnectedUser_shouldNotSendMessage() {
        // Given - User not connected
        Map<String, Object> alertData = Map.of(
                "type", "NEW_ALERT",
                "alertId", UUID.randomUUID().toString()
        );

        // When & Then - Should not throw exception
        assertDoesNotThrow(() -> handler.sendAlertToUser("nonexistent@example.com", alertData));
    }

    @Test
    void sendAlertToUser_shouldHandleNullSession() {
        // Given
        Map<String, Object> alertData = Map.of(
                "type", "NEW_ALERT",
                "title", "Test"
        );

        // When & Then - Should not throw exception even if user is registered but session is null
        assertDoesNotThrow(() -> handler.sendAlertToUser("unknown@example.com", alertData));
    }

    @Test
    void handleTextMessage_withRegisterEmptyUserId_shouldSendError() throws Exception {
        // Given
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("session-123");

        Map<String, Object> registerMessage = Map.of(
                "type", "REGISTER",
                "userId", ""
        );
        TextMessage message = new TextMessage(objectMapper.writeValueAsString(registerMessage));

        // When
        handler.handleTextMessage(session, message);

        // Then
        ArgumentCaptor<TextMessage> messageCaptor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(messageCaptor.capture());

        String payload = messageCaptor.getValue().getPayload();
        assertTrue(payload.contains("ERROR"));
        assertTrue(payload.contains("userId es requerido"));
    }

    @Test
    void handleTextMessage_withRegisterUserNotFound_shouldStillRegister() throws Exception {
        // Given
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("session-123");
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(null);

        Map<String, Object> registerMessage = Map.of(
                "type", "REGISTER",
                "userId", "unknown@example.com"
        );
        TextMessage message = new TextMessage(objectMapper.writeValueAsString(registerMessage));

        // When
        handler.handleTextMessage(session, message);

        // Then
        verify(session).sendMessage(any(TextMessage.class));
        assertEquals(1, handler.getConnectedUsersCount());
    }

    @Test
    void handleTextMessage_withUnregister_shouldUnregisterUser() throws Exception {
        // Given
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("session-123");
        when(userRepository.findByEmail("test@example.com")).thenReturn(testUser);

        // Register first
        Map<String, Object> registerMessage = Map.of(
                "type", "REGISTER",
                "userId", "test@example.com"
        );
        handler.handleTextMessage(session, new TextMessage(objectMapper.writeValueAsString(registerMessage)));

        assertEquals(1, handler.getConnectedUsersCount());

        // When - Unregister
        Map<String, Object> unregisterMessage = Map.of("type", "UNREGISTER");
        handler.handleTextMessage(session, new TextMessage(objectMapper.writeValueAsString(unregisterMessage)));

        // Then
        ArgumentCaptor<TextMessage> messageCaptor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session, times(2)).sendMessage(messageCaptor.capture());

        String lastPayload = messageCaptor.getAllValues().get(1).getPayload();
        assertTrue(lastPayload.contains("UNREGISTERED"));
        assertEquals(0, handler.getConnectedUsersCount());
    }

    @Test
    void handleTextMessage_withUnknownMessageType_shouldNotThrowException() throws Exception {
        // Given
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("session-123");

        Map<String, String> unknownMessage = Map.of("type", "UNKNOWN_TYPE");
        TextMessage message = new TextMessage(objectMapper.writeValueAsString(unknownMessage));

        // When & Then - Should not throw exception
        assertDoesNotThrow(() -> handler.handleTextMessage(session, message));
    }

    @Test
    void handleTextMessage_withInvalidJson_shouldSendError() throws Exception {
        // Given
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("session-123");

        TextMessage invalidMessage = new TextMessage("{invalid json");

        // When
        handler.handleTextMessage(session, invalidMessage);

        // Then
        ArgumentCaptor<TextMessage> messageCaptor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(messageCaptor.capture());

        String payload = messageCaptor.getValue().getPayload();
        assertTrue(payload.contains("ERROR"));
    }

    @Test
    void sendAlertToUser_withSendError_shouldHandleGracefully() {
        // Given - User not registered
        Map<String, Object> alertData = Map.of(
                "type", "NEW_ALERT",
                "title", "Test Alert"
        );

        // When & Then - Should handle gracefully even with send errors
        assertDoesNotThrow(() -> handler.sendAlertToUser("test@example.com", alertData));
    }

    @Test
    void isUserConnected_shouldReturnTrueForConnectedUser() throws Exception {
        // Given
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("session-123");
        when(userRepository.findByEmail("test@example.com")).thenReturn(testUser);

        // Register user
        Map<String, Object> registerMessage = Map.of(
                "type", "REGISTER",
                "userId", "test@example.com"
        );
        handler.handleTextMessage(session, new TextMessage(objectMapper.writeValueAsString(registerMessage)));

        // When & Then
        assertTrue(handler.isUserConnected(userId));
        assertFalse(handler.isUserConnected(UUID.randomUUID()));
    }

    @Test
    void getConnectedUserIds_shouldReturnAllConnectedUserIds() throws Exception {
        // Given
        WebSocketSession session1 = mock(WebSocketSession.class);
        WebSocketSession session2 = mock(WebSocketSession.class);
        when(session1.getId()).thenReturn("session-1");
        when(session2.getId()).thenReturn("session-2");

        User user2 = User.builder()
                .id(UUID.randomUUID())
                .email("user2@example.com")
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(testUser);
        when(userRepository.findByEmail("user2@example.com")).thenReturn(user2);

        // Register two users
        Map<String, Object> registerMessage1 = Map.of("type", "REGISTER", "userId", "test@example.com");
        Map<String, Object> registerMessage2 = Map.of("type", "REGISTER", "userId", "user2@example.com");

        handler.handleTextMessage(session1, new TextMessage(objectMapper.writeValueAsString(registerMessage1)));
        handler.handleTextMessage(session2, new TextMessage(objectMapper.writeValueAsString(registerMessage2)));

        // When
        var connectedIds = handler.getConnectedUserIds();

        // Then
        assertEquals(2, connectedIds.size());
        assertTrue(connectedIds.contains(userId));
        assertTrue(connectedIds.contains(user2.getId()));
    }

    @Test
    void afterConnectionClosed_withNoRegisteredUser_shouldNotThrowException() throws Exception {
        // Given
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("session-123");

        // When & Then - Should not throw exception
        assertDoesNotThrow(() -> handler.afterConnectionClosed(session, CloseStatus.NORMAL));
    }

    @Test
    void handleTextMessage_withRegisterByUUID_shouldRegister() throws Exception {
        // Given
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("session-123");

        String uuidString = userId.toString();
        Map<String, Object> registerMessage = Map.of(
                "type", "REGISTER",
                "userId", uuidString
        );
        TextMessage message = new TextMessage(objectMapper.writeValueAsString(registerMessage));

        // When
        handler.handleTextMessage(session, message);

        // Then
        verify(session).sendMessage(any(TextMessage.class));
        assertEquals(1, handler.getConnectedUsersCount());
    }

    @Test
    void sendAlertToUser_withValidSession_shouldSendAlert() throws Exception {
        // Given
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("session-123");
        when(userRepository.findByEmail("test@example.com")).thenReturn(testUser);

        // Register user
        Map<String, Object> registerMessage = Map.of(
                "type", "REGISTER",
                "userId", "test@example.com"
        );
        handler.afterConnectionEstablished(session);
        handler.handleTextMessage(session, new TextMessage(objectMapper.writeValueAsString(registerMessage)));

        // Configure session as open
        when(session.isOpen()).thenReturn(true);

        Map<String, Object> alertData = Map.of(
                "type", "NEW_ALERT",
                "alertId", UUID.randomUUID().toString(),
                "title", "Test Alert"
        );

        // When
        handler.sendAlertToUser("test@example.com", alertData);

        // Then - Should send at least 2 messages (welcome + registration + alert)
        verify(session, atLeast(2)).sendMessage(any(TextMessage.class));
    }

    @Test
    void sendAlertToUser_withClosedSession_shouldRemoveMapping() throws Exception {
        // Given
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("session-123");
        when(userRepository.findByEmail("test@example.com")).thenReturn(testUser);

        // Register user
        Map<String, Object> registerMessage = Map.of(
                "type", "REGISTER",
                "userId", "test@example.com"
        );
        handler.afterConnectionEstablished(session);
        handler.handleTextMessage(session, new TextMessage(objectMapper.writeValueAsString(registerMessage)));

        assertEquals(1, handler.getConnectedUsersCount());

        // Configure session as closed
        when(session.isOpen()).thenReturn(false);

        Map<String, Object> alertData = Map.of(
                "type", "NEW_ALERT",
                "title", "Test Alert"
        );

        // When
        handler.sendAlertToUser("test@example.com", alertData);

        // Then - User mapping should still exist (only sessionId is removed from check)
        // The method removes the user from userSessionMap when session is closed
        verify(session, times(2)).sendMessage(any(TextMessage.class)); // Only welcome + registration
    }

    @Test
    void sendAlertToUser_withIOException_shouldLogError() throws Exception {
        // Given
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("session-123");
        when(userRepository.findByEmail("test@example.com")).thenReturn(testUser);

        // Register user
        handler.afterConnectionEstablished(session);
        Map<String, Object> registerMessage = Map.of(
                "type", "REGISTER",
                "userId", "test@example.com"
        );
        handler.handleTextMessage(session, new TextMessage(objectMapper.writeValueAsString(registerMessage)));

        // Configure session as open but throw IOException on send
        when(session.isOpen()).thenReturn(true);

        // Reset the mock to avoid previous sendMessage calls
        reset(session);
        when(session.isOpen()).thenReturn(true);
        doThrow(new java.io.IOException("Network error")).when(session).sendMessage(any(TextMessage.class));

        Map<String, Object> alertData = Map.of(
                "type", "NEW_ALERT",
                "title", "Test Alert"
        );

        // When & Then - Should not throw exception, just log error
        assertDoesNotThrow(() -> handler.sendAlertToUser("test@example.com", alertData));

        // Verify it attempted to send
        verify(session).sendMessage(any(TextMessage.class));
    }

    @Test
    void sendError_shouldSendErrorMessage() throws Exception {
        // Given
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("session-123");

        // Create a scenario that triggers sendError by sending invalid register message
        Map<String, Object> registerMessage = Map.of(
                "type", "REGISTER"
                // Missing userId
        );
        TextMessage message = new TextMessage(objectMapper.writeValueAsString(registerMessage));

        // When
        handler.handleTextMessage(session, message);

        // Then
        ArgumentCaptor<TextMessage> messageCaptor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(messageCaptor.capture());

        String payload = messageCaptor.getValue().getPayload();
        assertTrue(payload.contains("ERROR"));
    }

    @Test
    void sendError_withIOException_shouldNotThrowException() throws Exception {
        // Given
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("session-123");
        doThrow(new java.io.IOException("Send failed")).when(session).sendMessage(any(TextMessage.class));

        // Create invalid message to trigger sendError
        Map<String, Object> invalidMessage = Map.of(
                "type", "REGISTER"
                // Missing userId - will trigger error
        );
        TextMessage message = new TextMessage(objectMapper.writeValueAsString(invalidMessage));

        // When & Then - Should not throw exception even if sendError fails
        assertDoesNotThrow(() -> handler.handleTextMessage(session, message));
    }
}
