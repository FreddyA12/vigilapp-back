package com.fram.vigilapp.controller;

import com.fram.vigilapp.dto.SaveUserZoneDto;
import com.fram.vigilapp.dto.UserZoneDto;
import com.fram.vigilapp.entity.User;
import com.fram.vigilapp.repository.UserRepository;
import com.fram.vigilapp.service.UserZoneService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
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
        User user = userRepository.findByEmail(email);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserZoneDto userZoneDto = userZoneService.createOrUpdateUserZone(user, saveUserZoneDto);
        return ResponseEntity.ok(userZoneDto);
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyAuthority('USER', 'MOD', 'ADMIN')")
    public ResponseEntity<UserZoneDto> getMyUserZone(Authentication authentication) {
        String email = authentication.getName();
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
        User user = userRepository.findByEmail(email);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        userZoneService.deleteUserZone(user.getId());
        return ResponseEntity.noContent().build();
    }
}
