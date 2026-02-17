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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationFilterTest {

    @Spy
    private RouteValidator validator = new RouteValidator();

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private GatewayFilterChain filterChain;

    @InjectMocks
    private AuthenticationFilter authenticationFilter;



    @Test
    void testApply_OpenEndpoint_ShouldBypassAuth() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest.get(Constants.ENDPOINT_AUTH_REGISTER).build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilter filter = authenticationFilter.apply(new AuthenticationFilter.Config());
        when(filterChain.filter(exchange)).thenReturn(Mono.empty());

        // Act
        filter.filter(exchange, filterChain).block();

        // Assert
        verify(jwtUtil, never()).validateToken(anyString());
        verify(filterChain).filter(exchange);
    }

    @Test
    void testApply_SecuredEndpoint_MissingAuthHeader_ShouldThrowexception() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/orders").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilter filter = authenticationFilter.apply(new AuthenticationFilter.Config());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> filter.filter(exchange, filterChain).block());
    }

    @Test
    void testApply_SecuredEndpoint_ValidToken_ShouldAuthenticate() {
        // Arrange
        String token = "valid_token";
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        Claims claims = new DefaultClaims(Map.of(
                "sub", "testUser",
                Constants.CLAIM_ROLE, "USER",
                Constants.CLAIM_USER_ID, 123));

        // Mock JwtUtil
        doNothing().when(jwtUtil).validateToken(token);
        when(jwtUtil.extractAllClaims(token)).thenReturn(claims);
        when(filterChain.filter(any())).thenReturn(Mono.empty());

        GatewayFilter filter = authenticationFilter.apply(new AuthenticationFilter.Config());

        // Act
        filter.filter(exchange, filterChain).block();

        // Assert
        verify(jwtUtil).validateToken(token);
        verify(filterChain).filter(any()); // Should be called with mutated exchange
    }

    @Test
    void testApply_SecuredEndpoint_InvalidToken_ShouldThrowException() {
        // Arrange
        String token = "invalid_token";
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        doThrow(new RuntimeException("Invalid Token")).when(jwtUtil).validateToken(token);

        GatewayFilter filter = authenticationFilter.apply(new AuthenticationFilter.Config());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> filter.filter(exchange, filterChain).block());
    }
    @Test
    void testApply_SecuredEndpoint_AuthHeaderWithoutBearerPrefix_ShouldStillWork() {
        // Arrange
        String token = "raw_token_without_bearer";
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, token) // NO "Bearer "
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        Claims claims = new DefaultClaims(Map.of(
                "sub", "testUser",
                Constants.CLAIM_ROLE, "USER",
                Constants.CLAIM_USER_ID, 123));

        doNothing().when(jwtUtil).validateToken(token);
        when(jwtUtil.extractAllClaims(token)).thenReturn(claims);
        when(filterChain.filter(any())).thenReturn(Mono.empty());

        GatewayFilter filter = authenticationFilter.apply(new AuthenticationFilter.Config());

        // Act
        filter.filter(exchange, filterChain).block();

        // Assert
        verify(jwtUtil).validateToken(token);
        verify(filterChain).filter(any());
    }
    @Test
    void testApply_SecuredEndpoint_ClaimsExtractionFails_ShouldThrowException() {
        String token = "valid_but_claims_fail";

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        doNothing().when(jwtUtil).validateToken(token);
        when(jwtUtil.extractAllClaims(token))
                .thenThrow(new RuntimeException("Claims error"));

        GatewayFilter filter = authenticationFilter.apply(new AuthenticationFilter.Config());

        assertThrows(RuntimeException.class,
                () -> filter.filter(exchange, filterChain).block());
    }

    @Test
    void testApply_SecuredEndpoint_NullAuthHeader_ShouldThrow() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, (String) null)
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilter filter = authenticationFilter.apply(new AuthenticationFilter.Config());

        assertThrows(RuntimeException.class, () -> filter.filter(exchange, filterChain).block());
    }

    @Test
    void testApply_SecuredEndpoint_NullClaimsValues_ShouldStillPass() {
        String token = "token";

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        Claims claims = new DefaultClaims();
        claims.setSubject("user"); // role + userId missing

        doNothing().when(jwtUtil).validateToken(token);
        when(jwtUtil.extractAllClaims(token)).thenReturn(claims);
        when(filterChain.filter(any())).thenReturn(Mono.empty());

        GatewayFilter filter = authenticationFilter.apply(new AuthenticationFilter.Config());

        assertDoesNotThrow(() -> filter.filter(exchange, filterChain).block());
    }

    

}
