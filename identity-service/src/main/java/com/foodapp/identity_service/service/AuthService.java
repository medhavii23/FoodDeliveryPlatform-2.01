package com.foodapp.identity_service.service;

import com.foodapp.identity_service.constants.Constants;
import com.foodapp.identity_service.model.User;
import com.foodapp.identity_service.repository.AuthRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Service for user registration (user/admin) and JWT token generation/validation.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private AuthRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    /**
     * Saves a new user with role USER (password is encoded).
     *
     * @param credential user with username and plain password
     * @return saved user entity
     */
    public User saveUser(User credential) {
        log.debug("saveUser: {}", credential.getUsername());
        credential.setRole(Constants.ROLE_USER);
        credential.setPassword(passwordEncoder.encode(credential.getPassword()));
        repository.save(credential);
        return credential;
    }

    /**
     * Saves a new user with role ADMIN (password is encoded).
     *
     * @param credential user with username and plain password
     * @return saved user entity
     */
    public User saveAdmin(User credential) {
        log.debug("saveAdmin: {}", credential.getUsername());
        credential.setRole(Constants.ROLE_ADMIN);
        credential.setPassword(passwordEncoder.encode(credential.getPassword()));
        repository.save(credential);
        return credential;
    }

    /**
     * Generates a JWT for the given username (user must exist).
     *
     * @param username username
     * @return JWT string
     * @throws RuntimeException if user not found
     */
    public String generateToken(String username) {
        log.debug("generateToken for: {}", username);
        User user = repository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException(Constants.USER_NOT_FOUND));
        return jwtService.generateToken(username, user.getRole(), user.getId());
    }

    /**
     * Validates the JWT (signature and expiry).
     *
     * @param token JWT string
     */
    public void validateToken(String token) {
        log.debug("validateToken");
        jwtService.validateToken(token);
    }
}
