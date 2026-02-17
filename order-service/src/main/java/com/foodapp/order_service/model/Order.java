package com.foodapp.order_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Order entity persisted by Order Service.
 *
 * Exposes "deliveryCharge" in API responses, but maps it to the existing DB column
 * "beyond2km_charge" for schema compatibility.
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class Order {

    private UUID customerId;

    @Id
    private UUID orderId;

    private Long restaurantId;

    private BigDecimal foodAmount;

    /**
     * Delivery fee for the order.
     * (DB column remains beyond2km_charge)
     */
    @Column(name = "beyond2km_charge")
    private BigDecimal deliveryCharge;

    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private Instant createdAt;
}
