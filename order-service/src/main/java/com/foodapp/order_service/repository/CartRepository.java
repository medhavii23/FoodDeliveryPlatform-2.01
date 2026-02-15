package com.foodapp.order_service.repository;

import com.foodapp.order_service.model.Cart;
import com.foodapp.order_service.model.CartStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CartRepository extends JpaRepository<Cart, UUID> {

    Optional<Cart> findByCustomerIdAndRestaurantIdAndStatus(UUID customerId, Long restaurantId, CartStatus status);

    Optional<Cart> findByCustomerIdAndStatus(UUID customerId, CartStatus status);
}

