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
import com.fram.vigilapp.service.FaceVerificationService;
import com.fram.vigilapp.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthServiceImplTest {

    @Mock private ModelMapper modelMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserRepository userRepository;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private CustomUserDetailsService userDetailsService;
    @Mock private JwtUtil jwtUtil;
    @Mock private FaceVerificationService faceVerificationService;
    @Mock private MediaRepository mediaRepository;
    @Mock private IdentityVerificationRepository identityVerificationRepository;

    @InjectMocks private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private SaveUserDto buildSaveUserDto(boolean withCedula, boolean withSelfie) {
        SaveUserDto dto = new SaveUserDto();
        dto.setEmail("new@user.com");
        dto.setFirstName("New");
        dto.setLastName("User");
        dto.setPassword("pwd");
        if (withCedula) {
            dto.setFotoCedula(new MockMultipartFile("fotoCedula", "id.png", "image/png", new byte[]{1,2}));
        }
        if (withSelfie) {
            dto.setSelfie(new MockMultipartFile("selfie", "selfie.png", "image/png", new byte[]{3,4}));
        }
        return dto;
    }

    @Test
    void register_when_user_exists_throws_precondition_failed() {
        when(userRepository.findByEmail("new@user.com")).thenReturn(User.builder().build());

        SaveUserDto dto = buildSaveUserDto(true, true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.register(dto));
        assertEquals(HttpStatus.PRECONDITION_FAILED, ex.getStatusCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_missing_cedula_throws_bad_request() {
        when(userRepository.findByEmail(anyString())).thenReturn(null);
        SaveUserDto dto = buildSaveUserDto(false, true);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.register(dto));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void register_missing_selfie_throws_bad_request() {
        when(userRepository.findByEmail(anyString())).thenReturn(null);
        SaveUserDto dto = buildSaveUserDto(true, false);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.register(dto));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void register_invalid_id_document_throws_bad_request() {
        when(userRepository.findByEmail(anyString())).thenReturn(null);
        SaveUserDto dto = buildSaveUserDto(true, true);
        when(faceVerificationService.validateIdDocument(any())).thenReturn(
                        IdValidationResponse.builder().isIdDocument(false).confidence(0.2).build()
                );
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.register(dto));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void register_face_mismatch_throws_bad_request() {
        when(userRepository.findByEmail(anyString())).thenReturn(null);
        SaveUserDto dto = buildSaveUserDto(true, true);
        when(faceVerificationService.validateIdDocument(any())).thenReturn(
                        IdValidationResponse.builder().isIdDocument(true).confidence(0.99).build()
                );
        when(faceVerificationService.verifyFace(any(), any())).thenReturn(
                com.fram.vigilapp.dto.FaceVerificationResponse.builder()
                        .match(false).distance(0.8).similarity(0.2).threshold(0.5).build()
        );
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.register(dto));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void register_success_persists_user_media_and_identity_verification() {
        when(userRepository.findByEmail(anyString())).thenReturn(null);
        when(faceVerificationService.validateIdDocument(any())).thenReturn(
                        IdValidationResponse.builder().isIdDocument(true).confidence(0.95).build()
                );
        when(faceVerificationService.verifyFace(any(), any())).thenReturn(
                FaceVerificationResponse.builder().match(true).distance(0.1).similarity(0.95).threshold(0.5).build()
        );
        when(passwordEncoder.encode("pwd")).thenReturn("ENCODED");

        User saved = User.builder().id(UUID.randomUUID()).email("new@user.com").passwordHash("ENCODED").build();
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(mediaRepository.save(any(Media.class))).thenAnswer(inv -> inv.getArgument(0));
        when(identityVerificationRepository.save(any(IdentityVerification.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDto mapped = new UserDto();
        mapped.setEmail("new@user.com");
        when(modelMapper.map(any(User.class), eq(UserDto.class))).thenReturn(mapped);

        SaveUserDto dto = buildSaveUserDto(true, true);
        UserDto result = authService.register(dto);

        assertEquals("new@user.com", result.getEmail());
        verify(userRepository).save(any(User.class));
        verify(mediaRepository, times(2)).save(any(Media.class));
        verify(identityVerificationRepository).save(any(IdentityVerification.class));
    }

    @Test
    void login_success_returns_jwt() {
        AuthenticationRequest request = new AuthenticationRequest();
        request.setEmail("u@e.com");
        request.setPassword("p");

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("u@e.com").password("p").authorities("ROLE_USER").build();

        when(userDetailsService.loadUserByUsername("u@e.com")).thenReturn(userDetails);
        when(jwtUtil.generateToken(userDetails)).thenReturn("token-123");

        String token = authService.login(request);
        assertEquals("token-123", token);
    }
}
