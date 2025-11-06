package com.fram.vigilapp.service.impl;

import com.fram.vigilapp.entity.User;
import com.fram.vigilapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username);
        if (user == null) {
            throw new UsernameNotFoundException("Credenciales inválidas");
        }
        
        // Crear authority basado en el rol del usuario
        // El rol en la DB es "USER", "MOD" o "ADMIN"
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(user.getRole());
        
        return new org.springframework.security.core.userdetails.User(
            user.getEmail(), 
            user.getPasswordHash(), 
            Collections.singletonList(authority)
        );
    }
}
