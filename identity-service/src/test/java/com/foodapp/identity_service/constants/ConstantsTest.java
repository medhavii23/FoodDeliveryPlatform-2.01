package com.foodapp.identity_service.constants;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ConstantsTest {

    @Test
    void testConstantsValues() {
        assertEquals("ADMIN", Constants.ROLE_ADMIN);
        assertEquals("USER", Constants.ROLE_USER);

        assertEquals("User not found", Constants.USER_NOT_FOUND);
        assertEquals("Invalid credentials", Constants.INVALID_CREDENTIALS);
        assertEquals("Invalid username or password", Constants.INVALID_USER_OR_PASSWORD);
        assertEquals("User already exists with username: ", Constants.USER_ALREADY_EXISTS);
        assertEquals("Token is valid", Constants.TOKEN_VALID);

        assertEquals("role", Constants.CLAIM_ROLE);
        assertEquals("userId", Constants.CLAIM_USER_ID);

        assertEquals("Authorization", Constants.AUTH_HEADER);
        assertEquals("Bearer ", Constants.TOKEN_PREFIX);

        assertEquals("5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437", Constants.JWT_SECRET);
    }

    @Test
    void testConstructor() {
        Constants constants = new Constants();
        assertNotNull(constants);
    }
}
