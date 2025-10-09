package com.fram.vigilapp.controller;

import com.fram.vigilapp.config.auth.AuthenticationRequest;
import com.fram.vigilapp.dto.SaveUserDto;
import com.fram.vigilapp.dto.UserDto;
import com.fram.vigilapp.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;


    @PostMapping("/register")
    public UserDto registerUser(@Valid @RequestBody SaveUserDto request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public String login(@Valid @RequestBody AuthenticationRequest authenticationRequest) {
        return authService.login(authenticationRequest);
    }
}