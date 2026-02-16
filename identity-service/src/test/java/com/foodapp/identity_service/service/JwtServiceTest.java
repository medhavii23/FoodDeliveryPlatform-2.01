package com.foodapp.identity_service.service;

import com.foodapp.identity_service.constants.Constants;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Key;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @InjectMocks
    private JwtService jwtService;

    @Test
    void testGenerateToken() {
        String token = jwtService.generateToken("testUser", Constants.ROLE_USER, UUID.randomUUID());
        assertNotNull(token);
    }

    @Test
    void testValidateToken_Valid() {
        String token = createTestToken("testUser");
        assertDoesNotThrow(() -> jwtService.validateToken(token));
    }

    @Test
    void testValidateToken_Invalid() {
        assertThrows(Exception.class, () -> jwtService.validateToken("invalidToken"));
    }

    private String createTestToken(String userName) {
        byte[] keyBytes = Decoders.BASE64.decode(Constants.JWT_SECRET);
        Key signKey = Keys.hmacShaKeyFor(keyBytes);

        return Jwts.builder()
                .setSubject(userName)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 30))
                .signWith(signKey, SignatureAlgorithm.HS256).compact();
    }
}
