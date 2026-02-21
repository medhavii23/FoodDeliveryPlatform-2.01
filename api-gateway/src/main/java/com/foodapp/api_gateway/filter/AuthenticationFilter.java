package com.foodapp.api_gateway.filter;

import com.foodapp.api_gateway.constants.Constants;
import com.foodapp.api_gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/**
 * Gateway filter that validates JWT on secured routes and adds user/role/id headers to the request.
 */
@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationFilter.class);

    @Autowired
    private RouteValidator validator;

    @Autowired
    private JwtUtil jwtUtil;

    public AuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {

            if (validator.isSecured(exchange.getRequest())) {

                if (!exchange.getRequest().getHeaders()
                        .containsKey(HttpHeaders.AUTHORIZATION)) {

                    log.warn("Secured route accessed without Authorization header: {}",
                            exchange.getRequest().getURI().getPath());

                    throw new RuntimeException(Constants.ERROR_MISSING_AUTH_HEADER);
                }

                String authHeader = exchange.getRequest()
                        .getHeaders()
                        .get(HttpHeaders.AUTHORIZATION)
                        .get(0);

                if (authHeader != null &&
                        authHeader.startsWith(Constants.BEARER_PREFIX)) {

                    authHeader = authHeader.substring(
                            Constants.BEARER_PREFIX_LENGTH);
                }

                try {
                    jwtUtil.validateToken(authHeader);
                    Claims claims = jwtUtil.extractAllClaims(authHeader);

                    String username = claims.getSubject();
                    String role = claims.get(Constants.CLAIM_ROLE, String.class);
                    String userId = String.valueOf(
                            claims.get(Constants.CLAIM_USER_ID));

                    return chain.filter(exchange.mutate().request(
                                    exchange.getRequest().mutate()
                                            .header(Constants.HEADER_AUTH_USER, username)
                                            .header(Constants.HEADER_AUTH_ROLE, role)
                                            .header(Constants.HEADER_AUTH_ID, userId)
                                            .build())
                            .build());

                } catch (Exception _) {
                    throw new RuntimeException(Constants.ERROR_UNAUTHORIZED_ACCESS);
                }
            }

            return chain.filter(exchange);
        });
    }


    /** Filter configuration (no-op for this filter). */
    public static class Config {
    }
}
