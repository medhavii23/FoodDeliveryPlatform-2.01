package com.foodapp.identity_service.controller;

import com.foodapp.identity_service.constants.Constants;
import com.foodapp.identity_service.dto.AuthRequest;
import com.foodapp.identity_service.model.User;
import com.foodapp.identity_service.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication: user/admin registration, token issuance, and token validation.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService service;

    @Autowired
    private AuthenticationManager authenticationManager;

    /**
     * Registers a new user (role USER).
     *
     * @param user user with username and password
     * @return saved user entity
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public User addNewUser(@RequestBody @jakarta.validation.Valid User user) {
        log.info("Register user: {}", user.getUsername());
        return service.saveUser(user);
    }

    /**
     * Registers a new admin (role ADMIN).
     *
     * @param user user with username and password
     * @return saved user entity
     */
    @PostMapping("/register/admin")
    @ResponseStatus(HttpStatus.CREATED)
    public User addAdmin(@RequestBody @jakarta.validation.Valid User user) {
        log.info("Register admin: {}", user.getUsername());
        return service.saveAdmin(user);
    }

    /**
     * Authenticates credentials and returns a JWT token.
     *
     * @param authRequest username and password
     * @return JWT token on success, 401 with error message on failure
     */
    @PostMapping("/token")
    public ResponseEntity<?> getToken(@RequestBody @jakarta.validation.Valid AuthRequest authRequest) {
        try {
            log.debug("Token request for user: {}", authRequest.getUsername());
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authRequest.getUsername(), authRequest.getPassword()));

            String token = service.generateToken(authRequest.getUsername());
            log.info("Token issued for user: {}", authRequest.getUsername());
            return ResponseEntity.ok(token);

        } catch (AuthenticationException ex) {
            log.warn("Authentication failed for user: {}", authRequest.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Constants.INVALID_USER_OR_PASSWORD);
        }
    }

    /**
     * Validates a JWT token (e.g. for gateway or other services).
     *
     * @param token JWT token string
     * @return success message if valid
     */
    @GetMapping("/validate")
    public String validateToken(@RequestParam("token") String token) {
        log.debug("Validating token");
        service.validateToken(token);
        return Constants.TOKEN_VALID;
    }
}
