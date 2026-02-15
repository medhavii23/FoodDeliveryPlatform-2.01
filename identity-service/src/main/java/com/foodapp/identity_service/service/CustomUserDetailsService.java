package com.foodapp.identity_service.service;

import com.foodapp.identity_service.constants.Constants;
import com.foodapp.identity_service.model.User;
import com.foodapp.identity_service.repository.AuthRepository;
import com.foodapp.identity_service.config.CustomUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Loads user by username for Spring Security authentication.
 */
@Component
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(CustomUserDetailsService.class);

    @Autowired
    private AuthRepository repository;

    /**
     * Loads user details by username for authentication.
     *
     * @param username the username
     * @return user details for the given username
     * @throws UsernameNotFoundException if user not found
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("loadUserByUsername: {}", username);
        Optional<User> credential = repository.findByUsername(username);
        return credential.map(CustomUserDetails::new)
                .orElseThrow(() -> {
                    log.warn("User not found: {}", username);
                    return new UsernameNotFoundException(Constants.USER_NOT_FOUND + ": " + username);
                });
    }
}
