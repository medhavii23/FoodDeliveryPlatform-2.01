package com.foodapp.delivery_service.repository;

import com.foodapp.delivery_service.model.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    Optional<Delivery> findByOrderId(UUID orderID);
}
