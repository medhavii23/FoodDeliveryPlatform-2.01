package com.foodapp.identity_service.service;

import com.foodapp.identity_service.constants.Constants;
import com.foodapp.identity_service.model.User;
import com.foodapp.identity_service.repository.AuthRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private AuthRepository repository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void loadUserByUsername_whenUserFound_returnsUserDetails() {
        User user = new User(UUID.randomUUID(), "alice", null, "encoded", "USER");
        when(repository.findByUsername("alice")).thenReturn(Optional.of(user));

        UserDetails details = customUserDetailsService.loadUserByUsername("alice");

        assertThat(details.getUsername()).isEqualTo("alice");
        assertThat(details.getPassword()).isEqualTo("encoded");
    }

    @Test
    void loadUserByUsername_whenUserNotFound_throws() {
        when(repository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("unknown"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining(Constants.USER_NOT_FOUND);
    }
}
