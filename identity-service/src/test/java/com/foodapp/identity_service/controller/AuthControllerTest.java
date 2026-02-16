package com.foodapp.identity_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodapp.identity_service.constants.Constants;
import com.foodapp.identity_service.dto.AuthRequest;
import com.foodapp.identity_service.model.User;
import com.foodapp.identity_service.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService service;

    @MockBean
    private AuthenticationManager authenticationManager;

    // We might need to mock CustomUserDetailsService or other security beans
    // depending on SecurityConfig. @WebMvcTest usually loads security config.
    // If SecurityConfig requires other beans, they need to be mocked.

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser
    void testAddNewUser() throws Exception {
        User user = new User();
        user.setUsername("newUser");
        user.setPassword("password");
        user.setEmail("test@test.com"); // Assuming validation requires it

        when(service.saveUser(any(User.class))).thenReturn(user);

        mockMvc.perform(post("/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isCreated());

        verify(service).saveUser(any(User.class));
    }

    @Test
    @WithMockUser
    void testAddAdmin() throws Exception {
        User user = new User();
        user.setUsername("adminUser");
        user.setPassword("password");

        when(service.saveAdmin(any(User.class))).thenReturn(user);

        mockMvc.perform(post("/auth/register/admin")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isCreated());

        verify(service).saveAdmin(any(User.class));
    }

    @Test
    @WithMockUser
    void testGetToken_Success() throws Exception {
        AuthRequest request = new AuthRequest("testUser", "password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(null);
        when(service.generateToken("testUser")).thenReturn("generatedToken");

        mockMvc.perform(post("/auth/token")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("generatedToken"));
    }

    @Test
    @WithMockUser
    void testGetToken_Failure() throws Exception {
        AuthRequest request = new AuthRequest("testUser", "wrongPassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new AuthenticationException("Failed") {
                });

        mockMvc.perform(post("/auth/token")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(Constants.INVALID_USER_OR_PASSWORD));
    }

    @Test
    @WithMockUser
    void testValidateToken() throws Exception {
        String token = "validToken";
        doNothing().when(service).validateToken(token);

        mockMvc.perform(get("/auth/validate")
                .param("token", token))
                .andExpect(status().isOk())
                .andExpect(content().string(Constants.TOKEN_VALID));
    }
}
