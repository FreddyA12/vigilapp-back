package com.fram.vigilapp.config.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void handleValidationErrors_shouldReturnPreconditionFailed() {
        // Given
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("object", "field", "error message");

        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(java.util.List.of(fieldError));

        // When
        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleValidationErrors(exception);

        // Then
        assertEquals(HttpStatus.PRECONDITION_FAILED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("message"));
        assertTrue(response.getBody().get("message").toString().contains("error message"));
    }

    @Test
    void handleResponseStatusException_shouldReturnCorrectStatus() {
        // Given
        ResponseStatusException exception = new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Resource not found"
        );

        // When
        ResponseEntity<?> response = exceptionHandler.handleResponseStatusException(exception);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Resource not found", body.get("message"));
    }

    @Test
    void handleBadCredentialsException_shouldReturnUnauthorized() {
        // Given
        BadCredentialsException exception = new BadCredentialsException("Bad credentials");

        // When
        ResponseEntity<?> response = exceptionHandler.handleBadCredentialsException(exception);

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Credenciales inválidas", body.get("message"));
    }

    @Test
    void handleBadCredentialsException_withCustomMessage_shouldReturnCustomMessage() {
        // Given
        String customMessage = "Account locked";
        BadCredentialsException exception = new BadCredentialsException(customMessage);

        // When
        ResponseEntity<?> response = exceptionHandler.handleBadCredentialsException(exception);

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(customMessage, body.get("message"));
    }

    @Test
    void handleMissingParams_shouldReturnBadRequest() {
        // Given
        MissingServletRequestParameterException exception = new MissingServletRequestParameterException(
                "firstName",
                "String"
        );

        // When
        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleMissingParams(exception);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().get("message").toString().contains("firstName"));
        assertEquals("firstName", response.getBody().get("field"));
        assertEquals("String", response.getBody().get("expectedType"));
    }

    @Test
    void handleMissingFilePart_shouldReturnBadRequest() {
        // Given
        MissingServletRequestPartException exception = new MissingServletRequestPartException("fotoCedula");

        // When
        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleMissingFilePart(exception);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().get("message").toString().contains("fotoCedula"));
        assertEquals("fotoCedula", response.getBody().get("field"));
    }

    @Test
    void handleTypeMismatch_shouldReturnBadRequest() {
        // Given
        MethodArgumentTypeMismatchException exception = mock(MethodArgumentTypeMismatchException.class);
        when(exception.getName()).thenReturn("age");
        when(exception.getRequiredType()).thenReturn((Class) Integer.class);
        when(exception.getValue()).thenReturn("abc");

        // When
        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleTypeMismatch(exception);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("age", response.getBody().get("field"));
        assertEquals("Integer", response.getBody().get("expectedType"));
        assertEquals("abc", response.getBody().get("providedValue"));
    }

    @Test
    void handleHttpMessageNotReadable_shouldReturnBadRequest() {
        // Given
        HttpMessageNotReadableException exception = mock(HttpMessageNotReadableException.class);
        when(exception.getMessage()).thenReturn("JSON parse error");

        // When
        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleHttpMessageNotReadable(exception);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().get("message").toString().contains("JSON"));
    }

    @Test
    void handleHttpMessageNotReadable_withoutJSON_shouldReturnGenericMessage() {
        // Given
        HttpMessageNotReadableException exception = mock(HttpMessageNotReadableException.class);
        when(exception.getMessage()).thenReturn("Malformed request");

        // When
        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleHttpMessageNotReadable(exception);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("message"));
    }

    @Test
    void handleGenericException_shouldReturnInternalServerError() {
        // Given
        Exception exception = new Exception("Unexpected error");

        // When
        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleGenericException(exception);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("message"));
        assertTrue(response.getBody().containsKey("details"));
        assertEquals("Unexpected error", response.getBody().get("details"));
    }
}
