package com.fram.vigilapp.service.impl;

import com.fram.vigilapp.config.auth.AuthenticationRequest;
import com.fram.vigilapp.dto.FaceVerificationResponse;
import com.fram.vigilapp.dto.IdValidationResponse;
import com.fram.vigilapp.dto.SaveUserDto;
import com.fram.vigilapp.dto.UserDto;
import com.fram.vigilapp.entity.User;
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
import org.springframework.web.server.ResponseStatusException;

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

    @Override
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
                    "La imagen no parece ser una cédula válida. Razones: " + String.join(", ", idValidation.getReasons()));
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

        // Paso 4: Si todo está bien, crear el usuario
        User user = User.builder()
                .email(request.getEmail())
                .firstName(request.getName())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role("USER")
                .status("ACTIVE")
                .build();

        return modelMapper.map(userRepository.save(user), UserDto.class);
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
