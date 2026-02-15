package com.foodapp.order_service.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;

public class RestaurantClientConfig {

    @Bean
    public RequestInterceptor authRoleInterceptor() {
        return template -> template.header("X-Auth-Role", "USER");
    }
}
