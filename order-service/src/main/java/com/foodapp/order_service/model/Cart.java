package com.foodapp.order_service.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "carts")
@Getter
@Setter
@NoArgsConstructor
public class Cart {

    @Id
    private UUID cartId;
    private UUID customerId;
    private Long restaurantId;
    private String deliveryArea;
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    private CartStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}

