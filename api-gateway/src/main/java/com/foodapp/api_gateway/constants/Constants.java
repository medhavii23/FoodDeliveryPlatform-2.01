package com.foodapp.api_gateway.constants;

public class Constants {
    // JWT Configuration
    public static final String JWT_SECRET = "5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final int BEARER_PREFIX_LENGTH = 7;

    // HTTP Headers
    public static final String HEADER_AUTH_USER = "X-Auth-User";
    public static final String HEADER_AUTH_ROLE = "X-Auth-Role";
    public static final String HEADER_AUTH_ID = "X-Auth-Id";

    // JWT Claims
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_USER_ID = "userId";

    // Open API Endpoints (No Authentication Required)
    public static final String ENDPOINT_AUTH_REGISTER = "/auth/register";
    public static final String ENDPOINT_AUTH_REGISTER_ADMIN = "/auth/register/admin";
    public static final String ENDPOINT_AUTH_TOKEN = "/auth/token";
    public static final String ENDPOINT_EUREKA = "/eureka";

    // Error Messages
    public static final String ERROR_MISSING_AUTH_HEADER = "missing authorization header";
    public static final String ERROR_UNAUTHORIZED_ACCESS = "un authorized access to application";
    public static final String ERROR_INVALID_ACCESS = "invalid access...!";
}
