package com.fram.vigilapp.service;

import com.fram.vigilapp.dto.FaceVerificationResponse;
import com.fram.vigilapp.dto.IdValidationResponse;
import org.springframework.web.multipart.MultipartFile;

public interface FaceVerificationService {

    /**
     * Valida que la imagen sea una cédula válida
     */
    IdValidationResponse validateIdDocument(MultipartFile image);

    /**
     * Compara el rostro de la cédula con el selfie
     */
    FaceVerificationResponse verifyFace(MultipartFile idImage, MultipartFile selfie);
}
