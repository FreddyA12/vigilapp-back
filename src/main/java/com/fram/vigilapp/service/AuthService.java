package com.fram.vigilapp.service;

import com.fram.vigilapp.config.auth.AuthenticationRequest;
import com.fram.vigilapp.dto.SaveUserDto;
import com.fram.vigilapp.dto.UserDto;

public interface AuthService {
    UserDto register(SaveUserDto request);

    String login(AuthenticationRequest authenticationRequest);
}
