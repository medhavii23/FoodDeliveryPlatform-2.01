package com.foodapp.api_gateway.filter;

import com.foodapp.api_gateway.constants.Constants;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Determines which routes require authentication (secured vs open).
 */
@Component
public class RouteValidator {

    /** Paths that do not require a JWT (register, token, eureka). */
    public static final List<String> openApiEndpoints = List.of(
            Constants.ENDPOINT_AUTH_REGISTER,
            Constants.ENDPOINT_AUTH_REGISTER_ADMIN,
            Constants.ENDPOINT_AUTH_TOKEN,
            Constants.ENDPOINT_EUREKA);

    /**
     * Returns predicate that determines whether route is secured.
     */
    public boolean isSecured(ServerHttpRequest request) {
        return openApiEndpoints
                .stream()
                .noneMatch(uri -> request.getURI().getPath().contains(uri));
    }


}
