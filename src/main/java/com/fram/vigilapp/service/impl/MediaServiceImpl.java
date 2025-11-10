package com.fram.vigilapp.service.impl;

import com.fram.vigilapp.entity.Media;
import com.fram.vigilapp.entity.User;
import com.fram.vigilapp.repository.MediaRepository;
import com.fram.vigilapp.service.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaServiceImpl implements MediaService {

    private final MediaRepository mediaRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${media.upload.directory}")
    private String uploadDirectory;

    @Value("${media.blur.enabled:true}")
    private boolean blurEnabled;

    @Value("${media.blur.auto-blur-images:true}")
    private boolean autoBlurImages;

    @Value("${face.verification.service.url:http://localhost:8000}")
    private String faceServiceUrl;

    @Override
    public Media processAndSaveMedia(MultipartFile file, User user, boolean forBlurAnalysis) {
        try {
            // Validar archivo
            if (file.isEmpty()) {
                throw new RuntimeException("El archivo está vacío");
            }

            // Validar tipo de archivo
            String contentType = file.getContentType();
            if (contentType == null) {
                throw new RuntimeException("Tipo de archivo desconocido");
            }

            // Crear directorio de uploads si no existe
            Path uploadPath = Paths.get(uploadDirectory);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generar nombre único para el archivo
            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : "";
            String uniqueFilename = UUID.randomUUID().toString() + fileExtension;

            // Procesar imagen con blur si es necesario
            byte[] fileBytes = file.getBytes();
            boolean wasBlurred = false;

            if (forBlurAnalysis && blurEnabled && autoBlurImages && isImage(contentType)) {
                try {
                    byte[] blurredBytes = blurFacesInImage(fileBytes, originalFilename);
                    fileBytes = blurredBytes;
                    wasBlurred = true;
                    System.out.println("Imagen procesada con blur de caras: " + originalFilename);
                } catch (Exception e) {
                    System.err.println("Error al difuminar caras, guardando imagen original: " + e.getMessage());
                    // Si falla el blur, continuamos con la imagen original
                }
            }

            // Guardar archivo en disco
            Path filePath = uploadPath.resolve(uniqueFilename);
            Files.write(filePath, fileBytes);

            // Crear URL relativa para acceso
            String fileUrl = "/uploads/" + uniqueFilename;

            // Guardar registro en base de datos
            Media media = Media.builder()
                    .ownerUser(user)
                    .url(fileUrl)
                    .mimeType(contentType)
                    .forBlurAnalysis(forBlurAnalysis && wasBlurred)
                    .build();

            return mediaRepository.save(media);

        } catch (IOException e) {
            throw new RuntimeException("Error al procesar el archivo: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Media> processAndSaveMultipleMedia(List<MultipartFile> files, User user, boolean forBlurAnalysis) {
        List<Media> mediaList = new ArrayList<>();

        if (files == null || files.isEmpty()) {
            return mediaList;
        }

        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                try {
                    Media media = processAndSaveMedia(file, user, forBlurAnalysis);
                    mediaList.add(media);
                } catch (Exception e) {
                    System.err.println("Error al procesar archivo " + file.getOriginalFilename() + ": " + e.getMessage());
                    // Continuamos con los demás archivos
                }
            }
        }

        return mediaList;
    }

    @Override
    public byte[] blurFacesInImage(byte[] imageBytes, String filename) {
        try {
            // Preparar request multipart para el servicio de Python
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            // Crear ByteArrayResource con nombre de archivo
            ByteArrayResource fileResource = new ByteArrayResource(imageBytes) {
                @Override
                public String getFilename() {
                    return filename;
                }
            };

            body.add("image", fileResource);
            body.add("blur_intensity", 99);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // Llamar al servicio de Python
            String blurUrl = faceServiceUrl + "/blur-faces";
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    blurUrl,
                    HttpMethod.POST,
                    requestEntity,
                    byte[].class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            } else {
                throw new RuntimeException("El servicio de blur retornó un estado no exitoso: " + response.getStatusCode());
            }

        } catch (Exception e) {
            throw new RuntimeException("Error al difuminar caras en la imagen: " + e.getMessage(), e);
        }
    }

    private boolean isImage(String contentType) {
        return contentType != null && (
                contentType.equals("image/jpeg") ||
                contentType.equals("image/jpg") ||
                contentType.equals("image/png")
        );
    }
}
