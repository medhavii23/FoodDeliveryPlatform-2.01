package com.foodapp.identity_service.repository;

import com.foodapp.identity_service.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuthRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);
}
