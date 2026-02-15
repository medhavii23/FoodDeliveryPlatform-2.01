package com.foodapp.order_service.repository;

import com.foodapp.order_service.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrderOrderId(UUID orderId);
}
