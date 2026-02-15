package com.foodapp.api_gateway.util;

import com.foodapp.api_gateway.constants.Constants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
    }

    private String createValidToken(String username, String role, UUID userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(Constants.CLAIM_ROLE, role);
        claims.put(Constants.CLAIM_USER_ID, userId);
        Key key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(JwtUtil.SECRET));
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 30))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    @Test
    void validateToken_withValidToken_doesNotThrow() {
        String token = createValidToken("testuser", "USER", UUID.randomUUID());
        jwtUtil.validateToken(token);
    }

    @Test
    void validateToken_withInvalidToken_throws() {
        assertThatThrownBy(() -> jwtUtil.validateToken("invalid-token"))
                .isInstanceOf(Exception.class);
    }

    @Test
    void validateToken_withExpiredToken_throws() {
        Key key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(JwtUtil.SECRET));
        String expired = Jwts.builder()
                .setSubject("user")
                .setIssuedAt(new Date(System.currentTimeMillis() - 10000))
                .setExpiration(new Date(System.currentTimeMillis() - 1000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
        assertThatThrownBy(() -> jwtUtil.validateToken(expired))
                .isInstanceOf(Exception.class);
    }

    @Test
    void extractUsername_returnsSubject() {
        String token = createValidToken("alice", "ADMIN", UUID.randomUUID());
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("alice");
    }

    @Test
    void extractAllClaims_returnsRoleAndUserId() {
        UUID userId = UUID.randomUUID();
        String token = createValidToken("bob", "USER", userId);
        Claims claims = jwtUtil.extractAllClaims(token);
        assertThat(claims.getSubject()).isEqualTo("bob");
        assertThat(claims.get(Constants.CLAIM_ROLE, String.class)).isEqualTo("USER");
        assertThat(claims.get(Constants.CLAIM_USER_ID)).isEqualTo(userId.toString());
    }
}
