package com.foodapp.api_gateway.filter;

import com.foodapp.api_gateway.constants.Constants;
import com.foodapp.api_gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthenticationFilterTest {

    private final RouteValidator routeValidator = new RouteValidator();

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private GatewayFilterChain chain;

    private AuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new AuthenticationFilter();
        ReflectionTestUtils.setField(filter, "validator", routeValidator);
        ReflectionTestUtils.setField(filter, "jwtUtil", jwtUtil);
        lenient().when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());
    }

    @Test
    void apply_whenPathNotSecured_invokesChainWithoutAuth() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/auth/register").build());

        Mono<Void> result = filter.apply(new AuthenticationFilter.Config()).filter(exchange, chain);

        StepVerifier.create(result).verifyComplete();
        verify(chain).filter(exchange);
        assertThat(exchange.getRequest().getHeaders().getFirst(Constants.HEADER_AUTH_USER)).isNull();
    }

    @Test
    void apply_whenSecuredAndNoAuthHeader_throwsMissingAuthHeader() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/cart").build());

        Throwable thrown = null;
        try {
            filter.apply(new AuthenticationFilter.Config()).filter(exchange, chain).block();
        } catch (Throwable e) {
            thrown = e.getCause() != null ? e.getCause() : e;
        }
        assertThat(thrown).isNotNull().hasMessageContaining(Constants.ERROR_MISSING_AUTH_HEADER);
    }

    @Test
    void apply_whenSecuredAndInvalidToken_throwsUnauthorized() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/cart")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                        .build());
        when(jwtUtil.extractAllClaims("invalid-token")).thenThrow(new RuntimeException("bad token"));

        Throwable thrown = null;
        try {
            filter.apply(new AuthenticationFilter.Config()).filter(exchange, chain).block();
        } catch (Throwable e) {
            thrown = e.getCause() != null ? e.getCause() : e;
        }
        assertThat(thrown).isNotNull().hasMessageContaining(Constants.ERROR_UNAUTHORIZED_ACCESS);
    }

    @Test
    void apply_whenSecuredAndValidToken_addsAuthHeadersAndInvokesChain() {
        String token = "valid-jwt";
        UUID userId = UUID.randomUUID();
        Claims claims = io.jsonwebtoken.Jwts.claims().setSubject("alice");
        claims.put(Constants.CLAIM_ROLE, "USER");
        claims.put(Constants.CLAIM_USER_ID, userId);

        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/cart")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build());
        when(jwtUtil.extractAllClaims(token)).thenReturn(claims);

        Mono<Void> result = filter.apply(new AuthenticationFilter.Config()).filter(exchange, chain);

        StepVerifier.create(result).verifyComplete();
        ArgumentCaptor<ServerWebExchange> exchangeCaptor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(exchangeCaptor.capture());
        ServerWebExchange captured = exchangeCaptor.getValue();
        assertThat(captured.getRequest().getHeaders().getFirst(Constants.HEADER_AUTH_USER)).isEqualTo("alice");
        assertThat(captured.getRequest().getHeaders().getFirst(Constants.HEADER_AUTH_ROLE)).isEqualTo("USER");
        assertThat(captured.getRequest().getHeaders().getFirst(Constants.HEADER_AUTH_ID)).isEqualTo(userId.toString());
    }
}
