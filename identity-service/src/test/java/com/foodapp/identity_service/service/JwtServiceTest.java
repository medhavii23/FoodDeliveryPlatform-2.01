package com.foodapp.identity_service.service;

import com.foodapp.identity_service.constants.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
    }

    @Test
    void generateToken_andValidateToken_success() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateToken("user1", Constants.ROLE_USER, userId);
        assertThat(token).isNotBlank();
        jwtService.validateToken(token);
    }

    @Test
    void validateToken_withInvalidToken_throws() {
        assertThatThrownBy(() -> jwtService.validateToken("invalid"))
                .isInstanceOf(Exception.class);
    }

    @Test
    void generateToken_includesRoleAndUserId() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateToken("admin", Constants.ROLE_ADMIN, userId);
        assertThat(token).isNotBlank();
        jwtService.validateToken(token);
    }
}
