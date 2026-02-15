package com.foodapp.api_gateway.util;

import com.foodapp.api_gateway.constants.Constants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.security.Key;

/**
 * Utility for JWT validation and claim extraction in the API Gateway.
 */
@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    public static final String SECRET = Constants.JWT_SECRET;

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
     * Extracts subject (username) from the JWT.
     *
     * @param token JWT string
     * @return username (subject)
     */
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Extracts all claims from the JWT body.
     *
     * @param token JWT string
     * @return claims
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(getSignKey()).build().parseClaimsJws(token).getBody();
    }

    private Key getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
