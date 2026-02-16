package com.foodapp.identity_service.config;

import com.foodapp.identity_service.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthConfigTest {

    @Test
    void testCustomUserDetails() {
        User user = new User();
        user.setUsername("testuser");
        user.setPassword("password");

        CustomUserDetails userDetails = new CustomUserDetails(user);

        assertEquals("testuser", userDetails.getUsername());
        assertEquals("password", userDetails.getPassword());
        assertTrue(userDetails.isAccountNonExpired());
        assertTrue(userDetails.isAccountNonLocked());
        assertTrue(userDetails.isCredentialsNonExpired());
        assertTrue(userDetails.isEnabled());
        assertTrue(userDetails.getAuthorities().isEmpty());
    }

    @Test
    void testNoArgsConstructor() {
        CustomUserDetails userDetails = new CustomUserDetails();
        assertNotNull(userDetails);
    }

    @Test
    void testAllArgsConstructor() {
        CustomUserDetails userDetails = new CustomUserDetails("user", "pass");
        assertEquals("user", userDetails.getUsername());
    }

    @Test
    void testSecurityConfigBeans() throws Exception {
        SecurityConfig securityConfig = new SecurityConfig();

        // Test PasswordEncoder bean
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();
        assertNotNull(passwordEncoder);

        // Test AuthenticationManager bean
        AuthenticationConfiguration authConfig = mock(AuthenticationConfiguration.class);
        AuthenticationManager authManager = mock(AuthenticationManager.class);
        when(authConfig.getAuthenticationManager()).thenReturn(authManager);

        assertEquals(authManager, securityConfig.authenticationManager(authConfig));
    }
}
