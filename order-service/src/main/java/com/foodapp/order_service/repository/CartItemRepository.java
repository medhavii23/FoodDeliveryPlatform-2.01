package com.foodapp.order_service.repository;

import com.foodapp.order_service.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByCart_CartIdAndItemNameIgnoreCase(UUID cartId, String itemName);

    List<CartItem> findByCart_CartId(UUID cartId);

    void deleteByCart_CartId(UUID cartId);
}

