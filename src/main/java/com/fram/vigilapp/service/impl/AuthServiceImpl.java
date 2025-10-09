package com.fram.vigilapp.service.impl;

import com.fram.vigilapp.config.auth.AuthenticationRequest;
import com.fram.vigilapp.dto.SaveUserDto;
import com.fram.vigilapp.dto.UserDto;
import com.fram.vigilapp.entity.User;
import com.fram.vigilapp.repository.UserRepository;
import com.fram.vigilapp.service.AuthService;
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

    @Override
    public UserDto register(SaveUserDto request) {
        User existantUser = userRepository.findByEmail(request.getEmail());

        if (existantUser != null) {
            throw new ResponseStatusException(
                    HttpStatus.PRECONDITION_FAILED,
                    "Ya existe un usuario registrado con el correo " + request.getEmail());
        }

        // Map DTO to new User schema
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
