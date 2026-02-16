package com.foodapp.order_service.config;

import feign.RequestTemplate;
import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderConfigTest {

    @Test
    void testOpenApiConfig() {
        OpenApiConfig config = new OpenApiConfig();
        OpenAPI openAPI = config.openAPI();
        assertNotNull(openAPI);
        assertNotNull(openAPI.getServers());
        assertTrue(openAPI.getServers().size() > 0);
    }

    @Test
    void testDeliveryClientConfig() {
        DeliveryClientConfig config = new DeliveryClientConfig();
        RequestTemplate template = new RequestTemplate();
        config.authRoleInterceptor().apply(template);
        assertTrue(template.headers().containsKey("X-Auth-Role"));
    }

    @Test
    void testRestaurantClientConfig() {
        RestaurantClientConfig config = new RestaurantClientConfig();
        RequestTemplate template = new RequestTemplate();
        config.authRoleInterceptor().apply(template);
        assertTrue(template.headers().containsKey("X-Auth-Role"));
    }
}
