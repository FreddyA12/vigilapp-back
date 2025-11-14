package com.fram.vigilapp.service.impl;

import com.fram.vigilapp.entity.Media;
import com.fram.vigilapp.entity.User;
import com.fram.vigilapp.repository.MediaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaServiceImplTest {

    @Mock
    private MediaRepository mediaRepository;

    @InjectMocks
    private MediaServiceImpl mediaService;

    @TempDir
    Path tempDir;

    private RestTemplate restTemplate;
    private User testUser;
    private MockMultipartFile imageFile;
    private MockMultipartFile videoFile;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(mediaService, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(mediaService, "uploadDirectory", tempDir.toString());
        ReflectionTestUtils.setField(mediaService, "blurEnabled", true);
        ReflectionTestUtils.setField(mediaService, "autoBlurImages", true);
        ReflectionTestUtils.setField(mediaService, "faceServiceUrl", "http://localhost:8000");

        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .build();

        imageFile = new MockMultipartFile(
                "image",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        videoFile = new MockMultipartFile(
                "video",
                "test.mp4",
                "video/mp4",
                "test video content".getBytes()
        );
    }

    @Test
    void processAndSaveMedia_WithImage_ShouldSaveMedia() {
        // Given
        Media savedMedia = Media.builder()
                .id(UUID.randomUUID())
                .ownerUser(testUser)
                .url("/uploads/test.jpg")
                .mimeType("image/jpeg")
                .forBlurAnalysis(false)
                .build();

        when(mediaRepository.save(any(Media.class))).thenReturn(savedMedia);

        // Mock blur service to avoid actual HTTP call
        byte[] blurredImage = "blurred image content".getBytes();
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(byte[].class)
        )).thenReturn(new ResponseEntity<>(blurredImage, HttpStatus.OK));

        // When
        Media result = mediaService.processAndSaveMedia(imageFile, testUser, true);

        // Then
        assertNotNull(result);
        assertEquals(testUser, result.getOwnerUser());
        verify(mediaRepository).save(any(Media.class));
    }

    @Test
    void processAndSaveMedia_WithEmptyFile_ShouldThrowException() {
        // Given
        MockMultipartFile emptyFile = new MockMultipartFile("empty", new byte[0]);

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> mediaService.processAndSaveMedia(emptyFile, testUser, false)
        );

        assertTrue(exception.getMessage().contains("El archivo está vacío"));
        verify(mediaRepository, never()).save(any());
    }

    @Test
    void processAndSaveMedia_WithNullContentType_ShouldThrowException() {
        // Given
        MockMultipartFile fileWithoutType = new MockMultipartFile(
                "file",
                "test.txt",
                null,
                "content".getBytes()
        );

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> mediaService.processAndSaveMedia(fileWithoutType, testUser, false)
        );

        assertTrue(exception.getMessage().contains("Tipo de archivo desconocido"));
        verify(mediaRepository, never()).save(any());
    }

    @Test
    void processAndSaveMedia_WithVideo_ShouldNotBlur() {
        // Given
        Media savedMedia = Media.builder()
                .id(UUID.randomUUID())
                .ownerUser(testUser)
                .url("/uploads/test.mp4")
                .mimeType("video/mp4")
                .forBlurAnalysis(false)
                .build();

        when(mediaRepository.save(any(Media.class))).thenReturn(savedMedia);

        // When
        Media result = mediaService.processAndSaveMedia(videoFile, testUser, true);

        // Then
        assertNotNull(result);
        assertEquals("video/mp4", result.getMimeType());
        verify(mediaRepository).save(any(Media.class));
        verify(restTemplate, never()).exchange(anyString(), any(), any(), any(Class.class));
    }

    @Test
    void processAndSaveMultipleMedia_WithMultipleFiles_ShouldProcessAll() {
        // Given
        List<MultipartFile> files = Arrays.asList(imageFile, videoFile);

        Media imageMedia = Media.builder()
                .id(UUID.randomUUID())
                .mimeType("image/jpeg")
                .build();

        Media videoMedia = Media.builder()
                .id(UUID.randomUUID())
                .mimeType("video/mp4")
                .build();

        when(mediaRepository.save(any(Media.class)))
                .thenReturn(imageMedia)
                .thenReturn(videoMedia);

        // Mock blur service
        byte[] blurredImage = "blurred image content".getBytes();
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(byte[].class)
        )).thenReturn(new ResponseEntity<>(blurredImage, HttpStatus.OK));

        // When
        List<Media> result = mediaService.processAndSaveMultipleMedia(files, testUser, true);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(mediaRepository, times(2)).save(any(Media.class));
    }

    @Test
    void processAndSaveMultipleMedia_WithEmptyList_ShouldReturnEmptyList() {
        // When
        List<Media> result = mediaService.processAndSaveMultipleMedia(null, testUser, false);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(mediaRepository, never()).save(any());
    }

    @Test
    void blurFacesInImage_WithValidImage_ShouldReturnBlurredBytes() {
        // Given
        byte[] originalBytes = "original image".getBytes();
        byte[] blurredBytes = "blurred image".getBytes();

        when(restTemplate.exchange(
                eq("http://localhost:8000/blur-faces"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(byte[].class)
        )).thenReturn(new ResponseEntity<>(blurredBytes, HttpStatus.OK));

        // When
        byte[] result = mediaService.blurFacesInImage(originalBytes, "test.jpg");

        // Then
        assertNotNull(result);
        assertArrayEquals(blurredBytes, result);
        verify(restTemplate).exchange(
                eq("http://localhost:8000/blur-faces"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(byte[].class)
        );
    }

    @Test
    void blurFacesInImage_WhenServiceFails_ShouldThrowException() {
        // Given
        byte[] originalBytes = "original image".getBytes();

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(byte[].class)
        )).thenReturn(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> mediaService.blurFacesInImage(originalBytes, "test.jpg")
        );

        assertTrue(exception.getMessage().contains("Error al difuminar caras"));
    }
}
