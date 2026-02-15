package com.foodapp.identity_service.service;

import com.foodapp.identity_service.constants.Constants;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for JWT creation and validation (HS256, claims: role, userId).
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    /**
     * Validates JWT signature and expiry; throws if invalid.
     *
     * @param token JWT string
     */
    public void validateToken(final String token) {
        log.debug("Validating JWT");
        Jwts.parserBuilder().setSigningKey(getSignKey()).build().parseClaimsJws(token);
    }

    /**
     * Builds a JWT with subject (username), role, and userId claims.
     *
     * @param userName subject (username)
     * @param role user role (USER/ADMIN)
     * @param userId user UUID
     * @return compact JWT string
     */
    public String generateToken(String userName, String role, java.util.UUID userId) {
        log.debug("Generating JWT for user: {} role: {}", userName, role);
        Map<String, Object> claims = new HashMap<>();
        claims.put(Constants.CLAIM_ROLE, role);
        claims.put(Constants.CLAIM_USER_ID, userId);
        return createToken(claims, userName);
    }

    /**
     * Creates the JWT with claims, subject, issued/expiry dates, and signature.
     */
    private String createToken(Map<String, Object> claims, String userName) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userName)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 30))
                .signWith(getSignKey(), SignatureAlgorithm.HS256).compact();
    }

    private Key getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(Constants.JWT_SECRET);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
