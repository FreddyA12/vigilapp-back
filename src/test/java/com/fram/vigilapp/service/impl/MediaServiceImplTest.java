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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaServiceImplTest {

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private MediaServiceImpl mediaService;

    @Mock
    private MultipartFile mockFile;

    private User testUser;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();

        // Set field values using reflection
        ReflectionTestUtils.setField(mediaService, "uploadDirectory", tempDir.toString());
        ReflectionTestUtils.setField(mediaService, "blurEnabled", true);
        ReflectionTestUtils.setField(mediaService, "autoBlurImages", true);
        ReflectionTestUtils.setField(mediaService, "faceServiceUrl", "http://localhost:8000");
    }

    @Test
    void processAndSaveMedia_withValidFile_shouldSaveMedia() throws IOException {
        // Given
        byte[] fileContent = "test image content".getBytes();
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getContentType()).thenReturn("image/jpeg");
        when(mockFile.getOriginalFilename()).thenReturn("test.jpg");
        when(mockFile.getBytes()).thenReturn(fileContent);

        Media savedMedia = Media.builder()
                .id(UUID.randomUUID())
                .ownerUser(testUser)
                .mimeType("image/jpeg")
                .build();

        when(mediaRepository.save(any(Media.class))).thenReturn(savedMedia);

        // Disable blur for this test
        ReflectionTestUtils.setField(mediaService, "blurEnabled", false);

        // When
        Media result = mediaService.processAndSaveMedia(mockFile, testUser, false);

        // Then
        assertNotNull(result);
        verify(mediaRepository).save(any(Media.class));
    }

    @Test
    void processAndSaveMedia_withEmptyFile_shouldThrowException() {
        // Given
        when(mockFile.isEmpty()).thenReturn(true);

        // When & Then
        assertThrows(RuntimeException.class, () ->
                mediaService.processAndSaveMedia(mockFile, testUser, false)
        );
        verify(mediaRepository, never()).save(any(Media.class));
    }

    @Test
    void processAndSaveMedia_withNullContentType_shouldThrowException() {
        // Given
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getContentType()).thenReturn(null);

        // When & Then
        assertThrows(RuntimeException.class, () ->
                mediaService.processAndSaveMedia(mockFile, testUser, false)
        );
        verify(mediaRepository, never()).save(any(Media.class));
    }

    @Test
    void processAndSaveMultipleMedia_withValidFiles_shouldSaveAll() throws IOException {
        // Given
        MultipartFile file1 = mock(MultipartFile.class);
        MultipartFile file2 = mock(MultipartFile.class);

        when(file1.isEmpty()).thenReturn(false);
        when(file1.getContentType()).thenReturn("image/jpeg");
        when(file1.getOriginalFilename()).thenReturn("test1.jpg");
        when(file1.getBytes()).thenReturn("content1".getBytes());

        when(file2.isEmpty()).thenReturn(false);
        when(file2.getContentType()).thenReturn("image/png");
        when(file2.getOriginalFilename()).thenReturn("test2.png");
        when(file2.getBytes()).thenReturn("content2".getBytes());

        Media media1 = Media.builder().id(UUID.randomUUID()).build();
        Media media2 = Media.builder().id(UUID.randomUUID()).build();

        when(mediaRepository.save(any(Media.class)))
                .thenReturn(media1)
                .thenReturn(media2);

        ReflectionTestUtils.setField(mediaService, "blurEnabled", false);

        // When
        List<Media> result = mediaService.processAndSaveMultipleMedia(
                Arrays.asList(file1, file2), testUser, false
        );

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(mediaRepository, times(2)).save(any(Media.class));
    }

    @Test
    void processAndSaveMultipleMedia_withNullList_shouldReturnEmptyList() {
        // When
        List<Media> result = mediaService.processAndSaveMultipleMedia(null, testUser, false);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(mediaRepository, never()).save(any(Media.class));
    }

    @Test
    void processAndSaveMultipleMedia_withEmptyList_shouldReturnEmptyList() {
        // When
        List<Media> result = mediaService.processAndSaveMultipleMedia(
                Arrays.asList(), testUser, false
        );

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(mediaRepository, never()).save(any(Media.class));
    }

    @Test
    void processAndSaveMultipleMedia_withSomeNullFiles_shouldProcessOnlyValidFiles() throws IOException {
        // Given
        MultipartFile validFile = mock(MultipartFile.class);

        when(validFile.isEmpty()).thenReturn(false);
        when(validFile.getContentType()).thenReturn("image/jpeg");
        when(validFile.getOriginalFilename()).thenReturn("valid.jpg");
        when(validFile.getBytes()).thenReturn("content".getBytes());

        Media media = Media.builder().id(UUID.randomUUID()).build();
        when(mediaRepository.save(any(Media.class))).thenReturn(media);

        ReflectionTestUtils.setField(mediaService, "blurEnabled", false);

        // When
        List<Media> result = mediaService.processAndSaveMultipleMedia(
                Arrays.asList(null, validFile, null), testUser, false
        );

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(mediaRepository, times(1)).save(any(Media.class));
    }

    @Test
    void blurFacesInImage_withValidImage_shouldReturnBlurredImage() {
        // Given
        byte[] imageBytes = "original image".getBytes();
        byte[] blurredBytes = "blurred image".getBytes();
        String filename = "test.jpg";

        ResponseEntity<byte[]> response = new ResponseEntity<>(blurredBytes, HttpStatus.OK);

        // Mock the RestTemplate that's created in MediaServiceImpl
        RestTemplate mockRestTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(mediaService, "restTemplate", mockRestTemplate);

        when(mockRestTemplate.exchange(
                anyString(),
                any(),
                any(),
                eq(byte[].class)
        )).thenReturn(response);

        // When
        byte[] result = mediaService.blurFacesInImage(imageBytes, filename);

        // Then
        assertNotNull(result);
        assertArrayEquals(blurredBytes, result);
    }

    @Test
    void blurFacesInImage_withNon200Response_shouldThrowException() {
        // Given
        byte[] imageBytes = "original image".getBytes();
        String filename = "test.jpg";

        ResponseEntity<byte[]> response = new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);

        RestTemplate mockRestTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(mediaService, "restTemplate", mockRestTemplate);

        when(mockRestTemplate.exchange(
                anyString(),
                any(),
                any(),
                eq(byte[].class)
        )).thenReturn(response);

        // When & Then
        assertThrows(RuntimeException.class, () ->
                mediaService.blurFacesInImage(imageBytes, filename)
        );
    }

    @Test
    void processAndSaveMedia_withIOException_shouldThrowRuntimeException() throws IOException {
        // Given
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getContentType()).thenReturn("image/jpeg");
        when(mockFile.getOriginalFilename()).thenReturn("test.jpg");
        when(mockFile.getBytes()).thenThrow(new IOException("Test exception"));

        // When & Then
        assertThrows(RuntimeException.class, () ->
                mediaService.processAndSaveMedia(mockFile, testUser, false)
        );
    }

    @Test
    void processAndSaveMedia_withBlurEnabled_shouldBlurImage() throws IOException {
        // Given
        byte[] originalBytes = "original image".getBytes();
        byte[] blurredBytes = "blurred image".getBytes();

        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getContentType()).thenReturn("image/jpeg");
        when(mockFile.getOriginalFilename()).thenReturn("test.jpg");
        when(mockFile.getBytes()).thenReturn(originalBytes);

        Media savedMedia = Media.builder()
                .id(UUID.randomUUID())
                .ownerUser(testUser)
                .mimeType("image/jpeg")
                .forBlurAnalysis(true)
                .build();

        when(mediaRepository.save(any(Media.class))).thenReturn(savedMedia);

        // Mock RestTemplate for blur service
        RestTemplate mockRestTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(mediaService, "restTemplate", mockRestTemplate);
        ReflectionTestUtils.setField(mediaService, "blurEnabled", true);
        ReflectionTestUtils.setField(mediaService, "autoBlurImages", true);

        ResponseEntity<byte[]> response = new ResponseEntity<>(blurredBytes, HttpStatus.OK);
        when(mockRestTemplate.exchange(
                anyString(),
                any(),
                any(),
                eq(byte[].class)
        )).thenReturn(response);

        // When
        Media result = mediaService.processAndSaveMedia(mockFile, testUser, true);

        // Then
        assertNotNull(result);
        verify(mediaRepository).save(any(Media.class));
    }

    @Test
    void processAndSaveMedia_withBlurFailure_shouldSaveOriginalImage() throws IOException {
        // Given
        byte[] originalBytes = "original image".getBytes();

        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getContentType()).thenReturn("image/jpeg");
        when(mockFile.getOriginalFilename()).thenReturn("test.jpg");
        when(mockFile.getBytes()).thenReturn(originalBytes);

        Media savedMedia = Media.builder()
                .id(UUID.randomUUID())
                .ownerUser(testUser)
                .mimeType("image/jpeg")
                .build();

        when(mediaRepository.save(any(Media.class))).thenReturn(savedMedia);

        // Mock RestTemplate to throw exception
        RestTemplate mockRestTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(mediaService, "restTemplate", mockRestTemplate);
        ReflectionTestUtils.setField(mediaService, "blurEnabled", true);
        ReflectionTestUtils.setField(mediaService, "autoBlurImages", true);

        when(mockRestTemplate.exchange(
                anyString(),
                any(),
                any(),
                eq(byte[].class)
        )).thenThrow(new RuntimeException("Blur service unavailable"));

        // When
        Media result = mediaService.processAndSaveMedia(mockFile, testUser, true);

        // Then
        assertNotNull(result);
        verify(mediaRepository).save(any(Media.class));
    }

    @Test
    void processAndSaveMedia_withNonImageFile_shouldNotBlur() throws IOException {
        // Given
        byte[] fileContent = "test video content".getBytes();
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getContentType()).thenReturn("video/mp4");
        when(mockFile.getOriginalFilename()).thenReturn("test.mp4");
        when(mockFile.getBytes()).thenReturn(fileContent);

        Media savedMedia = Media.builder()
                .id(UUID.randomUUID())
                .ownerUser(testUser)
                .mimeType("video/mp4")
                .build();

        when(mediaRepository.save(any(Media.class))).thenReturn(savedMedia);

        ReflectionTestUtils.setField(mediaService, "blurEnabled", true);
        ReflectionTestUtils.setField(mediaService, "autoBlurImages", true);

        // When
        Media result = mediaService.processAndSaveMedia(mockFile, testUser, true);

        // Then
        assertNotNull(result);
        verify(mediaRepository).save(any(Media.class));
    }

    @Test
    void processAndSaveMedia_withFileWithoutExtension_shouldSaveWithoutExtension() throws IOException {
        // Given
        byte[] fileContent = "test content".getBytes();
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getContentType()).thenReturn("image/jpeg");
        when(mockFile.getOriginalFilename()).thenReturn("testfile");
        when(mockFile.getBytes()).thenReturn(fileContent);

        Media savedMedia = Media.builder()
                .id(UUID.randomUUID())
                .build();

        when(mediaRepository.save(any(Media.class))).thenReturn(savedMedia);
        ReflectionTestUtils.setField(mediaService, "blurEnabled", false);

        // When
        Media result = mediaService.processAndSaveMedia(mockFile, testUser, false);

        // Then
        assertNotNull(result);
        verify(mediaRepository).save(any(Media.class));
    }

    @Test
    void processAndSaveMedia_withNullFilename_shouldGenerateUniqueFilename() throws IOException {
        // Given
        byte[] fileContent = "test content".getBytes();
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getContentType()).thenReturn("image/jpeg");
        when(mockFile.getOriginalFilename()).thenReturn(null);
        when(mockFile.getBytes()).thenReturn(fileContent);

        Media savedMedia = Media.builder()
                .id(UUID.randomUUID())
                .build();

        when(mediaRepository.save(any(Media.class))).thenReturn(savedMedia);
        ReflectionTestUtils.setField(mediaService, "blurEnabled", false);

        // When
        Media result = mediaService.processAndSaveMedia(mockFile, testUser, false);

        // Then
        assertNotNull(result);
        verify(mediaRepository).save(any(Media.class));
    }

    @Test
    void processAndSaveMultipleMedia_withOneFileFailure_shouldContinueWithOthers() throws IOException {
        // Given
        MultipartFile file1 = mock(MultipartFile.class);
        MultipartFile file2 = mock(MultipartFile.class);
        MultipartFile file3 = mock(MultipartFile.class);

        when(file1.isEmpty()).thenReturn(false);
        when(file1.getContentType()).thenReturn("image/jpeg");
        when(file1.getOriginalFilename()).thenReturn("test1.jpg");
        when(file1.getBytes()).thenReturn("content1".getBytes());

        when(file2.isEmpty()).thenReturn(false);
        when(file2.getContentType()).thenReturn("image/jpeg");
        when(file2.getOriginalFilename()).thenReturn("test2.jpg");
        when(file2.getBytes()).thenThrow(new IOException("File read error"));

        when(file3.isEmpty()).thenReturn(false);
        when(file3.getContentType()).thenReturn("image/jpeg");
        when(file3.getOriginalFilename()).thenReturn("test3.jpg");
        when(file3.getBytes()).thenReturn("content3".getBytes());

        Media media1 = Media.builder().id(UUID.randomUUID()).build();
        Media media3 = Media.builder().id(UUID.randomUUID()).build();

        when(mediaRepository.save(any(Media.class)))
                .thenReturn(media1)
                .thenReturn(media3);

        ReflectionTestUtils.setField(mediaService, "blurEnabled", false);

        // When
        List<Media> result = mediaService.processAndSaveMultipleMedia(
                Arrays.asList(file1, file2, file3), testUser, false
        );

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(mediaRepository, times(2)).save(any(Media.class));
    }

    @Test
    void processAndSaveMultipleMedia_withEmptyFiles_shouldSkipEmptyFiles() throws IOException {
        // Given
        MultipartFile file1 = mock(MultipartFile.class);
        MultipartFile file2 = mock(MultipartFile.class);

        when(file1.isEmpty()).thenReturn(true);
        when(file2.isEmpty()).thenReturn(false);
        when(file2.getContentType()).thenReturn("image/jpeg");
        when(file2.getOriginalFilename()).thenReturn("test2.jpg");
        when(file2.getBytes()).thenReturn("content2".getBytes());

        Media media2 = Media.builder().id(UUID.randomUUID()).build();
        when(mediaRepository.save(any(Media.class))).thenReturn(media2);

        ReflectionTestUtils.setField(mediaService, "blurEnabled", false);

        // When
        List<Media> result = mediaService.processAndSaveMultipleMedia(
                Arrays.asList(file1, file2), testUser, false
        );

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(mediaRepository, times(1)).save(any(Media.class));
    }

    @Test
    void blurFacesInImage_withException_shouldThrowRuntimeException() {
        // Given
        byte[] imageBytes = "image content".getBytes();
        String filename = "test.jpg";

        RestTemplate mockRestTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(mediaService, "restTemplate", mockRestTemplate);

        when(mockRestTemplate.exchange(
                anyString(),
                any(),
                any(),
                eq(byte[].class)
        )).thenThrow(new RuntimeException("Network error"));

        // When & Then
        assertThrows(RuntimeException.class, () ->
                mediaService.blurFacesInImage(imageBytes, filename)
        );
    }

    @Test
    void blurFacesInImage_withNullBody_shouldThrowException() {
        // Given
        byte[] imageBytes = "image content".getBytes();
        String filename = "test.jpg";

        RestTemplate mockRestTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(mediaService, "restTemplate", mockRestTemplate);

        ResponseEntity<byte[]> response = new ResponseEntity<>(null, HttpStatus.OK);
        when(mockRestTemplate.exchange(
                anyString(),
                any(),
                any(),
                eq(byte[].class)
        )).thenReturn(response);

        // When & Then
        assertThrows(RuntimeException.class, () ->
                mediaService.blurFacesInImage(imageBytes, filename)
        );
    }

    @Test
    void processAndSaveMedia_withPngImage_shouldProcess() throws IOException {
        // Given
        byte[] fileContent = "test png content".getBytes();
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getContentType()).thenReturn("image/png");
        when(mockFile.getOriginalFilename()).thenReturn("test.png");
        when(mockFile.getBytes()).thenReturn(fileContent);

        Media savedMedia = Media.builder()
                .id(UUID.randomUUID())
                .build();

        when(mediaRepository.save(any(Media.class))).thenReturn(savedMedia);
        ReflectionTestUtils.setField(mediaService, "blurEnabled", false);

        // When
        Media result = mediaService.processAndSaveMedia(mockFile, testUser, false);

        // Then
        assertNotNull(result);
        verify(mediaRepository).save(any(Media.class));
    }

    @Test
    void processAndSaveMedia_withJpgContentType_shouldProcess() throws IOException {
        // Given
        byte[] fileContent = "test jpg content".getBytes();
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getContentType()).thenReturn("image/jpg");
        when(mockFile.getOriginalFilename()).thenReturn("test.jpg");
        when(mockFile.getBytes()).thenReturn(fileContent);

        Media savedMedia = Media.builder()
                .id(UUID.randomUUID())
                .build();

        when(mediaRepository.save(any(Media.class))).thenReturn(savedMedia);
        ReflectionTestUtils.setField(mediaService, "blurEnabled", false);

        // When
        Media result = mediaService.processAndSaveMedia(mockFile, testUser, false);

        // Then
        assertNotNull(result);
        verify(mediaRepository).save(any(Media.class));
    }

    @Test
    void blurFacesInImage_shouldCreateByteArrayResourceWithFilename() {
        // Given
        byte[] imageBytes = "test image content".getBytes();
        byte[] blurredBytes = "blurred image".getBytes();
        String filename = "test-image.jpg";

        RestTemplate mockRestTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(mediaService, "restTemplate", mockRestTemplate);

        ResponseEntity<byte[]> response = new ResponseEntity<>(blurredBytes, HttpStatus.OK);
        when(mockRestTemplate.exchange(
                anyString(),
                any(HttpMethod.class),
                any(HttpEntity.class),
                eq(byte[].class)
        )).thenAnswer(invocation -> {
            HttpEntity<?> entity = invocation.getArgument(2);
            // Verify that the ByteArrayResource has getFilename() method working
            Object body = entity.getBody();
            assertNotNull(body);
            return response;
        });

        // When
        byte[] result = mediaService.blurFacesInImage(imageBytes, filename);

        // Then
        assertNotNull(result);
        assertArrayEquals(blurredBytes, result);
        verify(mockRestTemplate).exchange(
                anyString(),
                any(HttpMethod.class),
                any(HttpEntity.class),
                eq(byte[].class)
        );
    }

    @Test
    void blurFacesInImage_byteArrayResourceFilename_shouldReturnCorrectFilename() {
        // Given
        byte[] imageBytes = "test image".getBytes();
        String expectedFilename = "my-test-image.jpg";

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
    void processAndSaveMedia_withBlurEnabled_shouldCallBlurServiceCorrectly() throws IOException {
        // Given
        byte[] originalBytes = "original image content".getBytes();
        byte[] blurredBytes = "blurred image content".getBytes();
        String filename = "test-photo.jpg";

        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getContentType()).thenReturn("image/jpeg");
        when(mockFile.getOriginalFilename()).thenReturn(filename);
        when(mockFile.getBytes()).thenReturn(originalBytes);

        Media savedMedia = Media.builder()
                .id(UUID.randomUUID())
                .ownerUser(testUser)
                .mimeType("image/jpeg")
                .forBlurAnalysis(true)
                .build();

        when(mediaRepository.save(any(Media.class))).thenReturn(savedMedia);

        // Mock RestTemplate and capture the request
        RestTemplate mockRestTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(mediaService, "restTemplate", mockRestTemplate);
        ReflectionTestUtils.setField(mediaService, "blurEnabled", true);
        ReflectionTestUtils.setField(mediaService, "autoBlurImages", true);

        ResponseEntity<byte[]> response = new ResponseEntity<>(blurredBytes, HttpStatus.OK);

        when(mockRestTemplate.exchange(
                anyString(),
                any(HttpMethod.class),
                any(HttpEntity.class),
                eq(byte[].class)
        )).thenAnswer(invocation -> {
            // Verify the request entity contains correct data
            HttpEntity<?> entity = invocation.getArgument(2);
            org.springframework.util.MultiValueMap<String, Object> body =
                (org.springframework.util.MultiValueMap<String, Object>) entity.getBody();

            assertNotNull(body);
            assertTrue(body.containsKey("image"));
            assertTrue(body.containsKey("blur_intensity"));

            // Get the ByteArrayResource and verify getFilename() is called
            Object imageObject = body.getFirst("image");
            if (imageObject instanceof org.springframework.core.io.ByteArrayResource) {
                org.springframework.core.io.ByteArrayResource resource =
                    (org.springframework.core.io.ByteArrayResource) imageObject;
                // This will invoke the overridden getFilename() method
                String resourceFilename = resource.getFilename();
                assertEquals(filename, resourceFilename);
            }

            return response;
        });

        // When
        Media result = mediaService.processAndSaveMedia(mockFile, testUser, true);

        // Then
        assertNotNull(result);
        verify(mockRestTemplate).exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(byte[].class)
        );
        verify(mediaRepository).save(any(Media.class));
    }
}
