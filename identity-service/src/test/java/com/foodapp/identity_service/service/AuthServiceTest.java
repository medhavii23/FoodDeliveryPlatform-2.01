package com.foodapp.identity_service.service;

import com.foodapp.identity_service.constants.Constants;
import com.foodapp.identity_service.model.User;
import com.foodapp.identity_service.repository.AuthRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void testSaveUser() {
        User user = new User();
        user.setUsername("testUser");
        user.setPassword("password");

        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
        when(repository.save(any(User.class))).thenReturn(user);

        User savedUser = authService.saveUser(user);

        assertNotNull(savedUser);
        assertEquals(Constants.ROLE_USER, savedUser.getRole());
        assertEquals("encodedPassword", savedUser.getPassword());
        verify(repository).save(any(User.class));
    }

    @Test
    void testSaveAdmin() {
        User user = new User();
        user.setUsername("adminUser");
        user.setPassword("password");

        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
        when(repository.save(any(User.class))).thenReturn(user);

        User savedAdmin = authService.saveAdmin(user);

        assertNotNull(savedAdmin);
        assertEquals(Constants.ROLE_ADMIN, savedAdmin.getRole());
        verify(repository).save(any(User.class));
    }

    @Test
    void testGenerateToken_UserExists() {
        String username = "testUser";
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setUsername(username);
        user.setRole(Constants.ROLE_USER);
        user.setId(userId);

        when(repository.findByUsername(username)).thenReturn(Optional.of(user));
        when(jwtService.generateToken(username, Constants.ROLE_USER, userId)).thenReturn("token");

        String token = authService.generateToken(username);

        assertEquals("token", token);
    }

    @Test
    void testGenerateToken_UserNotFound() {
        String username = "unknownUser";
        when(repository.findByUsername(username)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> authService.generateToken(username));
    }

    @Test
    void testValidateToken() {
        String token = "validToken";
        doNothing().when(jwtService).validateToken(token);

        assertDoesNotThrow(() -> authService.validateToken(token));
        verify(jwtService).validateToken(token);
    }
}
