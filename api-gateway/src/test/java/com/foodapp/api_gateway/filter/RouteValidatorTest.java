package com.foodapp.api_gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

import static org.assertj.core.api.Assertions.assertThat;

class RouteValidatorTest {

    private RouteValidator routeValidator;

    @BeforeEach
    void setUp() {
        routeValidator = new RouteValidator();
    }

    @Test
    void isSecured_openPathAuthRegister_returnsFalse() {
        ServerHttpRequest request = MockServerHttpRequest.get("/auth/register").build();
        assertThat(routeValidator.isSecured.test(request)).isFalse();
    }

    @Test
    void isSecured_openPathAuthToken_returnsFalse() {
        ServerHttpRequest request = MockServerHttpRequest.get("/auth/token").build();
        assertThat(routeValidator.isSecured.test(request)).isFalse();
    }

    @Test
    void isSecured_openPathEureka_returnsFalse() {
        ServerHttpRequest request = MockServerHttpRequest.get("/eureka").build();
        assertThat(routeValidator.isSecured.test(request)).isFalse();
    }

    @Test
    void isSecured_openPathRegisterAdmin_returnsFalse() {
        ServerHttpRequest request = MockServerHttpRequest.get("/auth/register/admin").build();
        assertThat(routeValidator.isSecured.test(request)).isFalse();
    }

    @Test
    void isSecured_securedPathApiCart_returnsTrue() {
        ServerHttpRequest request = MockServerHttpRequest.get("/api/cart").build();
        assertThat(routeValidator.isSecured.test(request)).isTrue();
    }

    @Test
    void isSecured_securedPathApiDelivery_returnsTrue() {
        ServerHttpRequest request = MockServerHttpRequest.get("/api/delivery/estimate").build();
        assertThat(routeValidator.isSecured.test(request)).isTrue();
    }
}
