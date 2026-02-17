package com.foodapp.order_service.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DeliveryClientConfig {

    @Bean
    public RequestInterceptor deliveryAuthRoleInterceptor() {
        return template -> template.header("X-Auth-Role", "ADMIN");
    }
}
