package com.foodapp.api_gateway.filter;

import com.foodapp.api_gateway.constants.Constants;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteValidatorTest {

    private final RouteValidator routeValidator = new RouteValidator();

    @Test
    void testIsSecured_OpenEndpoints() {
        ServerHttpRequest request1 = MockServerHttpRequest.get(Constants.ENDPOINT_AUTH_REGISTER).build();
        assertFalse(routeValidator.isSecured.test(request1));

        ServerHttpRequest request2 = MockServerHttpRequest.get(Constants.ENDPOINT_AUTH_TOKEN).build();
        assertFalse(routeValidator.isSecured.test(request2));

        ServerHttpRequest request3 = MockServerHttpRequest.get(Constants.ENDPOINT_EUREKA).build();
        assertFalse(routeValidator.isSecured.test(request3));
    }

    @Test
    void testIsSecured_SecuredEndpoints() {
        ServerHttpRequest request1 = MockServerHttpRequest.get("/api/orders").build();
        assertTrue(routeValidator.isSecured.test(request1));

        ServerHttpRequest request2 = MockServerHttpRequest.get("/api/restaurants").build();
        assertTrue(routeValidator.isSecured.test(request2));
    }
}
