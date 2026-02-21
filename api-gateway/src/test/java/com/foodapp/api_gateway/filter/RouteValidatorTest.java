package com.foodapp.api_gateway.filter;

import com.foodapp.api_gateway.constants.Constants;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

import static org.junit.jupiter.api.Assertions.*;

class RouteValidatorTest {

    private final RouteValidator routeValidator = new RouteValidator();

    @Test
    void testIsSecured_OpenEndpoints() {

        ServerHttpRequest request1 =
                MockServerHttpRequest.get(Constants.ENDPOINT_AUTH_REGISTER).build();
        assertFalse(routeValidator.isSecured(request1));

        ServerHttpRequest request2 =
                MockServerHttpRequest.get(Constants.ENDPOINT_AUTH_REGISTER_ADMIN).build();
        assertFalse(routeValidator.isSecured(request2));

        ServerHttpRequest request3 =
                MockServerHttpRequest.get(Constants.ENDPOINT_AUTH_TOKEN).build();
        assertFalse(routeValidator.isSecured(request3));

        ServerHttpRequest request4 =
                MockServerHttpRequest.get(Constants.ENDPOINT_EUREKA).build();
        assertFalse(routeValidator.isSecured(request4));
    }

    @Test
    void testIsSecured_SecuredEndpoints() {

        ServerHttpRequest request1 =
                MockServerHttpRequest.get("/api/orders").build();
        assertTrue(routeValidator.isSecured(request1));

        ServerHttpRequest request2 =
                MockServerHttpRequest.get("/api/restaurants").build();
        assertTrue(routeValidator.isSecured(request2));
    }

    @Test
    void testIsSecured_PartialMatch_ShouldBeOpen() {

        ServerHttpRequest request =
                MockServerHttpRequest.get("/api/auth/register/extra").build();

        assertFalse(routeValidator.isSecured(request));
    }
}
