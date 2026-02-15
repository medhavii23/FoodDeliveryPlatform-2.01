package com.foodapp.identity_service.service;

import com.foodapp.identity_service.constants.Constants;
import com.foodapp.identity_service.model.User;
import com.foodapp.identity_service.repository.AuthRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    void saveUser_setsRoleAndEncodesPassword() {
        User user = new User();
        user.setUsername("alice");
        user.setPassword("plain");
        when(passwordEncoder.encode("plain")).thenReturn("encoded");

        User result = authService.saveUser(user);

        assertThat(result.getRole()).isEqualTo(Constants.ROLE_USER);
        assertThat(result.getPassword()).isEqualTo("encoded");
        verify(repository).save(user);
    }

    @Test
    void saveAdmin_setsRoleAndEncodesPassword() {
        User user = new User();
        user.setUsername("admin");
        user.setPassword("secret");
        when(passwordEncoder.encode("secret")).thenReturn("encoded");

        User result = authService.saveAdmin(user);

        assertThat(result.getRole()).isEqualTo(Constants.ROLE_ADMIN);
        assertThat(result.getPassword()).isEqualTo("encoded");
        verify(repository).save(user);
    }

    @Test
    void generateToken_whenUserFound_returnsToken() {
        User user = new User(UUID.randomUUID(), "bob", null, "encoded", Constants.ROLE_USER);
        when(repository.findByUsername("bob")).thenReturn(Optional.of(user));
        when(jwtService.generateToken("bob", Constants.ROLE_USER, user.getId())).thenReturn("jwt-token");

        String token = authService.generateToken("bob");

        assertThat(token).isEqualTo("jwt-token");
    }

    @Test
    void generateToken_whenUserNotFound_throws() {
        when(repository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.generateToken("unknown"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(Constants.USER_NOT_FOUND);
    }

    @Test
    void validateToken_delegatesToJwtService() {
        authService.validateToken("some-token");
        verify(jwtService).validateToken("some-token");
    }
}
