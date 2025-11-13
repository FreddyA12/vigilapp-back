package com.fram.vigilapp.controller;

import com.fram.vigilapp.config.auth.AuthenticationRequest;
import com.fram.vigilapp.dto.SaveUserDto;
import com.fram.vigilapp.dto.UserDto;
import com.fram.vigilapp.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;


    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserDto registerUser(
            @RequestParam("firstName") String firstName,
            @RequestParam("lastName") String lastName,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("fotoCedula") MultipartFile fotoCedula,
            @RequestParam("selfie") MultipartFile selfie
    ) {
        SaveUserDto request = new SaveUserDto();
        request.setFirstName(firstName);
        request.setLastName(lastName);
        request.setEmail(email);
        request.setPassword(password);
        request.setFotoCedula(fotoCedula);
        request.setSelfie(selfie);

        return authService.register(request);
    }

    @PostMapping("/login")
    public String login(@Valid @RequestBody AuthenticationRequest authenticationRequest) {
        return authService.login(authenticationRequest);
    }
}