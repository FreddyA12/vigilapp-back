package com.fram.vigilapp.controller;

import com.fram.vigilapp.dto.SaveUserZoneDto;
import com.fram.vigilapp.dto.UserZoneDto;
import com.fram.vigilapp.entity.User;
import com.fram.vigilapp.repository.UserRepository;
import com.fram.vigilapp.service.UserZoneService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/api/user-zones")
@RequiredArgsConstructor
public class UserZoneController {

    private final UserZoneService userZoneService;
    private final UserRepository userRepository;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('USER', 'MOD', 'ADMIN')")
    public ResponseEntity<UserZoneDto> createOrUpdateUserZone(
            @Valid @RequestBody SaveUserZoneDto saveUserZoneDto,
            Authentication authentication
    ) {
        String email = authentication.getName();
        log.info("[UserZoneController] 📍 POST /user-zones - User: {}", email);
        log.info("[UserZoneController] 📊 Data: lat={}, lon={}, radius={}m", 
            saveUserZoneDto.getCenterLatitude(), 
            saveUserZoneDto.getCenterLongitude(), 
            saveUserZoneDto.getRadiusM());
        
        User user = userRepository.findByEmail(email);

        if (user == null) {
            log.error("[UserZoneController] ❌ User not found: {}", email);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("[UserZoneController] ✅ User found: id={}, email={}", user.getId(), user.getEmail());
        
        try {
            UserZoneDto userZoneDto = userZoneService.createOrUpdateUserZone(user, saveUserZoneDto);
            log.info("[UserZoneController] ✅ Zone saved: id={}", userZoneDto.getId());
            return ResponseEntity.ok(userZoneDto);
        } catch (Exception e) {
            log.error("[UserZoneController] ❌ Error saving zone: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyAuthority('USER', 'MOD', 'ADMIN')")
    public ResponseEntity<UserZoneDto> getMyUserZone(Authentication authentication) {
        String email = authentication.getName();
        log.debug("[UserZoneController] getMyUserZone called by: {}", email);
        User user = userRepository.findByEmail(email);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserZoneDto userZoneDto = userZoneService.getUserZone(user.getId());

        if (userZoneDto == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(userZoneDto);
    }

    @DeleteMapping("/me")
    @PreAuthorize("hasAnyAuthority('USER', 'MOD', 'ADMIN')")
    public ResponseEntity<Void> deleteMyUserZone(Authentication authentication) {
        String email = authentication.getName();
        log.debug("[UserZoneController] deleteMyUserZone called by: {}", email);
        User user = userRepository.findByEmail(email);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        userZoneService.deleteUserZone(user.getId());
        return ResponseEntity.noContent().build();
    }
}
