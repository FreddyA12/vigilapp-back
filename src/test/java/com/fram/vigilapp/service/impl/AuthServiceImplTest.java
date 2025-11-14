package com.fram.vigilapp.service.impl;

import com.fram.vigilapp.config.auth.AuthenticationRequest;
import com.fram.vigilapp.dto.FaceVerificationResponse;
import com.fram.vigilapp.dto.IdValidationResponse;
import com.fram.vigilapp.dto.SaveUserDto;
import com.fram.vigilapp.dto.UserDto;
import com.fram.vigilapp.entity.User;
import com.fram.vigilapp.repository.IdentityVerificationRepository;
import com.fram.vigilapp.repository.MediaRepository;
import com.fram.vigilapp.repository.UserRepository;
import com.fram.vigilapp.service.FaceVerificationService;
import com.fram.vigilapp.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private FaceVerificationService faceVerificationService;

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private IdentityVerificationRepository identityVerificationRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    private SaveUserDto saveUserDto;
    private MockMultipartFile fotoCedula;
    private MockMultipartFile selfie;
    private User savedUser;

    @BeforeEach
    void setUp() {
        fotoCedula = new MockMultipartFile("fotoCedula", "cedula.jpg", "image/jpeg", "cedula content".getBytes());
        selfie = new MockMultipartFile("selfie", "selfie.jpg", "image/jpeg", "selfie content".getBytes());

        saveUserDto = new SaveUserDto();
        saveUserDto.setEmail("test@example.com");
        saveUserDto.setFirstName("John");
        saveUserDto.setLastName("Doe");
        saveUserDto.setPassword("password123");
        saveUserDto.setFotoCedula(fotoCedula);
        saveUserDto.setSelfie(selfie);

        savedUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .passwordHash("encodedPassword")
                .role("USER")
                .status("ACTIVE")
                .build();
    }

    @Test
    void register_WithValidData_ShouldCreateUser() {
        // Given
        when(userRepository.findByEmail(saveUserDto.getEmail())).thenReturn(null);

        IdValidationResponse idValidation = new IdValidationResponse();
        idValidation.setIsIdDocument(true);
        idValidation.setConfidence(0.95);
        when(faceVerificationService.validateIdDocument(any())).thenReturn(idValidation);

        FaceVerificationResponse faceVerification = new FaceVerificationResponse();
        faceVerification.setMatch(true);
        faceVerification.setSimilarity(0.85);
        faceVerification.setDistance(0.35);
        faceVerification.setThreshold(0.6);
        when(faceVerificationService.verifyFace(any(), any())).thenReturn(faceVerification);

        when(passwordEncoder.encode(saveUserDto.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(mediaRepository.save(any())).thenReturn(null);
        when(identityVerificationRepository.save(any())).thenReturn(null);

        UserDto expectedDto = new UserDto();
        expectedDto.setEmail("test@example.com");
        when(modelMapper.map(savedUser, UserDto.class)).thenReturn(expectedDto);

        // When
        UserDto result = authService.register(saveUserDto);

        // Then
        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
        verify(userRepository).findByEmail(saveUserDto.getEmail());
        verify(faceVerificationService).validateIdDocument(fotoCedula);
        verify(faceVerificationService).verifyFace(fotoCedula, selfie);
        verify(userRepository).save(any(User.class));
        verify(mediaRepository, times(2)).save(any());
        verify(identityVerificationRepository).save(any());
    }

    @Test
    void register_WithExistingEmail_ShouldThrowException() {
        // Given
        when(userRepository.findByEmail(saveUserDto.getEmail())).thenReturn(savedUser);

        // When & Then
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.register(saveUserDto)
        );

        assertEquals(HttpStatus.PRECONDITION_FAILED, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Ya existe un usuario registrado"));
        verify(userRepository).findByEmail(saveUserDto.getEmail());
        verify(faceVerificationService, never()).validateIdDocument(any());
    }

    @Test
    void register_WithoutFotoCedula_ShouldThrowException() {
        // Given
        saveUserDto.setFotoCedula(null);
        when(userRepository.findByEmail(saveUserDto.getEmail())).thenReturn(null);

        // When & Then
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.register(saveUserDto)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("La foto de la cédula es obligatoria", exception.getReason());
    }

    @Test
    void register_WithEmptyFotoCedula_ShouldThrowException() {
        // Given
        saveUserDto.setFotoCedula(new MockMultipartFile("fotoCedula", new byte[0]));
        when(userRepository.findByEmail(saveUserDto.getEmail())).thenReturn(null);

        // When & Then
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.register(saveUserDto)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("La foto de la cédula es obligatoria", exception.getReason());
    }

    @Test
    void register_WithoutSelfie_ShouldThrowException() {
        // Given
        saveUserDto.setSelfie(null);
        when(userRepository.findByEmail(saveUserDto.getEmail())).thenReturn(null);

        // When & Then
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.register(saveUserDto)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("El selfie es obligatorio", exception.getReason());
    }

    @Test
    void register_WithInvalidIdDocument_ShouldThrowException() {
        // Given
        when(userRepository.findByEmail(saveUserDto.getEmail())).thenReturn(null);

        IdValidationResponse idValidation = new IdValidationResponse();
        idValidation.setIsIdDocument(false);
        idValidation.setConfidence(0.3);
        when(faceVerificationService.validateIdDocument(any())).thenReturn(idValidation);

        // When & Then
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.register(saveUserDto)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("no parece ser una cédula válida"));
    }

    @Test
    void register_WithNonMatchingFaces_ShouldThrowException() {
        // Given
        when(userRepository.findByEmail(saveUserDto.getEmail())).thenReturn(null);

        IdValidationResponse idValidation = new IdValidationResponse();
        idValidation.setIsIdDocument(true);
        idValidation.setConfidence(0.95);
        when(faceVerificationService.validateIdDocument(any())).thenReturn(idValidation);

        FaceVerificationResponse faceVerification = new FaceVerificationResponse();
        faceVerification.setMatch(false);
        faceVerification.setSimilarity(0.45);
        faceVerification.setDistance(0.65);
        faceVerification.setThreshold(0.6);
        when(faceVerificationService.verifyFace(any(), any())).thenReturn(faceVerification);

        // When & Then
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.register(saveUserDto)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Los rostros no coinciden"));
    }

    @Test
    void login_WithValidCredentials_ShouldReturnJwtToken() {
        // Given
        AuthenticationRequest authRequest = new AuthenticationRequest();
        authRequest.setEmail("test@example.com");
        authRequest.setPassword("password123");

        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username("test@example.com")
                .password("encodedPassword")
                .authorities("ROLE_USER")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(userDetails);
        when(jwtUtil.generateToken(userDetails)).thenReturn("jwt-token-123");

        // When
        String token = authService.login(authRequest);

        // Then
        assertNotNull(token);
        assertEquals("jwt-token-123", token);
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userDetailsService).loadUserByUsername("test@example.com");
        verify(jwtUtil).generateToken(userDetails);
    }
}
