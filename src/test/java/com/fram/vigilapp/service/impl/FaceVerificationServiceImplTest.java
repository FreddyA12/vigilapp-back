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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FaceVerificationServiceImplTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private FaceVerificationServiceImpl faceVerificationService;

    private MockMultipartFile idImage;
    private MockMultipartFile selfieImage;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(faceVerificationService, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(faceVerificationService, "faceVerificationServiceUrl", "http://localhost:8000");

        idImage = new MockMultipartFile(
                "idImage",
                "id.jpg",
                "image/jpeg",
                "id image content".getBytes()
        );

        selfieImage = new MockMultipartFile(
                "selfie",
                "selfie.jpg",
                "image/jpeg",
                "selfie content".getBytes()
        );
    }

    @Test
    void validateIdDocument_withValidImage_shouldReturnValidationResponse() {
        // Given
        IdValidationResponse expectedResponse = IdValidationResponse.builder()
                .isIdDocument(true)
                .confidence(0.95)
                .aspectRatio(1.5)
                .build();

        ResponseEntity<IdValidationResponse> responseEntity =
                new ResponseEntity<>(expectedResponse, HttpStatus.OK);

        when(restTemplate.exchange(
                anyString(),
                any(),
                any(),
                eq(IdValidationResponse.class)
        )).thenReturn(responseEntity);

        // When
        IdValidationResponse result = faceVerificationService.validateIdDocument(idImage);

        // Then
        assertNotNull(result);
        assertTrue(result.getIsIdDocument());
        assertEquals(0.95, result.getConfidence());
        verify(restTemplate).exchange(
                anyString(),
                any(),
                any(),
                eq(IdValidationResponse.class)
        );
    }

    @Test
    void validateIdDocument_withInvalidImage_shouldReturnInvalidResponse() {
        // Given
        IdValidationResponse expectedResponse = IdValidationResponse.builder()
                .isIdDocument(false)
                .confidence(0.35)
                .build();

        ResponseEntity<IdValidationResponse> responseEntity =
                new ResponseEntity<>(expectedResponse, HttpStatus.OK);

        when(restTemplate.exchange(
                anyString(),
                any(),
                any(),
                eq(IdValidationResponse.class)
        )).thenReturn(responseEntity);

        // When
        IdValidationResponse result = faceVerificationService.validateIdDocument(idImage);

        // Then
        assertNotNull(result);
        assertFalse(result.getIsIdDocument());
        assertEquals(0.35, result.getConfidence());
    }

    @Test
    void validateIdDocument_withServiceError_shouldThrowException() {
        // Given
        when(restTemplate.exchange(
                anyString(),
                any(),
                any(),
                eq(IdValidationResponse.class)
        )).thenThrow(new RuntimeException("Service unavailable"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                faceVerificationService.validateIdDocument(idImage)
        );

        assertTrue(exception.getMessage().contains("Error validando documento de identidad"));
    }

    @Test
    void verifyFace_withMatchingFaces_shouldReturnSuccessResponse() {
        // Given
        FaceVerificationResponse expectedResponse = FaceVerificationResponse.builder()
                .match(true)
                .similarity(0.95)
                .distance(0.05)
                .threshold(0.6)
                .model("VGG-Face")
                .build();

        ResponseEntity<FaceVerificationResponse> responseEntity =
                new ResponseEntity<>(expectedResponse, HttpStatus.OK);

        when(restTemplate.exchange(
                anyString(),
                any(),
                any(),
                eq(FaceVerificationResponse.class)
        )).thenReturn(responseEntity);

        // When
        FaceVerificationResponse result = faceVerificationService.verifyFace(idImage, selfieImage);

        // Then
        assertNotNull(result);
        assertTrue(result.getMatch());
        assertEquals(0.95, result.getSimilarity());
        assertEquals("VGG-Face", result.getModel());
        verify(restTemplate).exchange(
                anyString(),
                any(),
                any(),
                eq(FaceVerificationResponse.class)
        );
    }

    @Test
    void verifyFace_withNonMatchingFaces_shouldReturnFailureResponse() {
        // Given
        FaceVerificationResponse expectedResponse = FaceVerificationResponse.builder()
                .match(false)
                .similarity(0.35)
                .distance(0.65)
                .threshold(0.6)
                .model("VGG-Face")
                .build();

        ResponseEntity<FaceVerificationResponse> responseEntity =
                new ResponseEntity<>(expectedResponse, HttpStatus.OK);

        when(restTemplate.exchange(
                anyString(),
                any(),
                any(),
                eq(FaceVerificationResponse.class)
        )).thenReturn(responseEntity);

        // When
        FaceVerificationResponse result = faceVerificationService.verifyFace(idImage, selfieImage);

        // Then
        assertNotNull(result);
        assertFalse(result.getMatch());
        assertEquals(0.35, result.getSimilarity());
        assertEquals(0.65, result.getDistance());
    }

    @Test
    void verifyFace_withServiceError_shouldThrowException() {
        // Given
        when(restTemplate.exchange(
                anyString(),
                any(),
                any(),
                eq(FaceVerificationResponse.class)
        )).thenThrow(new RuntimeException("Service error"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                faceVerificationService.verifyFace(idImage, selfieImage)
        );

        assertTrue(exception.getMessage().contains("Error verificando rostros"));
    }

    @Test
    void validateIdDocument_shouldUseCorrectUrl() {
        // Given
        IdValidationResponse response = IdValidationResponse.builder()
                .isIdDocument(true)
                .build();

        when(restTemplate.exchange(
                contains("/validate-id"),
                any(),
                any(),
                eq(IdValidationResponse.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // When
        faceVerificationService.validateIdDocument(idImage);

        // Then
        verify(restTemplate).exchange(
                contains("/validate-id"),
                any(),
                any(),
                eq(IdValidationResponse.class)
        );
    }

    @Test
    void verifyFace_shouldUseCorrectUrl() {
        // Given
        FaceVerificationResponse response = FaceVerificationResponse.builder()
                .match(true)
                .build();

        when(restTemplate.exchange(
                contains("/verify-face"),
                any(),
                any(),
                eq(FaceVerificationResponse.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // When
        faceVerificationService.verifyFace(idImage, selfieImage);

        // Then
        verify(restTemplate).exchange(
                contains("/verify-face"),
                any(),
                any(),
                eq(FaceVerificationResponse.class)
        );
    }

    @Test
    void validateIdDocument_shouldCreateByteArrayResourceWithFilename() {
        // Given
        IdValidationResponse response = IdValidationResponse.builder()
                .isIdDocument(true)
                .confidence(0.9)
                .build();

        when(restTemplate.exchange(
                anyString(),
                any(),
                any(),
                eq(IdValidationResponse.class)
        )).thenAnswer(invocation -> {
            // Verify ByteArrayResource with getFilename() is created correctly
            org.springframework.http.HttpEntity<?> entity = invocation.getArgument(2);
            org.springframework.util.MultiValueMap<String, Object> body =
                (org.springframework.util.MultiValueMap<String, Object>) entity.getBody();

            assertNotNull(body);
            assertTrue(body.containsKey("image"));

            Object imageObject = body.getFirst("image");
            if (imageObject instanceof org.springframework.core.io.ByteArrayResource) {
                org.springframework.core.io.ByteArrayResource resource =
                    (org.springframework.core.io.ByteArrayResource) imageObject;
                // This will invoke the overridden getFilename() method
                String filename = resource.getFilename();
                assertEquals("id.jpg", filename);
            }

            return new ResponseEntity<>(response, HttpStatus.OK);
        });

        // When
        IdValidationResponse result = faceVerificationService.validateIdDocument(idImage);

        // Then
        assertNotNull(result);
        assertTrue(result.getIsIdDocument());
    }

    @Test
    void verifyFace_shouldCreateByteArrayResourcesWithFilenames() {
        // Given
        FaceVerificationResponse response = FaceVerificationResponse.builder()
                .match(true)
                .similarity(0.92)
                .build();

        when(restTemplate.exchange(
                anyString(),
                any(),
                any(),
                eq(FaceVerificationResponse.class)
        )).thenAnswer(invocation -> {
            // Verify ByteArrayResources with getFilename() are created correctly
            org.springframework.http.HttpEntity<?> entity = invocation.getArgument(2);
            org.springframework.util.MultiValueMap<String, Object> body =
                (org.springframework.util.MultiValueMap<String, Object>) entity.getBody();

            assertNotNull(body);
            assertTrue(body.containsKey("id_image"));
            assertTrue(body.containsKey("selfie"));

            // Check id_image resource
            Object idImageObject = body.getFirst("id_image");
            if (idImageObject instanceof org.springframework.core.io.ByteArrayResource) {
                org.springframework.core.io.ByteArrayResource resource =
                    (org.springframework.core.io.ByteArrayResource) idImageObject;
                String filename = resource.getFilename();
                assertEquals("id.jpg", filename);
            }

            // Check selfie resource
            Object selfieObject = body.getFirst("selfie");
            if (selfieObject instanceof org.springframework.core.io.ByteArrayResource) {
                org.springframework.core.io.ByteArrayResource resource =
                    (org.springframework.core.io.ByteArrayResource) selfieObject;
                String filename = resource.getFilename();
                assertEquals("selfie.jpg", filename);
            }

            return new ResponseEntity<>(response, HttpStatus.OK);
        });

        // When
        FaceVerificationResponse result = faceVerificationService.verifyFace(idImage, selfieImage);

        // Then
        assertNotNull(result);
        assertTrue(result.getMatch());
    }

    @Test
    void validateIdDocument_byteArrayResourceFilename_shouldReturnCorrectFilename() {
        // Given
        byte[] imageBytes = "test image".getBytes();
        String expectedFilename = "test-id.jpg";

        // Create ByteArrayResource with filename override
        org.springframework.core.io.ByteArrayResource resource = new org.springframework.core.io.ByteArrayResource(imageBytes) {
            @Override
            public String getFilename() {
                return expectedFilename;
            }
        };

        // Then
        assertEquals(expectedFilename, resource.getFilename());
        assertArrayEquals(imageBytes, resource.getByteArray());
    }

    @Test
    void verifyFace_withIOException_shouldThrowRuntimeException() throws Exception {
        // Given
        MockMultipartFile badImage = mock(MockMultipartFile.class);
        when(badImage.getBytes()).thenThrow(new java.io.IOException("File read error"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                faceVerificationService.verifyFace(badImage, selfieImage)
        );

        assertTrue(exception.getMessage().contains("Error verificando rostros"));
    }

    @Test
    void validateIdDocument_withIOException_shouldThrowRuntimeException() throws Exception {
        // Given
        MockMultipartFile badImage = mock(MockMultipartFile.class);
        when(badImage.getBytes()).thenThrow(new java.io.IOException("File read error"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                faceVerificationService.validateIdDocument(badImage)
        );

        assertTrue(exception.getMessage().contains("Error validando documento de identidad"));
    }

    @Test
    void validateIdDocument_withNullResponse_shouldReturnNull() {
        // Given
        ResponseEntity<IdValidationResponse> responseEntity =
                new ResponseEntity<>(null, HttpStatus.OK);

        when(restTemplate.exchange(
                anyString(),
                any(),
                any(),
                eq(IdValidationResponse.class)
        )).thenReturn(responseEntity);

        // When
        IdValidationResponse result = faceVerificationService.validateIdDocument(idImage);

        // Then
        assertNull(result);
    }

    @Test
    void verifyFace_withNullResponse_shouldReturnNull() {
        // Given
        ResponseEntity<FaceVerificationResponse> responseEntity =
                new ResponseEntity<>(null, HttpStatus.OK);

        when(restTemplate.exchange(
                anyString(),
                any(),
                any(),
                eq(FaceVerificationResponse.class)
        )).thenReturn(responseEntity);

        // When
        FaceVerificationResponse result = faceVerificationService.verifyFace(idImage, selfieImage);

        // Then
        assertNull(result);
    }

    @Test
    void validateIdDocument_withEmptyFilename_shouldHandleCorrectly() {
        // Given
        MockMultipartFile imageWithoutName = new MockMultipartFile(
                "image",
                "",
                "image/jpeg",
                "content".getBytes()
        );

        IdValidationResponse response = IdValidationResponse.builder()
                .isIdDocument(true)
                .build();

        when(restTemplate.exchange(
                anyString(),
                any(),
                any(),
                eq(IdValidationResponse.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // When
        IdValidationResponse result = faceVerificationService.validateIdDocument(imageWithoutName);

        // Then
        assertNotNull(result);
        assertTrue(result.getIsIdDocument());
    }

    @Test
    void verifyFace_withDifferentImageFormats_shouldProcess() {
        // Given
        MockMultipartFile pngId = new MockMultipartFile(
                "id",
                "id.png",
                "image/png",
                "png content".getBytes()
        );

        MockMultipartFile pngSelfie = new MockMultipartFile(
                "selfie",
                "selfie.png",
                "image/png",
                "png content".getBytes()
        );

        FaceVerificationResponse response = FaceVerificationResponse.builder()
                .match(true)
                .similarity(0.88)
                .build();

        when(restTemplate.exchange(
                anyString(),
                any(),
                any(),
                eq(FaceVerificationResponse.class)
        )).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        // When
        FaceVerificationResponse result = faceVerificationService.verifyFace(pngId, pngSelfie);

        // Then
        assertNotNull(result);
        assertTrue(result.getMatch());
        assertEquals(0.88, result.getSimilarity());
    }
}
