package com.fram.vigilapp.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() throws Exception {
        jwtUtil = new JwtUtil();
        // Set secret via reflection because field is injected with @Value
        Field field = JwtUtil.class.getDeclaredField("SECRET_KEY");
        field.setAccessible(true);
        field.set(jwtUtil, "test-secret-key-0123456789-0123456789-0123456789-0123456789");
    }

    @Test
    void generate_and_validate_token_ok() {
        UserDetails userDetails = new User("alice@example.com", "pwd", Collections.emptyList());

        String token = jwtUtil.generateToken(userDetails);
        assertNotNull(token);

        assertTrue(jwtUtil.validateToken(token, userDetails));
        assertEquals("alice@example.com", jwtUtil.extractUsername(token));
        assertNotNull(jwtUtil.extractExpiration(token));
    }

    @Test
    void validate_token_wrong_user_returns_false() {
        UserDetails userA = new User("a@example.com", "pwd", Collections.emptyList());
        UserDetails userB = new User("b@example.com", "pwd", Collections.emptyList());
        String token = jwtUtil.generateToken(userA);
        assertFalse(jwtUtil.validateToken(token, userB));
    }

    @Test
    void invalid_token_returns_false() {
        assertFalse(jwtUtil.validateToken("invalid.token.value", new User("u","p", Collections.emptyList())));
    }
}
