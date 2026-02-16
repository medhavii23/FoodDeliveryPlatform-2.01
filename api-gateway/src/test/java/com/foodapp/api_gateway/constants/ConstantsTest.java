package com.foodapp.api_gateway.constants;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ConstantsTest {

    @Test
    public void testConstantsValues() {
        // JWT Configuration
        assertEquals("5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437", Constants.JWT_SECRET);
        assertEquals("Bearer ", Constants.BEARER_PREFIX);
        assertEquals(7, Constants.BEARER_PREFIX_LENGTH);

        // HTTP Headers
        assertEquals("X-Auth-User", Constants.HEADER_AUTH_USER);
        assertEquals("X-Auth-Role", Constants.HEADER_AUTH_ROLE);
        assertEquals("X-Auth-Id", Constants.HEADER_AUTH_ID);

        // JWT Claims
        assertEquals("role", Constants.CLAIM_ROLE);
        assertEquals("userId", Constants.CLAIM_USER_ID);

        // Open API Endpoints
        assertEquals("/auth/register", Constants.ENDPOINT_AUTH_REGISTER);
        assertEquals("/auth/register/admin", Constants.ENDPOINT_AUTH_REGISTER_ADMIN);
        assertEquals("/auth/token", Constants.ENDPOINT_AUTH_TOKEN);
        assertEquals("/eureka", Constants.ENDPOINT_EUREKA);

        // Error Messages
        assertEquals("missing authorization header", Constants.ERROR_MISSING_AUTH_HEADER);
        assertEquals("un authorized access to application", Constants.ERROR_UNAUTHORIZED_ACCESS);
        assertEquals("invalid access...!", Constants.ERROR_INVALID_ACCESS);
    }

    @Test
    public void testConstructor() {
        // This test is to satisfy coverage for the implicit public constructor if any,
        // though Constants classes usually should have private constructors.
        // Since the existing class is public with no constructor defined, it has a
        // default public one.
        Constants constants = new Constants();
        assertNotNull(constants);
    }
}
