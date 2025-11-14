package com.fram.vigilapp.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fram.vigilapp.dto.FaceVerificationResponse;
import com.fram.vigilapp.dto.IdValidationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FaceVerificationServiceImplTest {

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private FaceVerificationServiceImpl faceVerificationService;

    private RestTemplate restTemplate;
    private MockMultipartFile idImage;
    private MockMultipartFile selfie;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(faceVerificationService, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(faceVerificationService, "faceVerificationServiceUrl", "http://localhost:8000");

        idImage = new MockMultipartFile("idImage", "id.jpg", "image/jpeg", "id content".getBytes());
        selfie = new MockMultipartFile("selfie", "selfie.jpg", "image/jpeg", "selfie content".getBytes());
    }

    @Test
    void validateIdDocument_WithValidId_ShouldReturnValidResponse() throws Exception {
        // Given
        IdValidationResponse expectedResponse = new IdValidationResponse();
        expectedResponse.setIsIdDocument(true);
        expectedResponse.setConfidence(0.95);

        when(restTemplate.exchange(
                eq("http://localhost:8000/validate-id"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(IdValidationResponse.class)
        )).thenReturn(new ResponseEntity<>(expectedResponse, HttpStatus.OK));

        // When
        IdValidationResponse result = faceVerificationService.validateIdDocument(idImage);

        // Then
        assertNotNull(result);
        assertTrue(result.getIsIdDocument());
        assertEquals(0.95, result.getConfidence());
        verify(restTemplate).exchange(
                eq("http://localhost:8000/validate-id"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(IdValidationResponse.class)
        );
    }

    @Test
    void validateIdDocument_WithInvalidId_ShouldReturnInvalidResponse() throws Exception {
        // Given
        IdValidationResponse expectedResponse = new IdValidationResponse();
        expectedResponse.setIsIdDocument(false);
        expectedResponse.setConfidence(0.3);

        when(restTemplate.exchange(
                eq("http://localhost:8000/validate-id"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(IdValidationResponse.class)
        )).thenReturn(new ResponseEntity<>(expectedResponse, HttpStatus.OK));

        // When
        IdValidationResponse result = faceVerificationService.validateIdDocument(idImage);

        // Then
        assertNotNull(result);
        assertFalse(result.getIsIdDocument());
        assertEquals(0.3, result.getConfidence());
    }

    @Test
    void validateIdDocument_WhenServiceThrowsException_ShouldThrowRuntimeException() {
        // Given
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(IdValidationResponse.class)
        )).thenThrow(new RestClientException("Service unavailable"));

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> faceVerificationService.validateIdDocument(idImage)
        );

        assertTrue(exception.getMessage().contains("Error validando documento de identidad"));
    }

    @Test
    void verifyFace_WithMatchingFaces_ShouldReturnMatchResponse() throws Exception {
        // Given
        FaceVerificationResponse expectedResponse = new FaceVerificationResponse();
        expectedResponse.setMatch(true);
        expectedResponse.setSimilarity(0.85);
        expectedResponse.setDistance(0.35);
        expectedResponse.setThreshold(0.6);

        when(restTemplate.exchange(
                eq("http://localhost:8000/verify-face"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(FaceVerificationResponse.class)
        )).thenReturn(new ResponseEntity<>(expectedResponse, HttpStatus.OK));

        // When
        FaceVerificationResponse result = faceVerificationService.verifyFace(idImage, selfie);

        // Then
        assertNotNull(result);
        assertTrue(result.getMatch());
        assertEquals(0.85, result.getSimilarity());
        assertEquals(0.35, result.getDistance());
        assertEquals(0.6, result.getThreshold());
        verify(restTemplate).exchange(
                eq("http://localhost:8000/verify-face"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(FaceVerificationResponse.class)
        );
    }

    @Test
    void verifyFace_WithNonMatchingFaces_ShouldReturnNoMatchResponse() throws Exception {
        // Given
        FaceVerificationResponse expectedResponse = new FaceVerificationResponse();
        expectedResponse.setMatch(false);
        expectedResponse.setSimilarity(0.45);
        expectedResponse.setDistance(0.75);
        expectedResponse.setThreshold(0.6);

        when(restTemplate.exchange(
                eq("http://localhost:8000/verify-face"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(FaceVerificationResponse.class)
        )).thenReturn(new ResponseEntity<>(expectedResponse, HttpStatus.OK));

        // When
        FaceVerificationResponse result = faceVerificationService.verifyFace(idImage, selfie);

        // Then
        assertNotNull(result);
        assertFalse(result.getMatch());
        assertEquals(0.45, result.getSimilarity());
    }

    @Test
    void verifyFace_WhenServiceThrowsException_ShouldThrowRuntimeException() {
        // Given
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(FaceVerificationResponse.class)
        )).thenThrow(new RestClientException("Service unavailable"));

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> faceVerificationService.verifyFace(idImage, selfie)
        );

        assertTrue(exception.getMessage().contains("Error verificando rostros"));
    }
}
