package com.fram.vigilapp.service.impl;

import com.fram.vigilapp.config.auth.AuthenticationRequest;
import com.fram.vigilapp.dto.FaceVerificationResponse;
import com.fram.vigilapp.dto.IdValidationResponse;
import com.fram.vigilapp.dto.SaveUserDto;
import com.fram.vigilapp.dto.UserDto;
import com.fram.vigilapp.entity.IdentityVerification;
import com.fram.vigilapp.entity.Media;
import com.fram.vigilapp.entity.User;
import com.fram.vigilapp.repository.IdentityVerificationRepository;
import com.fram.vigilapp.repository.MediaRepository;
import com.fram.vigilapp.repository.UserRepository;
import com.fram.vigilapp.service.AuthService;
import com.fram.vigilapp.service.FaceVerificationService;
import com.fram.vigilapp.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final FaceVerificationService faceVerificationService;
    private final MediaRepository mediaRepository;
    private final IdentityVerificationRepository identityVerificationRepository;

    @Override
    @Transactional
    public UserDto register(SaveUserDto request) {
        User existantUser = userRepository.findByEmail(request.getEmail());

        if (existantUser != null) {
            throw new ResponseStatusException(
                    HttpStatus.PRECONDITION_FAILED,
                    "Ya existe un usuario registrado con el correo " + request.getEmail());
        }

        // Validar que las imágenes estén presentes
        if (request.getFotoCedula() == null || request.getFotoCedula().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La foto de la cédula es obligatoria");
        }

        if (request.getSelfie() == null || request.getSelfie().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El selfie es obligatorio");
        }

        // Paso 1: Validar que la imagen sea una cédula válida
        IdValidationResponse idValidation = faceVerificationService.validateIdDocument(request.getFotoCedula());

        if (!idValidation.getIsIdDocument()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La imagen no parece ser una cédula válida. Confianza: " +
                    String.format("%.2f%%", idValidation.getConfidence() * 100));
        }

        // Paso 2: Comparar rostros (cédula vs selfie)
        FaceVerificationResponse faceVerification;
        try {
            faceVerification = faceVerificationService.verifyFace(request.getFotoCedula(), request.getSelfie());
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Error al verificar rostros: " + e.getMessage());
        }

        // Paso 3: Verificar que los rostros coincidan
        if (!faceVerification.getMatch()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    String.format(
                            "Los rostros no coinciden. Similitud: %.2f%%, Distancia: %.4f (umbral: %.2f)",
                            faceVerification.getSimilarity() * 100,
                            faceVerification.getDistance(),
                            faceVerification.getThreshold()
                    ));
        }

        // Paso 4: Crear el usuario
        User user = User.builder()
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role("USER")
                .status("ACTIVE")
                .build();

        User savedUser = userRepository.save(user);

        // Paso 5: Guardar las imágenes en la tabla Media
        // TODO: En producción, las imágenes deberían guardarse en un servicio de almacenamiento
        // (S3, Azure Blob, etc.) y aquí solo guardar las URLs
        Media idDocumentMedia = Media.builder()
                .ownerUser(savedUser)
                .url("temp://id-document/" + savedUser.getId()) // URL temporal
                .mimeType(request.getFotoCedula().getContentType())
                .forBlurAnalysis(false)
                .build();

        Media selfieMedia = Media.builder()
                .ownerUser(savedUser)
                .url("temp://selfie/" + savedUser.getId()) // URL temporal
                .mimeType(request.getSelfie().getContentType())
                .forBlurAnalysis(false)
                .build();

        Media savedIdDocument = mediaRepository.save(idDocumentMedia);
        Media savedSelfie = mediaRepository.save(selfieMedia);

        // Paso 6: Guardar el registro de verificación de identidad
        IdentityVerification verification = IdentityVerification.builder()
                .user(savedUser)
                .idDocumentMedia(savedIdDocument)
                .selfieMedia(savedSelfie)
                .provider("FACE_RECOGNITION_PYTHON")
                .status("VERIFIED")
                .verificationMethod("FACE_RECOGNITION_PYTHON")
                .createdAt(OffsetDateTime.now())
                .decidedAt(OffsetDateTime.now())
                // Datos de validación de cédula
                .isValidIdDocument(idValidation.getIsIdDocument())
                .idValidationConfidence(BigDecimal.valueOf(idValidation.getConfidence()))
                // Datos de comparación facial
                .faceMatchSimilarity(BigDecimal.valueOf(faceVerification.getSimilarity()))
                .faceMatchConfidence(BigDecimal.valueOf(1.0 - faceVerification.getDistance())) // Confianza inversa a distancia
                .facesMatch(faceVerification.getMatch())
                .matchScore(BigDecimal.valueOf(faceVerification.getSimilarity() * 100)) // Score en porcentaje
                .build();

        identityVerificationRepository.save(verification);

        return modelMapper.map(savedUser, UserDto.class);
    }

    @Override
    public String login(AuthenticationRequest authenticationRequest) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authenticationRequest.getEmail(), authenticationRequest.getPassword())
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(authenticationRequest.getEmail());

        return jwtUtil.generateToken(userDetails);
    }
}
