package com.fram.vigilapp.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fram.vigilapp.dto.FaceVerificationResponse;
import com.fram.vigilapp.dto.IdValidationResponse;
import com.fram.vigilapp.service.FaceVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class FaceVerificationServiceImpl implements FaceVerificationService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    @Value("${face.verification.service.url:http://localhost:8000}")
    private String faceVerificationServiceUrl;

    @Override
    public IdValidationResponse validateIdDocument(MultipartFile image) {
        try {
            String url = faceVerificationServiceUrl + "/validate-id";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("image", new ByteArrayResource(image.getBytes()) {
                @Override
                public String getFilename() {
                    return image.getOriginalFilename();
                }
            });

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<IdValidationResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    IdValidationResponse.class
            );

            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Error validando documento de identidad: " + e.getMessage(), e);
        }
    }

    @Override
    public FaceVerificationResponse verifyFace(MultipartFile idImage, MultipartFile selfie) {
        try {
            String url = faceVerificationServiceUrl + "/verify-face";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("id_image", new ByteArrayResource(idImage.getBytes()) {
                @Override
                public String getFilename() {
                    return idImage.getOriginalFilename();
                }
            });
            body.add("selfie", new ByteArrayResource(selfie.getBytes()) {
                @Override
                public String getFilename() {
                    return selfie.getOriginalFilename();
                }
            });

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<FaceVerificationResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    FaceVerificationResponse.class
            );

            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Error verificando rostros: " + e.getMessage(), e);
        }
    }
}
