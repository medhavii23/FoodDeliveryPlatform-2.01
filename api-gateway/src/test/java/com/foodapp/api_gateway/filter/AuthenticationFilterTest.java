package com.foodapp.api_gateway.filter;

import com.foodapp.api_gateway.constants.Constants;
import com.foodapp.api_gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RouteValidator validator;

    @Mock
    private GatewayFilterChain filterChain;

    @InjectMocks
    private AuthenticationFilter authenticationFilter;

    private GatewayFilter filter;

    @BeforeEach
    void setup() {
        filter = authenticationFilter.apply(new AuthenticationFilter.Config());
    }

    // --------------------------------------------------
    // Constructor Coverage
    // --------------------------------------------------
    @Test
    void testConstructor() {
        AuthenticationFilter filter = new AuthenticationFilter();
        assertNotNull(filter);
    }

    // --------------------------------------------------
    // Open Endpoint → Bypass
    // --------------------------------------------------
    @Test
    void testOpenEndpoint_ShouldBypassAuth() {

        when(validator.isSecured(any())).thenReturn(false);

        MockServerHttpRequest request =
                MockServerHttpRequest.get("/auth/register").build();

        MockServerWebExchange exchange =
                MockServerWebExchange.from(request);

        when(filterChain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, filterChain).block();

        verify(jwtUtil, never()).validateToken(any());
        verify(filterChain).filter(exchange);
    }

    // --------------------------------------------------
    // Secured Endpoint → Missing Header
    // --------------------------------------------------
    @Test
    void testSecuredEndpoint_MissingAuthHeader_ShouldThrow() {

        when(validator.isSecured(any())).thenReturn(true);

        MockServerHttpRequest request =
                MockServerHttpRequest.get("/api/orders").build();

        MockServerWebExchange exchange =
                MockServerWebExchange.from(request);

        assertThrows(RuntimeException.class,
                () -> filter.filter(exchange, filterChain).block());
    }

    // --------------------------------------------------
    // Secured Endpoint → Valid Bearer Token
    // --------------------------------------------------
    @Test
    void testValidToken_WithBearerPrefix_ShouldAuthenticate() {

        when(validator.isSecured(any())).thenReturn(true);

        String token = "valid_token";

        MockServerHttpRequest request =
                MockServerHttpRequest.get("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build();

        MockServerWebExchange exchange =
                MockServerWebExchange.from(request);

        Claims claims = new DefaultClaims();
        claims.setSubject("testUser");
        claims.put(Constants.CLAIM_ROLE, "USER");
        claims.put(Constants.CLAIM_USER_ID, 123);

        doNothing().when(jwtUtil).validateToken(token);
        when(jwtUtil.extractAllClaims(token)).thenReturn(claims);
        when(filterChain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchange, filterChain).block();

        verify(jwtUtil).validateToken(token);
        verify(jwtUtil).extractAllClaims(token);
        verify(filterChain).filter(any());
    }

    // --------------------------------------------------
    // Secured Endpoint → Token without Bearer
    // --------------------------------------------------
    @Test
    void testValidToken_WithoutBearerPrefix_ShouldAuthenticate() {

        when(validator.isSecured(any())).thenReturn(true);

        String token = "raw_token";

        MockServerHttpRequest request =
                MockServerHttpRequest.get("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .build();

        MockServerWebExchange exchange =
                MockServerWebExchange.from(request);

        Claims claims = new DefaultClaims();
        claims.setSubject("user");

        doNothing().when(jwtUtil).validateToken(token);
        when(jwtUtil.extractAllClaims(token)).thenReturn(claims);
        when(filterChain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchange, filterChain).block();

        verify(jwtUtil).validateToken(token);
        verify(filterChain).filter(any());
    }

    // --------------------------------------------------
    // Secured Endpoint → Invalid Token
    // --------------------------------------------------
    @Test
    void testInvalidToken_ShouldThrowUnauthorized() {

        when(validator.isSecured(any())).thenReturn(true);

        String token = "invalid";

        MockServerHttpRequest request =
                MockServerHttpRequest.get("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build();

        MockServerWebExchange exchange =
                MockServerWebExchange.from(request);

        doThrow(new RuntimeException("Invalid"))
                .when(jwtUtil).validateToken(token);

        assertThrows(RuntimeException.class,
                () -> filter.filter(exchange, filterChain).block());
    }

    // --------------------------------------------------
    // Secured Endpoint → Claims Extraction Failure
    // --------------------------------------------------
    @Test
    void testClaimsExtractionFailure_ShouldThrowUnauthorized() {

        when(validator.isSecured(any())).thenReturn(true);

        String token = "token";

        MockServerHttpRequest request =
                MockServerHttpRequest.get("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build();

        MockServerWebExchange exchange =
                MockServerWebExchange.from(request);

        doNothing().when(jwtUtil).validateToken(token);
        when(jwtUtil.extractAllClaims(token))
                .thenThrow(new RuntimeException("Claims error"));

        assertThrows(RuntimeException.class,
                () -> filter.filter(exchange, filterChain).block());
    }

    // --------------------------------------------------
    // Secured Endpoint → Null Role & UserId
    // --------------------------------------------------
    @Test
    void testNullClaimsValues_ShouldStillPass() {

        when(validator.isSecured(any())).thenReturn(true);

        String token = "token";

        MockServerHttpRequest request =
                MockServerHttpRequest.get("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build();

        MockServerWebExchange exchange =
                MockServerWebExchange.from(request);

        Claims claims = new DefaultClaims();
        claims.setSubject("user");

        doNothing().when(jwtUtil).validateToken(token);
        when(jwtUtil.extractAllClaims(token)).thenReturn(claims);
        when(filterChain.filter(any())).thenReturn(Mono.empty());

        assertDoesNotThrow(() ->
                filter.filter(exchange, filterChain).block());
    }
}
